package com.fahim.bingonumbercaller.network

import kotlinx.coroutines.flow.StateFlow

expect class EmbeddedGameServer() : GameServerHost {
    override val isSupported: Boolean
    override val isRunning: StateFlow<Boolean>
    override fun start(port: Int): HostSession
    override fun stop()
}
