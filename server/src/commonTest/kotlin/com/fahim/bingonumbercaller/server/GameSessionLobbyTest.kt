package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.model.allNumbers
import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GameSessionLobbyTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun serverTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            module(GameSessionManager(currentSessionToken = QR_TOKEN, hostSecret = HOST_SECRET))
        }
        block()
    }

    private fun ApplicationTestBuilder.wsClient() = createClient { install(WebSockets) }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: ClientMessage) =
        send(Frame.Text(json.encodeToString(ClientMessage.serializer(), message)))

    /** Skips unrelated broadcasts (lobby, state) until a message of type [T] that matches arrives. */
    private suspend inline fun <reified T : ServerMessage> DefaultClientWebSocketSession.awaitMessage(
        crossinline matches: (T) -> Boolean = { true }
    ): T = withTimeout(10.seconds) {
        while (true) {
            val frame = incoming.receive() as? Frame.Text ?: continue
            val message = json.decodeFromString(ServerMessage.serializer(), frame.readText())
            if (message is T && matches(message)) return@withTimeout message
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }

    private suspend fun DefaultClientWebSocketSession.joinAsHost() {
        sendMessage(ClientMessage.JoinAsHost(hostSecret = HOST_SECRET))
        awaitMessage<ServerMessage.Joined>()
    }

    @Test
    fun `player gets the cleaned name back and host sees them in the lobby`() = serverTest {
        val client = wsClient()
        client.webSocket("/game") {
            joinAsHost()

            client.webSocket("/game") {
                sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN, playerName = "  Ann   Lee "))
                val joined = awaitMessage<ServerMessage.Joined>()
                assertEquals("Ann Lee", joined.playerName)
            }

            val lobby = awaitMessage<ServerMessage.LobbyUpdate> { update -> update.players.any { it.name == "Ann Lee" } }
            val ann = lobby.players.single { it.name == "Ann Lee" }
            assertTrue(ann.hasTicket)
        }
    }

    @Test
    fun `early claim is rejected for the player and reported to the host`() = serverTest {
        val client = wsClient()
        client.webSocket("/game") {
            joinAsHost()

            client.webSocket("/game") {
                sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN, playerName = "Bob"))
                val ticket = assertNotNull(awaitMessage<ServerMessage.Joined>().ticket)
                sendMessage(ClientMessage.ClaimFullHouse(ticketId = ticket.id))
                awaitMessage<ServerMessage.ClaimRejected>()
            }

            val notice = awaitMessage<ServerMessage.FalseClaim>()
            assertEquals("Bob", notice.playerName)
            assertEquals(15, notice.uncalledNumbers.size, "nothing called yet, all 15 are missing")
        }
    }

    @Test
    fun `winning claim announces the winner by name`() = serverTest {
        val client = wsClient()
        client.webSocket("/game") {
            val host = this
            joinAsHost()

            client.webSocket("/game") {
                sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN, playerName = "Cara"))
                val ticket = assertNotNull(awaitMessage<ServerMessage.Joined>().ticket)

                // Host draws the whole pool so every ticket number is called
                repeat(90) {
                    host.sendMessage(ClientMessage.DrawNumber)
                }
                awaitMessage<ServerMessage.GameStateUpdate> { it.calledNumbers.containsAll(ticket.allNumbers) }

                sendMessage(ClientMessage.ClaimFullHouse(ticketId = ticket.id))
                val gameOver = awaitMessage<ServerMessage.GameOver>()
                assertEquals("Cara", gameOver.winnerName)
            }
        }
    }

    private companion object {
        const val QR_TOKEN = "123456"
        const val HOST_SECRET = "host-only-secret-never-in-qr"
    }
}
