package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.Ticket

/** Outcome of a player trying to join or rejoin the game. */
sealed interface PlayerJoin {
    /**
     * @property ticket null when the player joined after the first draw; they get one at the next new game.
     * @property rejoinToken private seat credential the client presents to reclaim this seat after a drop.
     * @property rejoined true when an existing seat was reclaimed with a valid token.
     * @property replacedConnectionId the previous socket for this seat, which should now be closed.
     * @property playerId public id for lobby/winner display.
     * @property playerName cleaned, de-duplicated display name.
     */
    data class Accepted(
        val ticket: Ticket?,
        val rejoinToken: String,
        val rejoined: Boolean,
        val replacedConnectionId: String?,
        val playerId: String,
        val playerName: String
    ) : PlayerJoin

    /** The table has reached its player cap and the join carried no valid seat token. */
    data object TableFull : PlayerJoin
}
