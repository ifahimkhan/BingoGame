package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.bingonumbercaller.audio.BingoCallPhrases
import com.fahim.bingonumbercaller.audio.NumberAnnouncer
import com.fahim.bingonumbercaller.audio.SoundEffectPlayer
import com.fahim.bingonumbercaller.audio.SpeechAnnouncer
import com.fahim.bingonumbercaller.data.GameSettingsDataSource
import com.fahim.bingonumbercaller.data.GameStateStore
import com.fahim.bingonumbercaller.domain.NumberGenerator
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.GameState
import com.fahim.bingonumbercaller.network.EmbeddedGameServer
import com.fahim.bingonumbercaller.network.GameServerHost
import com.fahim.bingonumbercaller.network.HostConnection
import com.fahim.bingonumbercaller.network.GameSocketClient
import com.fahim.bingonumbercaller.protocol.PlayerRef
import com.fahim.bingonumbercaller.protocol.PlayerSummary
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallerUiState(
    val gameState: GameState = GameState(),
    val isLoading: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isVoiceSupported: Boolean = false,
    val isVoiceEnabled: Boolean = false,
    val isAutoCalling: Boolean = false,
    val autoCallIntervalSeconds: Int = AutoCallIntervals.DEFAULT_SECONDS,
    val isServerRunning: Boolean = false,
    val connectionInfo: ConnectionInfo? = null,
    val serverError: String? = null,
    val winnerConnectionId: String? = null,
    val winnerName: String? = null,
    val players: List<PlayerSummary> = emptyList(),
    // Latest bogus prize call, shown to the host until dismissed or a new game starts
    val falseClaim: ServerMessage.FalseClaim? = null,
    val lineWinnerName: String? = null,
    // Players who had a winning row / full ticket but didn't claim before the winner
    val lineMissedBy: List<PlayerRef> = emptyList(),
    val fullHouseMissedBy: List<PlayerRef> = emptyList()
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

private const val STATUS_COMPLETE = "COMPLETE"
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Applies an authoritative server update. The winner survives only while the
 * server still reports the game as complete; any other status means a new game.
 */
internal fun CallerUiState.withServerUpdate(update: ServerMessage.GameStateUpdate): CallerUiState {
    val called = update.calledNumbers
    val calledSet = called.toSet()
    val isComplete = update.status == STATUS_COMPLETE
    return copy(
        gameState = GameState(
            calledNumbers = called,
            remainingPool = (1..90).filterNot { it in calledSet }
        ),
        winnerConnectionId = if (isComplete) winnerConnectionId else null,
        winnerName = if (isComplete) winnerName else null,
        falseClaim = if (called.isEmpty()) null else falseClaim,
        lineWinnerName = update.lineWinnerName,
        lineMissedBy = if (update.lineWinnerName == null) emptyList() else lineMissedBy,
        fullHouseMissedBy = if (isComplete) fullHouseMissedBy else emptyList()
    )
}

/** True when [update] is exactly the current history plus one newly drawn number. */
internal fun CallerUiState.isSingleNewDraw(update: ServerMessage.GameStateUpdate): Boolean {
    val current = calledNumbers
    val incoming = update.calledNumbers
    return incoming.size == current.size + 1 && incoming.subList(0, current.size) == current
}

class CallerViewModel(
    private val dataSource: GameStateStore = GameSettingsDataSource(),
    private val soundPlayer: SoundEffectPlayer = SoundEffectPlayer(),
    private val embeddedServer: GameServerHost = EmbeddedGameServer(),
    private val socketClient: HostConnection = GameSocketClient(),
    private val announcer: NumberAnnouncer = SpeechAnnouncer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CallerUiState(
            isLoading = true,
            isVoiceSupported = announcer.isSupported,
            isVoiceEnabled = announcer.isSupported
        )
    )
    val uiState: StateFlow<CallerUiState> = _uiState.asStateFlow()

    // Set when the host requests a new game; cleared when the server confirms the reset
    private var resetPending = false

    private val autoCaller = AutoCaller(viewModelScope, onTick = ::onAutoCallTick)

    init {
        loadSavedGame()
        observeSocketMessages()
        observeServerLifecycle()
    }

    /** The server can be stopped outside this screen (notification action, app swiped away). */
    private fun observeServerLifecycle() {
        viewModelScope.launch {
            embeddedServer.isRunning.collect { running ->
                if (!running && _uiState.value.isServerRunning) {
                    resetHostingState()
                }
            }
        }
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
                        val previous = _uiState.value
                        val isConfirmedDraw = previous.isSingleNewDraw(message)
                        val isConfirmedReset = resetPending && message.calledNumbers.isEmpty()
                        if (isConfirmedReset) resetPending = false

                        val next = previous.withServerUpdate(message)
                        _uiState.value = next
                        dataSource.save(next.gameState)

                        // Sounds only for actions the server actually accepted
                        if (next.isSoundEnabled) {
                            when {
                                isConfirmedDraw -> soundPlayer.playDrawSound()
                                isConfirmedReset -> soundPlayer.playNewGameSound()
                            }
                        }
                        if (isConfirmedDraw) {
                            message.calledNumbers.lastOrNull()?.let(::announceNumber)
                        }
                    }

                    is ServerMessage.GameOver -> {
                        stopAutoCall()
                        _uiState.value = _uiState.value.copy(
                            winnerConnectionId = message.winnerConnectionId,
                            winnerName = message.winnerName,
                            fullHouseMissedBy = message.missedBy
                        )
                    }

                    is ServerMessage.LineWon -> {
                        // Pause so the caller can announce the line before carrying on
                        stopAutoCall()
                        _uiState.value = _uiState.value.copy(
                            lineWinnerName = message.winnerName,
                            lineMissedBy = message.missedBy
                        )
                    }

                    is ServerMessage.LobbyUpdate -> {
                        _uiState.value = _uiState.value.copy(players = message.players)
                    }

                    is ServerMessage.FalseClaim -> {
                        // A real caller stops to check a claim; the host resumes when ready
                        stopAutoCall()
                        _uiState.value = _uiState.value.copy(falseClaim = message)
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
            val hostSession = embeddedServer.start(port = port)
            _uiState.value = _uiState.value.copy(
                isServerRunning = true,
                connectionInfo = hostSession.publicInfo,
                serverError = null
            )

            // Connect caller device as Host over loopback, authenticated by the private host secret
            viewModelScope.launch {
                try {
                    socketClient.connectAsHost(hostSession)
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
        // Reset first so the isRunning observer sees hosting already ended and doesn't clean up twice
        resetHostingState()
        embeddedServer.stop()
    }

    private fun resetHostingState() {
        resetPending = false
        stopAutoCall()
        _uiState.value = _uiState.value.copy(
            isServerRunning = false,
            connectionInfo = null,
            players = emptyList(),
            falseClaim = null,
            lineWinnerName = null,
            lineMissedBy = emptyList(),
            fullHouseMissedBy = emptyList()
        )
        viewModelScope.launch {
            socketClient.disconnect()
        }
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(
            isSoundEnabled = !_uiState.value.isSoundEnabled
        )
    }

    fun drawNumber() {
        if (_uiState.value.isServerRunning) {
            // Sound plays when the server broadcasts the accepted draw
            viewModelScope.launch {
                socketClient.drawNumber()
            }
            return
        }

        // Local fallback if server not started
        val currentState = _uiState.value.gameState
        if (currentState.isGameComplete) return

        val updatedState = NumberGenerator.drawNumber(currentState)
        _uiState.value = _uiState.value.copy(gameState = updatedState)
        updatedState.calledNumbers.lastOrNull()?.let(::announceNumber)

        viewModelScope.launch {
            dataSource.save(updatedState)
        }

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playDrawSound()
        }
    }

    fun newGame() {
        stopAutoCall()
        if (_uiState.value.isServerRunning) {
            // Winner is cleared and sound plays when the server broadcasts the reset
            resetPending = true
            viewModelScope.launch {
                socketClient.requestNewGame()
            }
            return
        }

        val fresh = NumberGenerator.freshGame()
        _uiState.value = _uiState.value.copy(gameState = fresh, winnerConnectionId = null)

        viewModelScope.launch {
            dataSource.clear()
            dataSource.save(fresh)
        }

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playNewGameSound()
        }
    }

    fun toggleVoice() {
        val state = _uiState.value
        if (!state.isVoiceSupported) return
        if (state.isVoiceEnabled) announcer.stop()
        _uiState.value = state.copy(isVoiceEnabled = !state.isVoiceEnabled)
    }

    private fun announceNumber(number: Int) {
        if (_uiState.value.isVoiceEnabled) {
            announcer.announce(BingoCallPhrases.phraseFor(number))
        }
    }

    fun toggleAutoCall() {
        if (autoCaller.isRunning) stopAutoCall() else startAutoCall()
    }

    /** Ignores values not in [AutoCallIntervals.OPTIONS_SECONDS]. Retimes a running auto-call without an extra draw. */
    fun setAutoCallInterval(seconds: Int) {
        if (seconds !in AutoCallIntervals.OPTIONS_SECONDS) return
        _uiState.value = _uiState.value.copy(autoCallIntervalSeconds = seconds)
        if (autoCaller.isRunning) {
            autoCaller.start(seconds * MILLIS_PER_SECOND, tickImmediately = false)
        }
    }

    private fun startAutoCall() {
        if (!canDraw()) return
        _uiState.value = _uiState.value.copy(isAutoCalling = true)
        autoCaller.start(_uiState.value.autoCallIntervalSeconds * MILLIS_PER_SECOND, tickImmediately = true)
    }

    private fun stopAutoCall() {
        autoCaller.stop()
        if (_uiState.value.isAutoCalling) {
            _uiState.value = _uiState.value.copy(isAutoCalling = false)
        }
    }

    private fun onAutoCallTick() {
        if (canDraw()) drawNumber() else stopAutoCall()
    }

    private fun canDraw(): Boolean {
        val state = _uiState.value
        return !state.isGameComplete && state.winnerConnectionId == null
    }

    fun dismissFalseClaim() {
        _uiState.value = _uiState.value.copy(falseClaim = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(serverError = null)
    }

    override fun onCleared() {
        super.onCleared()
        autoCaller.stop()
        announcer.shutdown()
        stopServer()
    }
}
