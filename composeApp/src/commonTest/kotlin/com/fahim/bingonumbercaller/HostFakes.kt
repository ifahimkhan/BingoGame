package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.audio.NumberAnnouncer
import com.fahim.bingonumbercaller.data.GameStateStore
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.network.GameServerHost
import com.fahim.bingonumbercaller.network.HostConnection
import com.fahim.bingonumbercaller.network.HostSession
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

// Test doubles for the host side (embedded server + host socket), shared by Caller tests.

internal class FakeGameServerHost : GameServerHost {
    override val isSupported = true
    override val isRunning = MutableStateFlow(false)

    val session = HostSession(
        publicInfo = ConnectionInfo(host = "192.168.1.20", port = 8080, sessionToken = "123456"),
        hostSecret = "secret"
    )
    var stopCalls = 0

    override fun start(port: Int): HostSession {
        isRunning.value = true
        return session
    }

    override fun stop() {
        stopCalls++
        isRunning.value = false
    }

    /** Simulates "Stop hosting" from the notification or the app being swiped away. */
    fun stoppedExternally() {
        isRunning.value = false
    }
}

internal class FakeHostConnection : HostConnection {
    override val isConnected = MutableStateFlow(false)
    override val incomingMessages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 16)

    var connectedWith: HostSession? = null
    var disconnectCalls = 0

    override suspend fun connectAsHost(hostSession: HostSession) {
        connectedWith = hostSession
        isConnected.value = true
    }

    var drawCalls = 0

    override suspend fun drawNumber() {
        drawCalls++
    }
    override suspend fun requestNewGame() = Unit

    override suspend fun disconnect() {
        disconnectCalls++
        isConnected.value = false
    }
}

/** Pushes a server message to the Caller screen as if it came over the host socket. */
internal suspend fun FakeHostConnection.serverSends(message: ServerMessage) = incomingMessages.emit(message)

/** In-memory store so tests never touch disk or real IO threads. */
internal class InMemoryGameStateStore : GameStateStore {
    var saved: GameState? = null
    override suspend fun save(state: GameState) {
        saved = state
    }
    override suspend fun load(): GameState? = saved
    override suspend fun clear() {
        saved = null
    }
}

/** Records what would have been spoken. */
internal class FakeAnnouncer(override val isSupported: Boolean = true) : NumberAnnouncer {
    val spoken = mutableListOf<String>()
    override fun announce(text: String) {
        spoken += text
    }
    override fun stop() = Unit
    override fun shutdown() = Unit
}
