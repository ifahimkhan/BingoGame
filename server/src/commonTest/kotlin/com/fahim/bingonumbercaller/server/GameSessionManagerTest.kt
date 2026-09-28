package com.fahim.bingonumbercaller.server

import com.fahim.bingonumbercaller.protocol.ClientMessage
import com.fahim.bingonumbercaller.protocol.ProtocolLimits
import com.fahim.bingonumbercaller.protocol.ServerMessage
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class GameSessionManagerTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun serverTest(
        hostSecret: String = HOST_SECRET,
        block: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            module(GameSessionManager(currentSessionToken = QR_TOKEN, hostSecret = hostSecret))
        }
        block()
    }

    private fun ApplicationTestBuilder.wsClient() = createClient { install(WebSockets) }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: ClientMessage) =
        send(Frame.Text(json.encodeToString(ClientMessage.serializer(), message)))

    private suspend fun DefaultClientWebSocketSession.receiveMessage(): ServerMessage =
        withTimeout(5.seconds) {
            val frame = assertIs<Frame.Text>(incoming.receive())
            json.decodeFromString(ServerMessage.serializer(), frame.readText())
        }

    private suspend fun DefaultClientWebSocketSession.awaitCloseCode(): Short {
        val reason = withTimeout(5.seconds) { closeReason.await() }
        return assertNotNull(reason, "server should close the connection").code
    }

    @Test
    fun `host join with correct host secret is accepted`() = serverTest {
        wsClient().webSocket("/game") {
            sendMessage(ClientMessage.JoinAsHost(hostSecret = HOST_SECRET))

            val joined = assertIs<ServerMessage.Joined>(receiveMessage())
            assertEquals("HOST", joined.role)
        }
    }

    @Test
    fun `host join using the public QR token is rejected`() = serverTest {
        wsClient().webSocket("/game") {
            sendMessage(ClientMessage.JoinAsHost(hostSecret = QR_TOKEN))

            assertIs<ServerMessage.ErrorMessage>(receiveMessage())
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, awaitCloseCode())
        }
    }

    @Test
    fun `host join is rejected when server has no host secret configured`() = serverTest(hostSecret = "") {
        wsClient().webSocket("/game") {
            sendMessage(ClientMessage.JoinAsHost(hostSecret = ""))

            assertIs<ServerMessage.ErrorMessage>(receiveMessage())
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, awaitCloseCode())
        }
    }

    @Test
    fun `player join with QR token still receives a ticket`() = serverTest {
        wsClient().webSocket("/game") {
            sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN))

            val joined = assertIs<ServerMessage.Joined>(receiveMessage())
            assertEquals("PLAYER", joined.role)
            assertNotNull(joined.ticket)
        }
    }

    @Test
    fun `oversized frame closes the connection`() = serverTest {
        wsClient().webSocket("/game") {
            send(Frame.Text("x".repeat(ProtocolLimits.MAX_FRAME_BYTES.toInt() + 1)))

            assertEquals(CloseReason.Codes.TOO_BIG.code, awaitCloseCode())
        }
    }

    @Test
    fun `player rejoining with token gets the same ticket back`() = serverTest {
        val client = wsClient()
        var firstJoin: ServerMessage.Joined? = null
        client.webSocket("/game") {
            sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN))
            firstJoin = assertIs<ServerMessage.Joined>(receiveMessage())
        }
        val original = assertNotNull(firstJoin)
        val token = assertNotNull(original.rejoinToken, "server must issue a rejoin token")

        client.webSocket("/game") {
            sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN, rejoinToken = token))
            val again = assertIs<ServerMessage.Joined>(receiveMessage())
            assertEquals(original.ticket, again.ticket)
        }
    }

    @Test
    fun `player joining after first draw is seated without a ticket`() = serverTest {
        val client = wsClient()
        client.webSocket("/game") {
            sendMessage(ClientMessage.JoinAsHost(hostSecret = HOST_SECRET))
            assertIs<ServerMessage.Joined>(receiveMessage())
            assertIs<ServerMessage.GameStateUpdate>(receiveMessage())
            assertIs<ServerMessage.LobbyUpdate>(receiveMessage())
            sendMessage(ClientMessage.DrawNumber)
            assertIs<ServerMessage.GameStateUpdate>(receiveMessage())

            client.webSocket("/game") {
                sendMessage(ClientMessage.JoinAsPlayer(sessionToken = QR_TOKEN))
                val joined = assertIs<ServerMessage.Joined>(receiveMessage())
                assertEquals("PLAYER", joined.role)
                assertNull(joined.ticket)
                assertNotNull(joined.rejoinToken)
            }
        }
    }

    @Test
    fun `player with wrong session token gets a terminal join rejection`() = serverTest {
        wsClient().webSocket("/game") {
            sendMessage(ClientMessage.JoinAsPlayer(sessionToken = "stale-token"))

            assertIs<ServerMessage.JoinRejected>(receiveMessage())
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, awaitCloseCode())
        }
    }

    private companion object {
        const val QR_TOKEN = "123456"
        const val HOST_SECRET = "host-only-secret-never-in-qr"
    }
}

class ConstantTimeEqualsTest {
    @Test
    fun `matches only identical strings`() {
        kotlin.test.assertTrue(constantTimeEquals("abc123", "abc123"))
        kotlin.test.assertFalse(constantTimeEquals("abc123", "abc124"))
        kotlin.test.assertFalse(constantTimeEquals("abc", "abc123"))
        kotlin.test.assertFalse(constantTimeEquals("abc123", ""))
        kotlin.test.assertTrue(constantTimeEquals("", ""))
    }
}
