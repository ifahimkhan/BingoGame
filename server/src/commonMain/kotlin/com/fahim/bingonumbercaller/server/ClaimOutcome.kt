package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.protocol.PlayerSummary

/** Result of a Full House claim, detailed enough to tell a bogus claim from a lost race. */
sealed interface ClaimOutcome {
    /** Every number on the ticket was called. The game is now complete. */
    data class Accepted(val player: PlayerSummary) : ClaimOutcome

    /** A genuine false claim: the game is live but these ticket numbers have not been called. */
    data class Incomplete(val player: PlayerSummary, val uncalledNumbers: List<Int>) : ClaimOutcome

    /** Claimed a ticket id that isn't the one this seat holds. */
    data class WrongTicket(val player: PlayerSummary) : ClaimOutcome

    /** Game not running, e.g. someone else already won. Not the claimer's fault. */
    data object NotInProgress : ClaimOutcome

    /** Connection has no seat or no ticket (late joiner, host). */
    data object NotSeated : ClaimOutcome
}
