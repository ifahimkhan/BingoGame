package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.protocol.PlayerSummary
import com.fahim.bingonumbercaller.protocol.ServerMessage
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CallerLobbyTest {

    private lateinit var hostConnection: FakeHostConnection

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        hostConnection = FakeHostConnection()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.hostingViewModel(): CallerViewModel {
        val vm = CallerViewModel(dataSource = InMemoryGameStateStore(), embeddedServer = FakeGameServerHost(), socketClient = hostConnection)
        vm.startServerAndGame()
        advanceUntilIdle()
        return vm
    }

    private fun update(called: List<Int>, status: String = "IN_PROGRESS") =
        ServerMessage.GameStateUpdate(called, called.lastOrNull(), 90 - called.size, status)

    @Test
    fun `lobby update lists the players`() = runTest {
        val vm = hostingViewModel()
        val players = listOf(
            PlayerSummary("p1", "Ann", isConnected = true, hasTicket = true),
            PlayerSummary("p2", "Bob", isConnected = false, hasTicket = true)
        )

        hostConnection.serverSends(ServerMessage.LobbyUpdate(players))
        advanceUntilIdle()

        assertEquals(players, vm.uiState.value.players)
    }

    @Test
    fun `false claim is shown to the host and can be dismissed`() = runTest {
        val vm = hostingViewModel()
        val notice = ServerMessage.FalseClaim("p2", "Bob", uncalledNumbers = listOf(7, 42))

        hostConnection.serverSends(notice)
        advanceUntilIdle()
        assertEquals(notice, vm.uiState.value.falseClaim)

        vm.dismissFalseClaim()
        assertNull(vm.uiState.value.falseClaim)
    }

    @Test
    fun `winner is announced by name and cleared on new game`() = runTest {
        val vm = hostingViewModel()
        hostConnection.serverSends(update(listOf(5, 9)))
        hostConnection.serverSends(ServerMessage.FalseClaim("p2", "Bob", listOf(1)))
        hostConnection.serverSends(ServerMessage.GameOver(winnerConnectionId = "conn-1", winnerName = "Ann"))
        advanceUntilIdle()
        assertEquals("Ann", vm.uiState.value.winnerName)

        hostConnection.serverSends(update(emptyList()))
        advanceUntilIdle()

        assertNull(vm.uiState.value.winnerName)
        assertNull(vm.uiState.value.falseClaim, "stale false-claim notice belongs to the old game")
    }

    @Test
    fun `stopping hosting clears the lobby`() = runTest {
        val vm = hostingViewModel()
        hostConnection.serverSends(
            ServerMessage.LobbyUpdate(listOf(PlayerSummary("p1", "Ann", isConnected = true, hasTicket = true)))
        )
        advanceUntilIdle()

        vm.stopServer()
        advanceUntilIdle()

        assertEquals(emptyList(), vm.uiState.value.players)
    }
}
