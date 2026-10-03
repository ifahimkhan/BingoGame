package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** What a player's screen needs from the game connection. Lets reconnect logic be tested with a fake. */
interface PlayerConnection {
    val isConnected: StateFlow<Boolean>
    val incomingMessages: SharedFlow<ServerMessage>

    /**
     * Opens the socket and sends the join. [rejoinToken] reclaims an existing seat when non-null;
     * [playerName] is only a request, the server decides the final name.
     */
    suspend fun connectAsPlayer(connectionInfo: ConnectionInfo, rejoinToken: String?, playerName: String?)
    suspend fun claimFullHouse(ticketId: String)
    suspend fun claimLine(ticketId: String)
    suspend fun disconnect()
}
