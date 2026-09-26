
package com.fahim.bingonumbercaller.protocol

import com.fahim.bingonumbercaller.model.Ticket
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    data class JoinAsPlayer(val sessionToken: String = "") : ClientMessage()

    @Serializable
    data class JoinAsHost(val sessionToken: String = "") : ClientMessage()

    @Serializable
    data object DrawNumber : ClientMessage()

    @Serializable
    data object RequestNewGame : ClientMessage()

    @Serializable
    data class ClaimFullHouse(val ticketId: String) : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    @Serializable
    data class Joined(val ticket: Ticket?, val role: String) : ServerMessage()

    @Serializable
    data class GameStateUpdate(
        val calledNumbers: List<Int>,
        val currentNumber: Int?,
        val remainingCount: Int,
        val status: String
    ) : ServerMessage()

    @Serializable
    data class ClaimRejected(val reason: String) : ServerMessage()

    @Serializable
    data class GameOver(val winnerConnectionId: String) : ServerMessage()

    @Serializable
    data class ErrorMessage(val reason: String) : ServerMessage()
}
