package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.network.PlayerConnection
import com.fahim.bingonumbercaller.protocol.ServerMessage
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakePlayerConnection : PlayerConnection {
    override val isConnected = MutableStateFlow(false)
    override val incomingMessages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)

    val connectCalls = mutableListOf<Pair<ConnectionInfo, String?>>()
    var failConnect = false

    override suspend fun connectAsPlayer(connectionInfo: ConnectionInfo, rejoinToken: String?, playerName: String?) {
        connectCalls += connectionInfo to rejoinToken
        if (failConnect) throw IllegalStateException("host unreachable")
        isConnected.value = true
    }

    override suspend fun claimFullHouse(ticketId: String) = Unit

    override suspend fun disconnect() {
        isConnected.value = false
    }

    suspend fun serverSends(message: ServerMessage) = incomingMessages.emit(message)

    fun dropConnection() {
        isConnected.value = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnswerSheetReconnectTest {

    private val gameA = ConnectionInfo(host = "192.168.1.20", port = 8080, sessionToken = "111111")
    private val gameB = ConnectionInfo(host = "192.168.1.30", port = 8080, sessionToken = "222222")

    private val ticketA = Ticket(
        id = "ticket-a",
        cells = listOf(
            listOf(1, null, 20, null, 40, null, 60, null, 80),
            listOf(null, 11, null, 31, 41, null, 61, null, 81),
            listOf(2, null, 22, null, null, 52, null, 72, 82)
        )
    )
    private val ticketB = ticketA.copy(id = "ticket-b")

    private lateinit var connection: FakePlayerConnection

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        connection = FakePlayerConnection()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.joinedViewModel(
        ticket: Ticket? = ticketA,
        token: String = "seat-token"
    ): AnswerSheetViewModel {
        val vm = AnswerSheetViewModel(connection)
        advanceUntilIdle()
        vm.connect(gameA)
        advanceUntilIdle()
        connection.serverSends(ServerMessage.Joined(ticket = ticket, role = "PLAYER", rejoinToken = token))
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `dropped connection reconnects using the rejoin token`() = runTest {
        joinedViewModel(token = "seat-token")

        connection.dropConnection()
        advanceUntilIdle()

        assertEquals(2, connection.connectCalls.size)
        assertEquals(gameA to "seat-token", connection.connectCalls.last())
    }

    @Test
    fun `shows reconnecting and keeps retrying with backoff until the host is back`() = runTest {
        val vm = joinedViewModel()
        connection.failConnect = true

        connection.dropConnection()
        advanceTimeBy(20_000)
        assertTrue(vm.uiState.value.isReconnecting)
        assertTrue(connection.connectCalls.size in 3..7, "backoff should space out attempts")

        connection.failConnect = false
        advanceTimeBy(20_000)
        assertTrue(connection.isConnected.value)
        assertFalse(vm.uiState.value.isReconnecting)
    }

    @Test
    fun `rejoin with the same ticket keeps the player's marks`() = runTest {
        val vm = joinedViewModel()
        connection.serverSends(
            ServerMessage.GameStateUpdate(listOf(1, 20), currentNumber = 20, remainingCount = 88, status = "IN_PROGRESS")
        )
        advanceUntilIdle()
        vm.toggleCell(1)
        vm.toggleCell(20)

        connection.dropConnection()
        advanceUntilIdle()
        connection.serverSends(ServerMessage.Joined(ticket = ticketA, role = "PLAYER", rejoinToken = "seat-token"))
        advanceUntilIdle()

        assertEquals(setOf(1, 20), vm.uiState.value.userCheckedNumbers)
    }

    @Test
    fun `a different ticket clears the marks`() = runTest {
        val vm = joinedViewModel()
        connection.serverSends(
            ServerMessage.GameStateUpdate(listOf(1), currentNumber = 1, remainingCount = 89, status = "IN_PROGRESS")
        )
        advanceUntilIdle()
        vm.toggleCell(1)

        connection.serverSends(ServerMessage.Joined(ticket = ticketB, role = "PLAYER"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.userCheckedNumbers.isEmpty())
    }

    @Test
    fun `leaving the game stops reconnect attempts`() = runTest {
        val vm = joinedViewModel()

        vm.leaveGame()
        advanceUntilIdle()

        assertEquals(1, connection.connectCalls.size)
        assertFalse(connection.isConnected.value)
    }

    @Test
    fun `join rejection is terminal and shown to the player`() = runTest {
        val vm = joinedViewModel()

        connection.serverSends(ServerMessage.JoinRejected("This game is full."))
        advanceUntilIdle()
        connection.dropConnection()
        advanceUntilIdle()

        assertEquals(1, connection.connectCalls.size)
        assertEquals("This game is full.", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isReconnecting)
    }

    @Test
    fun `late joiner waits for the next game then gets a ticket`() = runTest {
        val vm = joinedViewModel(ticket = null)
        assertTrue(vm.uiState.value.isWaitingForNextGame)

        connection.serverSends(ServerMessage.Joined(ticket = ticketA, role = "PLAYER"))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isWaitingForNextGame)
        assertEquals(ticketA, vm.uiState.value.ticket)
    }

    @Test
    fun `rejoin token is reused for the same game but not for a different game`() = runTest {
        val vm = joinedViewModel(token = "seat-token")
        vm.leaveGame()
        advanceUntilIdle()

        vm.connect(gameA)
        advanceUntilIdle()
        assertEquals(gameA to "seat-token", connection.connectCalls.last())

        vm.leaveGame()
        advanceUntilIdle()
        vm.connect(gameB)
        advanceUntilIdle()
        assertNull(connection.connectCalls.last().second)
    }
}
