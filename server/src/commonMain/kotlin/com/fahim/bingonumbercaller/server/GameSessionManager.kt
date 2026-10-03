package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.Prize
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
                                playerName = join.playerName,
                                playerId = join.playerId
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
                    val liveSessions = sessionsMutex.withLock { sessions.toMap() }
                    for ((connection, newTicket) in newTickets) {
                        liveSessions[connection]?.let {
                            sendMessage(it, ServerMessage.Joined(ticket = newTicket, role = "PLAYER"))
                        }
                    }
                    broadcastGameState(newState)
                    // Late joiners now hold tickets
                    broadcastLobby()
                } else {
                    sendMessage(session, ServerMessage.ErrorMessage("Only host can reset game"))
                }
            }

            is ClientMessage.ClaimFullHouse -> handleClaim(
                connectionId, session, Prize.FULL_HOUSE, gameEngine.claimFullHouse(connectionId, message.ticketId)
            )

            is ClientMessage.ClaimLine -> handleClaim(
                connectionId, session, Prize.LINE, gameEngine.claimLine(connectionId, message.ticketId)
            )
        }
    }

    private suspend fun handleClaim(
        connectionId: String,
        session: WebSocketSession,
        prize: Prize,
        outcome: ClaimOutcome
    ) {
        when (outcome) {
            is ClaimOutcome.Accepted -> broadcast(
                when (prize) {
                    Prize.FULL_HOUSE -> ServerMessage.GameOver(
                        winnerConnectionId = connectionId,
                        winnerName = outcome.player.name,
                        winnerPlayerId = outcome.player.playerId,
                        missedBy = outcome.missedBy
                    )

                    Prize.LINE -> ServerMessage.LineWon(
                        winnerPlayerId = outcome.player.playerId,
                        winnerName = outcome.player.name,
                        missedBy = outcome.missedBy
                    )
                }
            )

            is ClaimOutcome.Incomplete -> {
                val reason = when (prize) {
                    Prize.FULL_HOUSE -> "Not all of your numbers have been called yet."
                    Prize.LINE -> "None of your rows is fully called yet."
                }
                sendMessage(session, ServerMessage.ClaimRejected(reason, prize))
                // Bogus call: let the caller announce it
                sendToHost(
                    ServerMessage.FalseClaim(
                        playerId = outcome.player.playerId,
                        playerName = outcome.player.name,
                        uncalledNumbers = outcome.uncalledNumbers,
                        prize = prize
                    )
                )
            }

            is ClaimOutcome.WrongTicket -> {
                sendMessage(session, ServerMessage.ClaimRejected("That ticket isn't yours for this game.", prize))
            }

            is ClaimOutcome.NotInProgress -> {
                sendMessage(
                    session,
                    ServerMessage.ClaimRejected("The game isn't in progress. Someone may have already won.", prize)
                )
            }

            is ClaimOutcome.PrizeTaken -> {
                sendMessage(session, ServerMessage.ClaimRejected("${outcome.winnerName} already won the line.", prize))
            }

            is ClaimOutcome.NotSeated -> {
                sendMessage(session, ServerMessage.ClaimRejected("You don't have a ticket in this game.", prize))
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
        sendMessage(session, stateUpdate(gameEngine.getGameState()))
    }

    private suspend fun broadcastGameState(state: GameState) {
        broadcast(stateUpdate(state))
    }

    private suspend fun stateUpdate(state: GameState) = ServerMessage.GameStateUpdate(
        calledNumbers = state.calledNumbers,
        currentNumber = state.calledNumbers.lastOrNull(),
        remainingCount = state.remainingPool.size,
        status = gameEngine.getStatus(),
        lineWinnerName = gameEngine.getLineWinnerName()
    )

    /**
     * Sends to every socket in parallel, each with a deadline: one stalled phone (screen off,
     * Wi-Fi asleep) must never hold up the numbers for everyone else.
     */
    suspend fun broadcast(message: ServerMessage) {
        val activeSessions = sessionsMutex.withLock { sessions.values.toList() }
        val text = json.encodeToString(message)
        coroutineScope {
            for (session in activeSessions) {
                launch { sendOrDrop(session, text) }
            }
        }
    }

    private suspend fun sendMessage(session: WebSocketSession, message: ServerMessage) {
        sendOrDrop(session, json.encodeToString(message))
    }

    /**
     * A socket that can't take a frame within [SEND_TIMEOUT_MILLIS] is dead or stuck. Cancel it
     * so its handler cleans up; the player's client reconnects and gets the full state on rejoin.
     */
    private suspend fun sendOrDrop(session: WebSocketSession, text: String) {
        val sent = try {
            withTimeoutOrNull(SEND_TIMEOUT_MILLIS) { session.send(Frame.Text(text)) } != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        // Never drop the host: its loopback socket has no reconnect loop and is needed to draw
        if (!sent && !isHostSession(session)) {
            session.cancel()
        }
    }

    private suspend fun isHostSession(session: WebSocketSession): Boolean {
        val hostId = gameEngine.getHostConnectionId() ?: return false
        return sessionsMutex.withLock { sessions[hostId] } === session
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

    private companion object {
        const val SEND_TIMEOUT_MILLIS = 5_000L
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
