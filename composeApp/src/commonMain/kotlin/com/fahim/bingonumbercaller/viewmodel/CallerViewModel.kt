package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.bingonumbercaller.audio.SoundEffectPlayer
import com.fahim.bingonumbercaller.data.GameSettingsDataSource
import com.fahim.bingonumbercaller.domain.NumberGenerator
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.network.EmbeddedGameServer
import com.fahim.bingonumbercaller.network.GameSocketClient
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallerUiState(
    val gameState: GameState = GameState(),
    val isLoading: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isServerRunning: Boolean = false,
    val connectionInfo: ConnectionInfo? = null,
    val serverError: String? = null,
    val winnerConnectionId: String? = null
) {
    val currentNumber: Int?
        get() = gameState.calledNumbers.lastOrNull()

    val calledNumbers: List<Int>
        get() = gameState.calledNumbers

    val remainingPool: List<Int>
        get() = gameState.remainingPool

    val isGameComplete: Boolean
        get() = gameState.isGameComplete
}

class CallerViewModel(
    private val dataSource: GameSettingsDataSource = GameSettingsDataSource(),
    private val soundPlayer: SoundEffectPlayer = SoundEffectPlayer(),
    private val embeddedServer: EmbeddedGameServer = EmbeddedGameServer(),
    private val socketClient: GameSocketClient = GameSocketClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallerUiState(isLoading = true))
    val uiState: StateFlow<CallerUiState> = _uiState.asStateFlow()

    init {
        loadSavedGame()
        observeSocketMessages()
    }

    private fun loadSavedGame() {
        viewModelScope.launch {
            val saved = dataSource.load()
            val state = saved ?: NumberGenerator.freshGame()
            _uiState.value = _uiState.value.copy(
                gameState = state,
                isLoading = false
            )
        }
    }

    private fun observeSocketMessages() {
        viewModelScope.launch {
            socketClient.incomingMessages.collect { message ->
                when (message) {
                    is ServerMessage.GameStateUpdate -> {
                        val updatedState = GameState(
                            calledNumbers = message.calledNumbers,
                            remainingPool = (1..90).filterNot { message.calledNumbers.contains(it) }
                        )
                        _uiState.value = _uiState.value.copy(gameState = updatedState)
                        dataSource.save(updatedState)
                    }

                    is ServerMessage.GameOver -> {
                        _uiState.value = _uiState.value.copy(
                            winnerConnectionId = message.winnerConnectionId
                        )
                    }

                    is ServerMessage.ErrorMessage -> {
                        _uiState.value = _uiState.value.copy(serverError = message.reason)
                    }

                    else -> {}
                }
            }
        }
    }

    fun startServerAndGame(port: Int = 8080) {
        if (!embeddedServer.isSupported) {
            _uiState.value = _uiState.value.copy(
                serverError = "Hosting a live game server is not supported on this platform. Please host from Android."
            )
            return
        }

        try {
            val info = embeddedServer.start(port = port)
            _uiState.value = _uiState.value.copy(
                isServerRunning = true,
                connectionInfo = info,
                serverError = null
            )

            // Connect caller device as Host over WebSocket
            viewModelScope.launch {
                try {
                    socketClient.connectAsHost(info)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        serverError = "Failed to connect host socket: ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                serverError = e.message ?: "Failed to start game server. Ensure Wi-Fi is active."
            )
        }
    }

    fun stopServer() {
        embeddedServer.stop()
        viewModelScope.launch {
            socketClient.disconnect()
        }
        _uiState.value = _uiState.value.copy(
            isServerRunning = false,
            connectionInfo = null
        )
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(
            isSoundEnabled = !_uiState.value.isSoundEnabled
        )
    }

    fun drawNumber() {
        if (_uiState.value.isServerRunning) {
            viewModelScope.launch {
                socketClient.drawNumber()
            }
        } else {
            // Local fallback if server not started
            val currentState = _uiState.value.gameState
            if (currentState.isGameComplete) return

            val updatedState = NumberGenerator.drawNumber(currentState)
            _uiState.value = _uiState.value.copy(gameState = updatedState)

            viewModelScope.launch {
                dataSource.save(updatedState)
            }
        }

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playDrawSound()
        }
    }

    fun newGame() {
        if (_uiState.value.isServerRunning) {
            viewModelScope.launch {
                socketClient.requestNewGame()
            }
        } else {
            val fresh = NumberGenerator.freshGame()
            _uiState.value = _uiState.value.copy(gameState = fresh, winnerConnectionId = null)

            viewModelScope.launch {
                dataSource.clear()
                dataSource.save(fresh)
            }
        }

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playNewGameSound()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(serverError = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
    }
}
