package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameSocketClient(
    private val client: HttpClient = HttpClient {
        install(WebSockets)
    }
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val _incomingMessages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ServerMessage> = _incomingMessages.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    suspend fun connectAsPlayer(connectionInfo: ConnectionInfo) {
        connect(connectionInfo, isHost = false)
    }

    suspend fun connectAsHost(connectionInfo: ConnectionInfo) {
        connect(connectionInfo, isHost = true)
    }

    private suspend fun connect(connectionInfo: ConnectionInfo, isHost: Boolean) {
        disconnect()

        val socketUrl = "ws://${connectionInfo.host}:${connectionInfo.port}/game"
        try {
            val newSession = client.webSocketSession {
                url(socketUrl)
            }
            session = newSession
            _isConnected.value = true

            // Send initial join message
            val joinMessage = if (isHost) {
                ClientMessage.JoinAsHost(sessionToken = connectionInfo.sessionToken)
            } else {
                ClientMessage.JoinAsPlayer(sessionToken = connectionInfo.sessionToken)
            }
            sendMessage(joinMessage)

            // Listen for incoming messages
            receiveJob = scope.launch {
                try {
                    for (frame in newSession.incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val serverMessage = json.decodeFromString<ServerMessage>(text)
                                _incomingMessages.emit(serverMessage)
                            } catch (e: Exception) {
                                // Ignore unparseable frames
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Socket closed or network error
                } finally {
                    _isConnected.value = false
                }
            }
        } catch (e: Exception) {
            _isConnected.value = false
            throw e
        }
    }

    suspend fun claimFullHouse(ticketId: String) {
        sendMessage(ClientMessage.ClaimFullHouse(ticketId = ticketId))
    }

    suspend fun drawNumber() {
        sendMessage(ClientMessage.DrawNumber)
    }

    suspend fun requestNewGame() {
        sendMessage(ClientMessage.RequestNewGame)
    }

    suspend fun sendMessage(message: ClientMessage) {
        val currentSession = session
        if (currentSession != null && currentSession.isActive) {
            val text = json.encodeToString(message)
            currentSession.send(Frame.Text(text))
        }
    }

    suspend fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        try {
            session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
        } catch (e: Exception) {
            // Ignore close error
        }
        session = null
        _isConnected.value = false
    }
}
