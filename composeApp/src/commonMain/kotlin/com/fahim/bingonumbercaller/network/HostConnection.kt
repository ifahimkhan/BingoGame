package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** What the host's Caller screen needs from its own socket to the embedded server. */
interface HostConnection {
    val isConnected: StateFlow<Boolean>
    val incomingMessages: SharedFlow<ServerMessage>

    suspend fun connectAsHost(hostSession: HostSession)
    suspend fun drawNumber()
    suspend fun requestNewGame()
    suspend fun disconnect()
}
