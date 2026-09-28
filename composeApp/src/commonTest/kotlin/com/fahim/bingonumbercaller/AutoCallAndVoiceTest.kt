package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.audio.BingoCallPhrases
import com.fahim.bingonumbercaller.protocol.ServerMessage
import com.fahim.bingonumbercaller.viewmodel.AutoCallIntervals
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCallAndVoiceTest {

    private lateinit var server: FakeGameServerHost
    private lateinit var hostConnection: FakeHostConnection
    private lateinit var announcer: FakeAnnouncer

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        server = FakeGameServerHost()
        hostConnection = FakeHostConnection()
        announcer = FakeAnnouncer()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.hostingViewModel(): CallerViewModel {
        val vm = CallerViewModel(
            dataSource = InMemoryGameStateStore(),
            embeddedServer = server,
            socketClient = hostConnection,
            announcer = announcer
        )
        vm.toggleSound() // keep the chime out of host tests
        vm.startServerAndGame()
        advanceUntilIdle()
        return vm
    }

    private fun update(called: List<Int>) =
        ServerMessage.GameStateUpdate(called, called.lastOrNull(), 90 - called.size, "IN_PROGRESS")

    // --- Auto-call ---

    @Test
    fun `auto-call draws immediately and then once per interval`() = runTest {
        val vm = hostingViewModel()
        vm.setAutoCallInterval(5)

        vm.toggleAutoCall()
        runCurrent()
        assertEquals(1, hostConnection.drawCalls)
        assertTrue(vm.uiState.value.isAutoCalling)

        advanceTimeBy(10_001)
        assertEquals(3, hostConnection.drawCalls)
        vm.toggleAutoCall()
    }

    @Test
    fun `pausing auto-call stops further draws`() = runTest {
        val vm = hostingViewModel()
        vm.toggleAutoCall()
        runCurrent()

        vm.toggleAutoCall()
        advanceTimeBy(60_000)

        assertEquals(1, hostConnection.drawCalls)
        assertFalse(vm.uiState.value.isAutoCalling)
    }

    @Test
    fun `auto-call stops when someone wins`() = runTest {
        val vm = hostingViewModel()
        vm.toggleAutoCall()
        runCurrent()

        hostConnection.serverSends(ServerMessage.GameOver(winnerConnectionId = "c1", winnerName = "Ann"))
        advanceTimeBy(60_000)

        assertEquals(1, hostConnection.drawCalls)
        assertFalse(vm.uiState.value.isAutoCalling)
    }

    @Test
    fun `auto-call pauses on a false claim so the caller can announce it`() = runTest {
        val vm = hostingViewModel()
        vm.toggleAutoCall()
        runCurrent()

        hostConnection.serverSends(ServerMessage.FalseClaim("p2", "Bob", listOf(7)))
        advanceTimeBy(60_000)

        assertEquals(1, hostConnection.drawCalls)
        assertFalse(vm.uiState.value.isAutoCalling)
    }

    @Test
    fun `auto-call stops when hosting stops`() = runTest {
        val vm = hostingViewModel()
        vm.toggleAutoCall()
        runCurrent()

        server.stoppedExternally()
        advanceTimeBy(60_000)

        assertEquals(1, hostConnection.drawCalls)
        assertFalse(vm.uiState.value.isAutoCalling)
    }

    @Test
    fun `changing the interval while running takes effect`() = runTest {
        val vm = hostingViewModel()
        vm.setAutoCallInterval(12)
        vm.toggleAutoCall()
        runCurrent()

        vm.setAutoCallInterval(3)
        advanceTimeBy(6_001)

        assertTrue(hostConnection.drawCalls >= 3, "3s cadence should apply, got ${hostConnection.drawCalls}")
        assertEquals(3, vm.uiState.value.autoCallIntervalSeconds)
        vm.toggleAutoCall()
    }

    @Test
    fun `only offered intervals are accepted`() = runTest {
        val vm = hostingViewModel()

        vm.setAutoCallInterval(1)

        assertEquals(AutoCallIntervals.DEFAULT_SECONDS, vm.uiState.value.autoCallIntervalSeconds)
    }

    // --- Voice ---

    @Test
    fun `confirmed draw is announced`() = runTest {
        hostingViewModel()

        hostConnection.serverSends(update(listOf(42)))
        advanceUntilIdle()

        assertEquals(listOf(BingoCallPhrases.phraseFor(42)), announcer.spoken)
    }

    @Test
    fun `nothing is announced with voice off or for a non-draw update`() = runTest {
        val vm = hostingViewModel()
        hostConnection.serverSends(update(listOf(42)))
        advanceUntilIdle()
        hostConnection.serverSends(update(listOf(42))) // repeat state, not a new draw
        advanceUntilIdle()

        vm.toggleVoice()
        hostConnection.serverSends(update(listOf(42, 7)))
        advanceUntilIdle()

        assertEquals(1, announcer.spoken.size)
        assertFalse(vm.uiState.value.isVoiceEnabled)
    }

    @Test
    fun `voice is off and unavailable when the platform has no speech`() = runTest {
        announcer = FakeAnnouncer(isSupported = false)
        val vm = hostingViewModel()

        hostConnection.serverSends(update(listOf(5)))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isVoiceSupported)
        assertTrue(announcer.spoken.isEmpty())
    }

    @Test
    fun `call phrases follow the classic caller style`() {
        assertEquals("On its own, number 7", BingoCallPhrases.phraseFor(7))
        assertEquals("4 and 2, 42", BingoCallPhrases.phraseFor(42))
        assertEquals("9 and 0, 90", BingoCallPhrases.phraseFor(90))
    }
}
