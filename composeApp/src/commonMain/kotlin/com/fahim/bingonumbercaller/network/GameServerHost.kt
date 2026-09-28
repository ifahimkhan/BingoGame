package com.fahim.bingonumbercaller.network

import kotlinx.coroutines.flow.StateFlow

/** The embedded game server as the Caller screen sees it. */
interface GameServerHost {
    val isSupported: Boolean

    /**
     * True while the server is up. Can turn false without the app asking, e.g. when the host
     * taps "Stop hosting" in the notification or swipes the app away.
     */
    val isRunning: StateFlow<Boolean>

    fun start(port: Int = 8080): HostSession
    fun stop()
}
