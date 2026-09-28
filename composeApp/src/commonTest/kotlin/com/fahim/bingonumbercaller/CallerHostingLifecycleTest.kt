package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.viewmodel.CallerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CallerHostingLifecycleTest {

    private lateinit var server: FakeGameServerHost
    private lateinit var hostConnection: FakeHostConnection

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        server = FakeGameServerHost()
        hostConnection = FakeHostConnection()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = CallerViewModel(dataSource = InMemoryGameStateStore(), embeddedServer = server, socketClient = hostConnection)

    @Test
    fun `starting hosting connects the host socket with the host session`() = runTest {
        val vm = viewModel()

        vm.startServerAndGame()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isServerRunning)
        assertEquals(server.session.publicInfo, vm.uiState.value.connectionInfo)
        assertSame(server.session, hostConnection.connectedWith)
    }

    @Test
    fun `server stopped from outside the app resets hosting state`() = runTest {
        val vm = viewModel()
        vm.startServerAndGame()
        advanceUntilIdle()

        server.stoppedExternally()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isServerRunning)
        assertNull(vm.uiState.value.connectionInfo)
        assertEquals(1, hostConnection.disconnectCalls)
    }

    @Test
    fun `stopping hosting from the app stops the server once`() = runTest {
        val vm = viewModel()
        vm.startServerAndGame()
        advanceUntilIdle()

        vm.stopServer()
        advanceUntilIdle()

        assertEquals(1, server.stopCalls)
        assertFalse(vm.uiState.value.isServerRunning)
        assertEquals(1, hostConnection.disconnectCalls)
    }
}
