package com.fahim.bingonumbercaller.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class EmbeddedGameServer actual constructor() : GameServerHost {
    actual override val isSupported: Boolean = false

    actual override val isRunning: StateFlow<Boolean> = MutableStateFlow(false)

    actual override fun start(port: Int): HostSession {
        throw UnsupportedOperationException(
            "Hosting an embedded game server on iOS is currently unsupported. " +
            "Please host the bingo session from an Android device. " +
            "Players on iOS can connect and play seamlessly via 'Join Game'."
        )
    }

    actual override fun stop() {
        // No-op
    }
}
