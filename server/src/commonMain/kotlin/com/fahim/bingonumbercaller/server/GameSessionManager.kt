package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * @param currentSessionToken public token carried in the QR code; lets players join.
 * @param hostSecret private secret known only to the host app; required to join as host.
 *   An empty secret disables host joins entirely (fail closed).
 */
class GameSessionManager(
    val gameEngine: GameEngine = GameEngine(),
    var currentSessionToken: String = "",
    private val hostSecret: String = ""
) {
    private val sessionsMutex = Mutex()
    private val sessions = mutableMapOf<String, WebSocketSession>()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun handleSession(session: WebSocketSession) {
        val connectionId = generateConnectionId()
        sessionsMutex.withLock {
            sessions[connectionId] = session
        }

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    handleClientMessage(connectionId, session, text)
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // Normal disconnect
        } catch (e: Exception) {
            // Connection error
        } finally {
            // NonCancellable: cleanup must finish even when the socket coroutine was cancelled
            withContext(NonCancellable) {
                sessionsMutex.withLock {
                    sessions.remove(connectionId)
                }
                gameEngine.unregisterConnection(connectionId)
                // Seat stays, but the lobby should show this player as disconnected
                broadcastLobby()
            }
        }
    }

    private suspend fun handleClientMessage(
        connectionId: String,
        session: WebSocketSession,
        rawText: String
    ) {
        val message = try {
            json.decodeFromString<ClientMessage>(rawText)
        } catch (e: Exception) {
            sendMessage(session, ServerMessage.ErrorMessage("Invalid message payload: ${e.message}"))
            return
        }

        when (message) {
            is ClientMessage.JoinAsHost -> {
                if (!isValidHostSecret(message.hostSecret)) {
                    sendMessage(session, ServerMessage.ErrorMessage("Not authorized to host"))
                    session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid host secret"))
                    return
                }

                val success = gameEngine.registerHost(connectionId)
                if (!success) {
                    sendMessage(session, ServerMessage.ErrorMessage("Host already connected"))
                    session.close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Host already connected"))
                    return
                }

                sendMessage(session, ServerMessage.Joined(ticket = null, role = "HOST"))
                sendCurrentGameState(session)
                sendMessage(session, ServerMessage.LobbyUpdate(gameEngine.lobbySnapshot()))
            }

            is ClientMessage.JoinAsPlayer -> {
                if (currentSessionToken.isNotEmpty() && message.sessionToken != currentSessionToken) {
                    rejectJoin(session, "This game session has ended or the code is wrong. Scan the host's QR code again.")
                    return
                }

                when (val join = gameEngine.registerPlayer(connectionId, message.rejoinToken, message.playerName)) {
                    is PlayerJoin.TableFull -> {
                        rejectJoin(session, "This game is full.")
                    }

                    is PlayerJoin.Accepted -> {
                        join.replacedConnectionId?.let { closeReplacedSession(it) }
                        sendMessage(
                            session,
                            ServerMessage.Joined(
                                ticket = join.ticket,
                                role = "PLAYER",
                                rejoinToken = join.rejoinToken,
                                playerName = join.playerName
                            )
                        )
                        sendCurrentGameState(session)
                        broadcastLobby()
                    }
                }
            }

            is ClientMessage.DrawNumber -> {
                val updatedState = gameEngine.drawNumber(connectionId)
                if (updatedState != null) {
                    broadcastGameState(updatedState)
                } else {
                    sendMessage(
                        session,
                        ServerMessage.ErrorMessage("Draw not permitted: caller is not host or game is not in progress")
                    )
                }
            }

            is ClientMessage.RequestNewGame -> {
                val result = gameEngine.newGame(connectionId)
                if (result != null) {
                    val (newState, newTickets) = result
                    // Notify each player of their new ticket
                    sessionsMutex.withLock {
                        for ((playerId, newTicket) in newTickets) {
                            val playerSession = sessions[playerId]
                            if (playerSession != null) {
                                sendMessage(playerSession, ServerMessage.Joined(ticket = newTicket, role = "PLAYER"))
                            }
                        }
                    }
                    broadcastGameState(newState)
                    // Late joiners now hold tickets
                    broadcastLobby()
                } else {
                    sendMessage(session, ServerMessage.ErrorMessage("Only host can reset game"))
                }
            }

            is ClientMessage.ClaimFullHouse -> handleClaim(connectionId, session, message.ticketId)
        }
    }

    private suspend fun handleClaim(connectionId: String, session: WebSocketSession, ticketId: String) {
        when (val outcome = gameEngine.claimFullHouse(connectionId, ticketId)) {
            is ClaimOutcome.Accepted -> {
                broadcast(ServerMessage.GameOver(winnerConnectionId = connectionId, winnerName = outcome.player.name))
            }

            is ClaimOutcome.Incomplete -> {
                sendMessage(session, ServerMessage.ClaimRejected("Not all of your numbers have been called yet."))
                // Bogus call: let the caller announce it
                sendToHost(
                    ServerMessage.FalseClaim(
                        playerId = outcome.player.playerId,
                        playerName = outcome.player.name,
                        uncalledNumbers = outcome.uncalledNumbers
                    )
                )
            }

            is ClaimOutcome.WrongTicket -> {
                sendMessage(session, ServerMessage.ClaimRejected("That ticket isn't yours for this game."))
            }

            is ClaimOutcome.NotInProgress -> {
                sendMessage(session, ServerMessage.ClaimRejected("The game isn't in progress. Someone may have already won."))
            }

            is ClaimOutcome.NotSeated -> {
                sendMessage(session, ServerMessage.ClaimRejected("You don't have a ticket in this game."))
            }
        }
    }

    private suspend fun broadcastLobby() {
        broadcast(ServerMessage.LobbyUpdate(gameEngine.lobbySnapshot()))
    }

    private suspend fun sendToHost(message: ServerMessage) {
        val hostId = gameEngine.getHostConnectionId() ?: return
        val hostSession = sessionsMutex.withLock { sessions[hostId] } ?: return
        sendMessage(hostSession, message)
    }

    private suspend fun sendCurrentGameState(session: WebSocketSession) {
        val state = gameEngine.getGameState()
        val status = gameEngine.getStatus()
        val update = ServerMessage.GameStateUpdate(
            calledNumbers = state.calledNumbers,
            currentNumber = state.calledNumbers.lastOrNull(),
            remainingCount = state.remainingPool.size,
            status = status
        )
        sendMessage(session, update)
    }

    private suspend fun broadcastGameState(state: GameState) {
        val status = gameEngine.getStatus()
        val update = ServerMessage.GameStateUpdate(
            calledNumbers = state.calledNumbers,
            currentNumber = state.calledNumbers.lastOrNull(),
            remainingCount = state.remainingPool.size,
            status = status
        )
        broadcast(update)
    }

    suspend fun broadcast(message: ServerMessage) {
        val activeSessions = sessionsMutex.withLock { sessions.values.toList() }
        val text = json.encodeToString(message)
        for (session in activeSessions) {
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                // Ignore send failures on stale sessions
            }
        }
    }

    private suspend fun sendMessage(session: WebSocketSession, message: ServerMessage) {
        try {
            val text = json.encodeToString(message)
            session.send(Frame.Text(text))
        } catch (e: Exception) {
            // Ignore send failures
        }
    }

    private suspend fun rejectJoin(session: WebSocketSession, reason: String) {
        sendMessage(session, ServerMessage.JoinRejected(reason))
        session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Join rejected"))
    }

    /** A seat was reclaimed from a new socket; the old one (often a half-dead connection) is closed. */
    private suspend fun closeReplacedSession(connectionId: String) {
        val stale = sessionsMutex.withLock { sessions.remove(connectionId) } ?: return
        try {
            stale.close(CloseReason(CloseReason.Codes.NORMAL, "Seat reclaimed by another connection"))
        } catch (e: Exception) {
            // Stale socket may already be dead
        }
    }

    private fun isValidHostSecret(candidate: String): Boolean =
        hostSecret.isNotEmpty() && constantTimeEquals(candidate, hostSecret)

    private fun generateConnectionId(): String {
        val randomHex = Random.nextInt(0, 0xFFFFFF).toString(16).padStart(6, '0')
        return "conn-$randomHex"
    }
}

/** Compares without early exit so response timing does not leak how much of the secret matched. */
internal fun constantTimeEquals(a: String, b: String): Boolean {
    var diff = a.length xor b.length
    for (i in 0 until maxOf(a.length, b.length)) {
        val ca = if (i < a.length) a[i].code else 0
        val cb = if (i < b.length) b[i].code else 0
        diff = diff or (ca xor cb)
    }
    return diff == 0
}
