package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {

    @Test
    fun testOnlyOneHostAllowed() = runBlocking {
        val engine = GameEngine()
        assertTrue(engine.registerHost("host-1"))
        assertFalse(engine.registerHost("host-2"), "Second host must be rejected")
        assertEquals("host-1", engine.getHostConnectionId())

        // If host-1 unregisters, a new host can register
        engine.unregisterConnection("host-1")
        assertTrue(engine.registerHost("host-2"))
        assertEquals("host-2", engine.getHostConnectionId())
    }

    @Test
    fun testDrawNumberOnlyAllowedForHostWhenInProgress() = runBlocking {
        val engine = GameEngine()
        // No host registered yet -> draw fails
        assertNull(engine.drawNumber("anyone"))

        engine.registerHost("host-1")
        // Non-host caller fails
        assertNull(engine.drawNumber("player-1"))

        // Host draw succeeds
        val state1 = engine.drawNumber("host-1")
        assertNotNull(state1)
        assertEquals(1, state1.calledNumbers.size)
        assertEquals(89, state1.remainingPool.size)
    }

    @Test
    fun testFullHouseClaimValidation() = runBlocking {
        val engine = GameEngine()
        engine.registerHost("host-1")
        val ticket = joinWithTicket(engine, "player-1")

        // 1. Claim when no numbers called -> rejected
        assertFalse(engine.validateClaim("player-1", ticket.id))

        // 2. Claim with someone else's ticket id -> rejected
        assertFalse(engine.validateClaim("player-1", "random-wrong-id"))

        // 3. Draw until exactly the last ticket number is still missing -> still rejected
        val ticketNumbers = ticket.allNumbers.toSet()
        while (true) {
            val s = engine.drawNumber("host-1") ?: break
            val missing = ticketNumbers.count { it !in s.calledNumbers }
            if (missing == 1) break
        }
        assertFalse(engine.validateClaim("player-1", ticket.id), "14 of 15 called must be rejected")

        // 4. Draw until all 15 are called -> accepted, game completes
        while (true) {
            val s = engine.drawNumber("host-1") ?: break
            if (ticketNumbers.all { it in s.calledNumbers }) break
        }
        assertTrue(engine.validateClaim("player-1", ticket.id))
        assertEquals("COMPLETE", engine.getStatus())

        // Immediate subsequent draw must fail because status is COMPLETE
        assertNull(engine.drawNumber("host-1"), "Draw after game complete must be rejected")

        // Second claim attempt must fail because status is COMPLETE
        assertFalse(engine.validateClaim("player-1", ticket.id))
    }

    @Test
    fun testNewGameResetsStateAndGeneratesNewTickets() = runBlocking {
        val engine = GameEngine()
        engine.registerHost("host-1")
        val oldTicket = joinWithTicket(engine, "player-1")
        engine.drawNumber("host-1")

        val result = engine.newGame("host-1")
        assertNotNull(result)
        val (newState, newTickets) = result
        assertEquals(0, newState.calledNumbers.size)
        assertEquals(90, newState.remainingPool.size)
        assertEquals("IN_PROGRESS", engine.getStatus())

        val newTicket = newTickets["player-1"]
        assertNotNull(newTicket)
        // New ticket should have distinct ID
        assertTrue(oldTicket.id != newTicket.id)
    }

    private suspend fun joinWithTicket(engine: GameEngine, connectionId: String): Ticket {
        val join = assertIs<PlayerJoin.Accepted>(engine.registerPlayer(connectionId, rejoinToken = null))
        return assertNotNull(join.ticket)
    }
}
