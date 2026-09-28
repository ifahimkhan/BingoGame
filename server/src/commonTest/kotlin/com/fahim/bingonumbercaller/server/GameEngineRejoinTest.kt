package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.allNumbers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineRejoinTest {

    private suspend fun GameEngine.joinFresh(connectionId: String): PlayerJoin.Accepted =
        assertIs<PlayerJoin.Accepted>(registerPlayer(connectionId, rejoinToken = null))

    @Test
    fun `player joining before first draw gets a ticket and a rejoin token`() = runBlocking<Unit> {
        val engine = GameEngine()
        val join = engine.joinFresh("conn-1")

        assertNotNull(join.ticket)
        assertTrue(join.rejoinToken.length >= 32, "token must be long and unguessable")
        assertFalse(join.rejoined)
    }

    @Test
    fun `rejoin with token after disconnect restores the same ticket`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        val first = engine.joinFresh("conn-1")
        engine.drawNumber("host")

        engine.unregisterConnection("conn-1")
        val again = assertIs<PlayerJoin.Accepted>(engine.registerPlayer("conn-2", first.rejoinToken))

        assertEquals(first.ticket, again.ticket)
        assertEquals(first.rejoinToken, again.rejoinToken)
        assertTrue(again.rejoined)
    }

    @Test
    fun `rejoin moves claim rights to the new connection`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        val first = engine.joinFresh("conn-1")
        val ticket = assertNotNull(first.ticket)

        val again = assertIs<PlayerJoin.Accepted>(engine.registerPlayer("conn-2", first.rejoinToken))
        assertEquals("conn-1", again.replacedConnectionId)

        drawUntilAllCalled(engine, ticket.allNumbers.toSet())
        assertFalse(engine.validateClaim("conn-1", ticket.id), "stale connection must not claim")
        assertTrue(engine.validateClaim("conn-2", ticket.id))
    }

    @Test
    fun `new player after first draw waits without a ticket`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        engine.drawNumber("host")

        val late = engine.joinFresh("late")

        assertNull(late.ticket)
        assertFalse(engine.validateClaim("late", "anything"))
    }

    @Test
    fun `waiting player receives a ticket on new game`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        engine.drawNumber("host")
        engine.joinFresh("late")

        val (_, tickets) = assertNotNull(engine.newGame("host"))

        assertNotNull(tickets["late"])
    }

    @Test
    fun `unknown rejoin token is treated as a new player`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        engine.drawNumber("host")

        val join = assertIs<PlayerJoin.Accepted>(engine.registerPlayer("conn-x", "forged-token"))

        assertNull(join.ticket, "forged token must not bypass the late-join rule")
        assertNotEquals("forged-token", join.rejoinToken)
        assertFalse(join.rejoined)
    }

    @Test
    fun `new players are refused once the table is full`() = runBlocking<Unit> {
        val engine = GameEngine(maxPlayers = 2)
        val a = engine.joinFresh("a")
        engine.joinFresh("b")

        assertIs<PlayerJoin.TableFull>(engine.registerPlayer("c", rejoinToken = null))
        // Existing seats can still come back
        assertIs<PlayerJoin.Accepted>(engine.registerPlayer("a2", a.rejoinToken))
    }

    private suspend fun drawUntilAllCalled(engine: GameEngine, numbers: Set<Int>) {
        while (true) {
            val state = engine.drawNumber("host") ?: return
            if (numbers.all { it in state.calledNumbers }) return
        }
    }
}
