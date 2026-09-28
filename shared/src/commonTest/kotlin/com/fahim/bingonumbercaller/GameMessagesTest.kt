package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.TicketGenerator
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GameMessagesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testClientMessagesSerializationRoundTrip() {
        val messages: List<ClientMessage> = listOf(
            ClientMessage.JoinAsPlayer("session-token-123"),
            ClientMessage.JoinAsHost("session-token-123"),
            ClientMessage.DrawNumber,
            ClientMessage.RequestNewGame,
            ClientMessage.ClaimFullHouse("ticket-uuid-abc")
        )

        for (msg in messages) {
            val encoded = json.encodeToString(msg)
            val decoded = json.decodeFromString<ClientMessage>(encoded)
            assertEquals(msg, decoded)
        }
    }

    @Test
    fun testServerMessagesSerializationRoundTrip() {
        val ticket = TicketGenerator.generateTicket()
        val messages: List<ServerMessage> = listOf(
            ServerMessage.Joined(ticket = ticket, role = "PLAYER"),
            ServerMessage.Joined(ticket = null, role = "HOST"),
            ServerMessage.GameStateUpdate(
                calledNumbers = listOf(5, 12, 45, 88),
                currentNumber = 88,
                remainingCount = 86,
                status = "IN_PROGRESS"
            ),
            ServerMessage.ClaimRejected("Number 42 is not on your ticket"),
            ServerMessage.GameOver(winnerConnectionId = "conn-999"),
            ServerMessage.ErrorMessage("Only the host can draw numbers")
        )

        for (msg in messages) {
            val encoded = json.encodeToString(msg)
            val decoded = json.decodeFromString<ServerMessage>(encoded)
            assertEquals(msg, decoded)
        }
    }
}
