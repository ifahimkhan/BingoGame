package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.PlayerNames
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.network.PlayerConnection
import com.fahim.bingonumbercaller.protocol.ServerMessage
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetViewModel
import com.fahim.bingonumbercaller.viewmodel.JoinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class NameRecordingConnection : PlayerConnection {
    override val isConnected = MutableStateFlow(false)
    override val incomingMessages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 16)
    val names = mutableListOf<String?>()

    override suspend fun connectAsPlayer(connectionInfo: ConnectionInfo, rejoinToken: String?, playerName: String?) {
        names += playerName
        isConnected.value = true
    }

    override suspend fun claimFullHouse(ticketId: String) = Unit
    override suspend fun disconnect() {
        isConnected.value = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerNameFlowTest {

    private val game = ConnectionInfo(host = "192.168.1.20", port = 8080, sessionToken = "111111")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `join screen caps the name at the shared maximum`() {
        val vm = JoinViewModel()

        vm.onPlayerNameChanged("z".repeat(PlayerNames.MAX_LENGTH + 5))

        assertEquals(PlayerNames.MAX_LENGTH, vm.uiState.value.playerName.length)
    }

    @Test
    fun `requested name is sent on join and again on reconnect`() = runTest {
        val connection = NameRecordingConnection()
        val vm = AnswerSheetViewModel(connection)
        advanceUntilIdle()

        vm.connect(game, playerName = "Ann")
        advanceUntilIdle()
        connection.incomingMessages.emit(ServerMessage.Joined(null, "PLAYER", "tok", playerName = "Ann"))
        advanceUntilIdle()
        connection.isConnected.value = false
        advanceUntilIdle()

        assertEquals(listOf<String?>("Ann", "Ann"), connection.names)
    }

    @Test
    fun `answer sheet shows the server-assigned name and the winner's name`() = runTest {
        val connection = NameRecordingConnection()
        val vm = AnswerSheetViewModel(connection)
        advanceUntilIdle()
        vm.connect(game, playerName = "sam")
        advanceUntilIdle()

        connection.incomingMessages.emit(ServerMessage.Joined(null, "PLAYER", "tok", playerName = "sam (2)"))
        connection.incomingMessages.emit(ServerMessage.GameOver(winnerConnectionId = "c9", winnerName = "Ann"))
        advanceUntilIdle()

        assertEquals("sam (2)", vm.uiState.value.playerName)
        assertEquals("Ann", vm.uiState.value.winnerName)
    }
}
