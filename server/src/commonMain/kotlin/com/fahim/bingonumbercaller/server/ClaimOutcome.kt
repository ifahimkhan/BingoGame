package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.protocol.PlayerRef
import com.fahim.bingonumbercaller.protocol.PlayerSummary

/** Result of a prize claim, detailed enough to tell a bogus claim from a lost race. */
sealed interface ClaimOutcome {
    /**
     * The claim is valid. For Full House the game is now complete.
     * @property missedBy other seats that also qualified but didn't claim first.
     */
    data class Accepted(val player: PlayerSummary, val missedBy: List<PlayerRef> = emptyList()) : ClaimOutcome

    /** A genuine false claim: the game is live but these ticket numbers have not been called. */
    data class Incomplete(val player: PlayerSummary, val uncalledNumbers: List<Int>) : ClaimOutcome

    /** Claimed a ticket id that isn't the one this seat holds. */
    data class WrongTicket(val player: PlayerSummary) : ClaimOutcome

    /** Game not running, e.g. someone else already won. Not the claimer's fault. */
    data object NotInProgress : ClaimOutcome

    /** The line was already awarded to [winnerName]. Not a false claim. */
    data class PrizeTaken(val winnerName: String) : ClaimOutcome

    /** Connection has no seat or no ticket (late joiner, host). */
    data object NotSeated : ClaimOutcome
}
