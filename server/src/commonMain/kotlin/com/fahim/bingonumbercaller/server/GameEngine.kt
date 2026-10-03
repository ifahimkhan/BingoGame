package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.domain.NumberGenerator
import com.fahim.bingonumbercaller.domain.PlayerNames
import com.fahim.bingonumbercaller.domain.TicketGenerator
import com.fahim.bingonumbercaller.domain.WinRules
import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import com.fahim.bingonumbercaller.protocol.PlayerRef
import com.fahim.bingonumbercaller.protocol.PlayerSummary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameEngine(
    initialState: GameState = NumberGenerator.freshGame(),
    private val maxPlayers: Int = DEFAULT_MAX_PLAYERS,
    private val newToken: () -> String = SecureTokens::newToken
) {
    /**
     * A player's place at the table. Outlives any one socket.
     * @property playerId public, broadcastable id (never the rejoin token).
     * @property ticket null while the player waits for the next game after joining late.
     */
    private data class Seat(
        val playerId: String,
        val name: String,
        val ticket: Ticket?
    )

    private val mutex = Mutex()

    private var gameState: GameState = initialState

    // Seats keyed by the player's private rejoin token, in join order
    private val seats = linkedMapOf<String, Seat>()
    private val connectionToSeat = mutableMapOf<String, String>()
    private var seatsCreated = 0
    private var hostConnectionId: String? = null
    private var status: String = "WAITING" // "WAITING", "IN_PROGRESS", "COMPLETE"
    private var lineWinnerName: String? = null

    suspend fun registerHost(connectionId: String): Boolean = mutex.withLock {
        if (hostConnectionId != null && hostConnectionId != connectionId) {
            return false
        }
        hostConnectionId = connectionId
        if (status == "WAITING") {
            status = "IN_PROGRESS"
        }
        return true
    }

    /**
     * Seats a player. A valid [rejoinToken] reclaims the existing seat (same ticket, same name)
     * and moves it to [connectionId]. Otherwise a new seat is created: with a ticket before the
     * first draw, or waiting (no ticket) once numbers have been called.
     */
    suspend fun registerPlayer(
        connectionId: String,
        rejoinToken: String?,
        requestedName: String? = null
    ): PlayerJoin = mutex.withLock {
        val existing = rejoinToken?.let { seats[it] }
        if (rejoinToken != null && existing != null) {
            val previousConnection = connectionToSeat.entries
                .firstOrNull { it.value == rejoinToken && it.key != connectionId }
                ?.key
            previousConnection?.let { connectionToSeat.remove(it) }
            connectionToSeat[connectionId] = rejoinToken
            return accepted(existing, rejoinToken, rejoined = true, replacedConnectionId = previousConnection)
        }

        if (seats.size >= maxPlayers) {
            return PlayerJoin.TableFull
        }

        seatsCreated++
        val baseName = PlayerNames.sanitize(requestedName) ?: PlayerNames.defaultName(seatsCreated)
        val seat = Seat(
            playerId = "p$seatsCreated",
            name = PlayerNames.unique(baseName, seats.values.mapTo(mutableSetOf()) { it.name }),
            ticket = if (gameState.calledNumbers.isEmpty()) TicketGenerator.generateTicket() else null
        )
        val token = newToken()
        seats[token] = seat
        connectionToSeat[connectionId] = token
        return accepted(seat, token, rejoined = false, replacedConnectionId = null)
    }

    private fun accepted(seat: Seat, token: String, rejoined: Boolean, replacedConnectionId: String?) =
        PlayerJoin.Accepted(
            ticket = seat.ticket,
            rejoinToken = token,
            rejoined = rejoined,
            replacedConnectionId = replacedConnectionId,
            playerId = seat.playerId,
            playerName = seat.name
        )

    suspend fun drawNumber(requesterId: String): GameState? = mutex.withLock {
        if (requesterId != hostConnectionId || status != "IN_PROGRESS") {
            return null
        }
        val updated = NumberGenerator.drawNumber(gameState)
        gameState = updated
        return gameState
    }

    suspend fun newGame(requesterId: String): Pair<GameState, Map<String, Ticket>>? = mutex.withLock {
        if (requesterId != hostConnectionId) {
            return null
        }
        gameState = NumberGenerator.freshGame()
        lineWinnerName = null
        // Every seat, including late joiners and currently disconnected players, gets a fresh ticket
        for ((token, seat) in seats.entries.toList()) {
            seats[token] = seat.copy(ticket = TicketGenerator.generateTicket())
        }
        status = "IN_PROGRESS"
        // Only live connections are notified now; disconnected seats receive theirs on rejoin
        val ticketsByConnection = connectionToSeat.mapNotNull { (connectionId, token) ->
            seats[token]?.ticket?.let { connectionId to it }
        }.toMap()
        return gameState to ticketsByConnection
    }

    /** Checks a Full House claim against the authoritative called numbers and this seat's ticket. */
    suspend fun claimFullHouse(connectionId: String, ticketId: String): ClaimOutcome = mutex.withLock {
        if (status != "IN_PROGRESS") {
            return ClaimOutcome.NotInProgress
        }
        val token = connectionToSeat[connectionId] ?: return ClaimOutcome.NotSeated
        val seat = seats[token] ?: return ClaimOutcome.NotSeated
        val ticket = seat.ticket ?: return ClaimOutcome.NotSeated
        val player = summaryOf(token, seat)
        if (ticket.id != ticketId) {
            return ClaimOutcome.WrongTicket(player)
        }

        val calledSet = gameState.calledNumbers.toSet()
        if (!WinRules.isFullHouse(ticket, calledSet)) {
            return ClaimOutcome.Incomplete(player, ticket.allNumbers.filterNot { it in calledSet }.sorted())
        }

        // Win condition verified: transition to COMPLETE immediately
        status = "COMPLETE"
        return ClaimOutcome.Accepted(player, missedBy = othersQualifying(token) { WinRules.isFullHouse(it, calledSet) })
    }

    /**
     * Checks a line claim: any one row of this seat's ticket fully called. Only the first valid
     * claim wins; the game stays in progress for Full House.
     */
    suspend fun claimLine(connectionId: String, ticketId: String): ClaimOutcome = mutex.withLock {
        if (status != "IN_PROGRESS") {
            return ClaimOutcome.NotInProgress
        }
        lineWinnerName?.let { return ClaimOutcome.PrizeTaken(it) }
        val token = connectionToSeat[connectionId] ?: return ClaimOutcome.NotSeated
        val seat = seats[token] ?: return ClaimOutcome.NotSeated
        val ticket = seat.ticket ?: return ClaimOutcome.NotSeated
        val player = summaryOf(token, seat)
        if (ticket.id != ticketId) {
            return ClaimOutcome.WrongTicket(player)
        }

        val calledSet = gameState.calledNumbers.toSet()
        if (!WinRules.hasCompletedRow(ticket, calledSet)) {
            return ClaimOutcome.Incomplete(player, WinRules.closestRowMissing(ticket, calledSet))
        }

        lineWinnerName = seat.name
        return ClaimOutcome.Accepted(player, missedBy = othersQualifying(token) { WinRules.hasCompletedRow(it, calledSet) })
    }

    /** Seats other than [winnerToken] whose ticket also met the prize condition: they missed it. */
    private fun othersQualifying(winnerToken: String, qualifies: (Ticket) -> Boolean): List<PlayerRef> =
        seats.filter { (token, seat) -> token != winnerToken && seat.ticket?.let(qualifies) == true }
            .map { (_, seat) -> PlayerRef(seat.playerId, seat.name) }

    suspend fun validateClaim(connectionId: String, ticketId: String): Boolean =
        claimFullHouse(connectionId, ticketId) is ClaimOutcome.Accepted

    /** Every seat in join order, with live connection and ticket status. */
    suspend fun lobbySnapshot(): List<PlayerSummary> = mutex.withLock {
        seats.map { (token, seat) -> summaryOf(token, seat) }
    }

    private fun summaryOf(token: String, seat: Seat) = PlayerSummary(
        playerId = seat.playerId,
        name = seat.name,
        isConnected = connectionToSeat.containsValue(token),
        hasTicket = seat.ticket != null
    )

    suspend fun unregisterConnection(connectionId: String) = mutex.withLock {
        if (hostConnectionId == connectionId) {
            hostConnectionId = null
        }
        // Keep the seat so the player can rejoin with their token; only drop the socket mapping
        connectionToSeat.remove(connectionId)
    }

    suspend fun getGameState(): GameState = mutex.withLock { gameState }

    suspend fun getStatus(): String = mutex.withLock { status }

    suspend fun getLineWinnerName(): String? = mutex.withLock { lineWinnerName }

    suspend fun getHostConnectionId(): String? = mutex.withLock { hostConnectionId }

    suspend fun getPlayerTicket(connectionId: String): Ticket? = mutex.withLock {
        connectionToSeat[connectionId]?.let { seats[it]?.ticket }
    }

    companion object {
        const val DEFAULT_MAX_PLAYERS = 100
    }
}
