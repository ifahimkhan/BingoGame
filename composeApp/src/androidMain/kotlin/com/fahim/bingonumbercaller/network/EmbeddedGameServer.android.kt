package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.hosting.HostingService
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.platform.AndroidAppContext
import com.fahim.bingonumbercaller.server.GameEngine
import com.fahim.bingonumbercaller.server.GameSessionManager
import com.fahim.bingonumbercaller.server.SecureTokens
import com.fahim.bingonumbercaller.server.module
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

actual class EmbeddedGameServer actual constructor() : GameServerHost {
    actual override val isSupported: Boolean = true

    private val _isRunning = MutableStateFlow(false)
    actual override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var engine: EmbeddedServer<*, *>? = null
    var sessionManager: GameSessionManager? = null
        private set

    actual override fun start(port: Int): HostSession {
        stop()

        val localIp = LocalIpProvider.getLocalIpAddress()
            ?: throw IllegalStateException("Device is not connected to a local Wi-Fi or hotspot network. Connect to Wi-Fi to host a game.")

        val sessionToken = Random.nextInt(100000, 999999).toString()
        val hostSecret = SecureTokens.newToken()
        val manager = GameSessionManager(
            gameEngine = GameEngine(),
            currentSessionToken = sessionToken,
            hostSecret = hostSecret
        )
        sessionManager = manager

        val server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            module(sessionManager = manager)
        }
        server.start(wait = false)
        engine = server
        active = this
        _isRunning.value = true

        // Keep the process alive while players depend on this phone
        AndroidAppContext.context?.let { HostingService.start(it, address = "$localIp:$port") }

        return HostSession(
            publicInfo = ConnectionInfo(
                host = localIp,
                port = port,
                sessionToken = sessionToken
            ),
            hostSecret = hostSecret
        )
    }

    actual override fun stop() {
        try {
            engine?.stop(gracePeriodMillis = 500, timeoutMillis = 1000)
        } catch (e: Exception) {
            // Ignore stop errors
        }
        engine = null
        sessionManager = null
        if (active === this) {
            active = null
            AndroidAppContext.context?.let { HostingService.stop(it) }
        }
        _isRunning.value = false
    }

    companion object {
        // The server currently hosting, so the notification action and task removal can end the game
        @Volatile
        private var active: EmbeddedGameServer? = null

        internal fun stopActive() {
            active?.stop()
        }
    }
}
