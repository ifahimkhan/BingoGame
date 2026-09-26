package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GameSessionManager(
    val gameEngine: GameEngine = GameEngine(),
    var currentSessionToken: String = ""
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
            sessionsMutex.withLock {
                sessions.remove(connectionId)
            }
            gameEngine.unregisterConnection(connectionId)
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
                if (currentSessionToken.isNotEmpty() && message.sessionToken != currentSessionToken) {
                    sendMessage(session, ServerMessage.ErrorMessage("stale or invalid session"))
                    session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid session token"))
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
            }

            is ClientMessage.JoinAsPlayer -> {
                if (currentSessionToken.isNotEmpty() && message.sessionToken != currentSessionToken) {
                    sendMessage(session, ServerMessage.ErrorMessage("stale or invalid session"))
                    session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid session token"))
                    return
                }

                val ticket = gameEngine.registerPlayer(connectionId)
                sendMessage(session, ServerMessage.Joined(ticket = ticket, role = "PLAYER"))
                sendCurrentGameState(session)
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
                } else {
                    sendMessage(session, ServerMessage.ErrorMessage("Only host can reset game"))
                }
            }

            is ClientMessage.ClaimFullHouse -> {
                val isValid = gameEngine.validateClaim(connectionId, message.ticketId)
                if (isValid) {
                    broadcast(ServerMessage.GameOver(winnerConnectionId = connectionId))
                } else {
                    sendMessage(
                        session,
                        ServerMessage.ClaimRejected("Full House claim rejected: incomplete numbers or game not in progress")
                    )
                }
            }
        }
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

    private fun generateConnectionId(): String {
        val randomHex = Random.nextInt(0, 0xFFFFFF).toString(16).padStart(6, '0')
        return "conn-$randomHex"
    }
}
