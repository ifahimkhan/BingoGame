package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ProtocolLimits
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
        install(WebSockets) {
            maxFrameSize = ProtocolLimits.MAX_FRAME_BYTES
            // Client-side pings so a half-open socket (Wi-Fi drop, screen off) is noticed and reconnect kicks in
            pingIntervalMillis = PING_INTERVAL_MILLIS
        }
    }
) : PlayerConnection, HostConnection {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    private val _incomingMessages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    override val incomingMessages: SharedFlow<ServerMessage> = _incomingMessages.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override suspend fun connectAsPlayer(connectionInfo: ConnectionInfo, rejoinToken: String?, playerName: String?) {
        connect(
            socketUrl = "ws://${connectionInfo.host}:${connectionInfo.port}/game",
            joinMessage = ClientMessage.JoinAsPlayer(
                sessionToken = connectionInfo.sessionToken,
                rejoinToken = rejoinToken,
                playerName = playerName
            )
        )
    }

    override suspend fun connectAsHost(hostSession: HostSession) {
        connect(
            socketUrl = hostSession.hostSocketUrl,
            joinMessage = ClientMessage.JoinAsHost(hostSecret = hostSession.hostSecret)
        )
    }

    private suspend fun connect(socketUrl: String, joinMessage: ClientMessage) {
        disconnect()

        try {
            val newSession = client.webSocketSession {
                url(socketUrl)
            }
            session = newSession
            _isConnected.value = true

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

    override suspend fun claimFullHouse(ticketId: String) {
        sendMessage(ClientMessage.ClaimFullHouse(ticketId = ticketId))
    }

    override suspend fun drawNumber() {
        sendMessage(ClientMessage.DrawNumber)
    }

    override suspend fun requestNewGame() {
        sendMessage(ClientMessage.RequestNewGame)
    }

    suspend fun sendMessage(message: ClientMessage) {
        val currentSession = session
        if (currentSession != null && currentSession.isActive) {
            val text = json.encodeToString(message)
            currentSession.send(Frame.Text(text))
        }
    }

    override suspend fun disconnect() {
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

    private companion object {
        const val PING_INTERVAL_MILLIS = 10_000L
    }
}
