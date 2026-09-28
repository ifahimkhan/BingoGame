package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.protocol.ProtocolLimits
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlin.time.Duration.Companion.seconds

fun Application.module(sessionManager: GameSessionManager = GameSessionManager()) {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = ProtocolLimits.MAX_FRAME_BYTES
        masking = false
    }

    routing {
        webSocket("/game") {
            sessionManager.handleSession(this)
        }
    }
}

/**
 * Standalone entry point, superseded by the embedded server. Without a host secret no
 * connection can become host, so this is only useful for player-side smoke testing.
 */
fun main() {
    embeddedServer(CIO, port = 8080, module = Application::module).start(wait = true)
}
