
package com.fahim.bingonumbercaller.protocol

import com.fahim.bingonumbercaller.model.Ticket
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    /**
     * @property rejoinToken the private seat token from a previous [ServerMessage.Joined];
     *   presenting it reclaims the same seat and ticket after a disconnect.
     * @property playerName requested display name; the server cleans it up and may change it.
     */
    @Serializable
    data class JoinAsPlayer(
        val sessionToken: String = "",
        val rejoinToken: String? = null,
        val playerName: String? = null
    ) : ClientMessage()

    /**
     * Sent only by the host's own app over loopback. [hostSecret] is generated on the
     * host device and is never part of the QR payload, so scanning the QR code does not
     * grant host privileges.
     */
    @Serializable
    data class JoinAsHost(val hostSecret: String) : ClientMessage()

    @Serializable
    data object DrawNumber : ClientMessage()

    @Serializable
    data object RequestNewGame : ClientMessage()

    @Serializable
    data class ClaimFullHouse(val ticketId: String) : ClientMessage()
}

@Serializable
sealed class ServerMessage {
    /**
     * @property ticket null for a player who joined after the first draw; they receive a
     *   ticket via another [Joined] when the host starts a new game.
     * @property rejoinToken private seat token for players; keep it to reclaim the seat.
     * @property playerName the name the server actually assigned (cleaned, de-duplicated).
     */
    @Serializable
    data class Joined(
        val ticket: Ticket?,
        val role: String,
        val rejoinToken: String? = null,
        val playerName: String? = null
    ) : ServerMessage()

    /** Join refused for good (wrong game session, table full). Clients must not auto-retry. */
    @Serializable
    data class JoinRejected(val reason: String) : ServerMessage()

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
    data class GameOver(
        val winnerConnectionId: String,
        val winnerName: String? = null
    ) : ServerMessage()

    /** Everyone seated at the table. Broadcast whenever someone joins, leaves, or gets a ticket. */
    @Serializable
    data class LobbyUpdate(val players: List<PlayerSummary>) : ServerMessage()

    /**
     * Host only: a player claimed Full House while numbers on their ticket were still uncalled.
     * The caller can announce the bogus claim, as in a live game.
     */
    @Serializable
    data class FalseClaim(
        val playerId: String,
        val playerName: String,
        val uncalledNumbers: List<Int>
    ) : ServerMessage()

    @Serializable
    data class ErrorMessage(val reason: String) : ServerMessage()
}

/**
 * Public view of a seat. [playerId] is safe to show and broadcast; it is NOT the rejoin token.
 * [hasTicket] is false for players who joined mid-game and are waiting for the next one.
 */
@Serializable
data class PlayerSummary(
    val playerId: String,
    val name: String,
    val isConnected: Boolean,
    val hasTicket: Boolean
)
