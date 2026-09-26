package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.domain.NumberGenerator
import com.fahim.bingonumbercaller.domain.TicketGenerator
import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameEngine(
    initialState: GameState = NumberGenerator.freshGame()
) {
    private val mutex = Mutex()

    private var gameState: GameState = initialState
    private val tickets = mutableMapOf<String, Ticket>()
    private var hostConnectionId: String? = null
    private var status: String = "WAITING" // "WAITING", "IN_PROGRESS", "COMPLETE"

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

    suspend fun registerPlayer(connectionId: String): Ticket = mutex.withLock {
        val existing = tickets[connectionId]
        if (existing != null) {
            return existing
        }
        val ticket = TicketGenerator.generateTicket()
        tickets[connectionId] = ticket
        return ticket
    }

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
        // Regenerate fresh tickets for any already-connected players
        val playerIds = tickets.keys.toList()
        for (playerId in playerIds) {
            tickets[playerId] = TicketGenerator.generateTicket()
        }
        status = "IN_PROGRESS"
        return gameState to tickets.toMap()
    }

    suspend fun validateClaim(connectionId: String, ticketId: String): Boolean = mutex.withLock {
        if (status != "IN_PROGRESS") {
            return false
        }
        val ticket = tickets[connectionId] ?: return false
        if (ticket.id != ticketId) {
            return false
        }
        val calledSet = gameState.calledNumbers.toSet()
        val allOnTicket = ticket.allNumbers
        if (allOnTicket.isEmpty()) {
            return false
        }
        val allCalled = allOnTicket.all { calledSet.contains(it) }
        if (!allCalled) {
            return false
        }

        // Win condition verified: transition to COMPLETE immediately
        status = "COMPLETE"
        return true
    }

    suspend fun unregisterConnection(connectionId: String) = mutex.withLock {
        if (hostConnectionId == connectionId) {
            hostConnectionId = null
        }
        tickets.remove(connectionId)
    }

    suspend fun getGameState(): GameState = mutex.withLock { gameState }

    suspend fun getStatus(): String = mutex.withLock { status }

    suspend fun getHostConnectionId(): String? = mutex.withLock { hostConnectionId }

    suspend fun getPlayerTicket(connectionId: String): Ticket? = mutex.withLock { tickets[connectionId] }
}
