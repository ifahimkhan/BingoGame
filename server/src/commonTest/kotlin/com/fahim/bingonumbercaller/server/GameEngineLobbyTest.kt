package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.allNumbers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineLobbyTest {

    private suspend fun GameEngine.join(
        connectionId: String,
        name: String? = null,
        rejoinToken: String? = null
    ): PlayerJoin.Accepted = assertIs(registerPlayer(connectionId, rejoinToken, name))

    @Test
    fun `player names are cleaned and default when blank`() = runBlocking<Unit> {
        val engine = GameEngine()

        assertEquals("Ann Lee", engine.join("c1", name = "  Ann   Lee ").playerName)
        assertEquals("Player 2", engine.join("c2", name = "   ").playerName)
        assertEquals("Player 3", engine.join("c3", name = null).playerName)
    }

    @Test
    fun `duplicate names get a suffix`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.join("c1", name = "Sam")

        assertEquals("sam (2)", engine.join("c2", name = "sam").playerName, "keeps their casing, suffix added")
    }

    @Test
    fun `rejoin keeps the original name and public id`() = runBlocking<Unit> {
        val engine = GameEngine()
        val first = engine.join("c1", name = "Ann")
        engine.unregisterConnection("c1")

        val again = engine.join("c2", name = "Mallory", rejoinToken = first.rejoinToken)

        assertEquals("Ann", again.playerName)
        assertEquals(first.playerId, again.playerId)
    }

    @Test
    fun `public player id is not the rejoin token`() = runBlocking<Unit> {
        val join = GameEngine().join("c1", name = "Ann")

        assertNotEquals(join.rejoinToken, join.playerId)
        assertFalse(join.rejoinToken.contains(join.playerId) && join.playerId.length > 8)
    }

    @Test
    fun `lobby lists every seat with connection and ticket status`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        engine.join("c1", name = "Ann")
        engine.join("c2", name = "Bob")
        engine.unregisterConnection("c2")
        engine.drawNumber("host")
        engine.join("c3", name = "Late")

        val lobby = engine.lobbySnapshot().associateBy { it.name }

        assertEquals(3, lobby.size)
        assertTrue(lobby.getValue("Ann").isConnected)
        assertFalse(lobby.getValue("Bob").isConnected, "disconnected seat stays listed")
        assertTrue(lobby.getValue("Ann").hasTicket)
        assertFalse(lobby.getValue("Late").hasTicket, "late joiner waits for next game")
    }

    @Test
    fun `early claim reports the uncalled numbers`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        val join = engine.join("c1", name = "Ann")
        val ticket = assertNotNull(join.ticket)

        val outcome = assertIs<ClaimOutcome.Incomplete>(engine.claimFullHouse("c1", ticket.id))

        assertEquals("Ann", outcome.player.name)
        assertEquals(ticket.allNumbers.sorted(), outcome.uncalledNumbers)
    }

    @Test
    fun `claim after the game is won is not treated as a false claim`() = runBlocking<Unit> {
        val engine = GameEngine()
        engine.registerHost("host")
        val winner = assertNotNull(engine.join("c1", name = "Ann").ticket)
        val other = assertNotNull(engine.join("c2", name = "Bob").ticket)
        while (true) {
            val state = engine.drawNumber("host") ?: break
            if (winner.allNumbers.all { it in state.calledNumbers }) break
        }

        val won = assertIs<ClaimOutcome.Accepted>(engine.claimFullHouse("c1", winner.id))
        assertEquals("Ann", won.player.name)
        assertIs<ClaimOutcome.NotInProgress>(engine.claimFullHouse("c2", other.id))
    }
}
