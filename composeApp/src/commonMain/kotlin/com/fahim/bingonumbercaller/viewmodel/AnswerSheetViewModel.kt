package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.bingonumbercaller.domain.WinRules
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import com.fahim.bingonumbercaller.network.ConnectErrorMessages
import com.fahim.bingonumbercaller.network.GameSocketClient
import com.fahim.bingonumbercaller.network.PlayerConnection
import com.fahim.bingonumbercaller.protocol.Prize
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the line prize went for this player, once someone has won it. */
enum class LineOutcome {
    WON,

    /** This player also had a fully called row but someone else claimed first. */
    MISSED,

    /** Someone else won and this player had no complete row. */
    LOST
}

data class AnswerSheetUiState(
    val ticket: Ticket? = null,
    val playerId: String? = null,
    val calledNumbers: List<Int> = emptyList(),
    val currentNumber: Int? = null,
    val gameStatus: String = "WAITING",
    val claimResult: String? = null, // null, "pending", "rejected: <reason>", "won", "game_over"
    val winnerConnectionId: String? = null,
    val winnerName: String? = null,
    val playerName: String? = null,
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val isWaitingForNextGame: Boolean = false,
    val errorMessage: String? = null,
    val userCheckedNumbers: Set<Int> = emptySet(),
    val feedbackMessage: String? = null,
    val lineWinnerName: String? = null,
    val lineOutcome: LineOutcome? = null,
    val isLineClaimPending: Boolean = false,
    // Every number on this ticket was called but another player claimed Full House first
    val missedFullHouse: Boolean = false
) {
    // Only numbers that the player manually checked AND that have actually been called by the server
    val markedNumbers: Set<Int>
        get() = userCheckedNumbers.intersect(calledNumbers.toSet())

    val isTicketComplete: Boolean
        get() = ticket != null && ticket.allNumbers.isNotEmpty() &&
                markedNumbers.size == ticket.allNumbers.size &&
                ticket.allNumbers.all { markedNumbers.contains(it) }

    /** Rows the player has fully marked; only called numbers count as marked. */
    val completedRows: List<Int>
        get() = ticket?.let { WinRules.completedRows(it, markedNumbers) }.orEmpty()

    val canClaimLine: Boolean
        get() = completedRows.isNotEmpty() && lineWinnerName == null && !isLineClaimPending &&
                gameStatus != STATUS_COMPLETE

    fun isNumberCalled(num: Int): Boolean = calledNumbers.contains(num)

    fun isNumberChecked(num: Int): Boolean = userCheckedNumbers.contains(num)
}

/** 1s, 2s, 4s, 8s, then every 10s. */
internal fun reconnectDelayMillis(attempt: Int): Long =
    (1_000L shl attempt.coerceAtMost(4)).coerceAtMost(MAX_RECONNECT_DELAY_MILLIS)

private const val MAX_RECONNECT_DELAY_MILLIS = 10_000L
private const val FIRST_CONNECT_RETRIES = 3
private const val STATUS_COMPLETE = "COMPLETE"
private const val FEEDBACK_MILLIS = 2_000L
private const val CLAIM_REJECTED_MILLIS = 3_500L

/** Line state for a fresh ticket / new game. */
private fun AnswerSheetUiState.withLineReset() = copy(
    lineWinnerName = null,
    lineOutcome = null,
    isLineClaimPending = false,
    missedFullHouse = false
)

class AnswerSheetViewModel(
    private val connection: PlayerConnection = GameSocketClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnswerSheetUiState())
    val uiState: StateFlow<AnswerSheetUiState> = _uiState.asStateFlow()

    // Game currently joined (or being joined) and this player's private seat token for it
    private var activeGame: ConnectionInfo? = null
    private var rejoinToken: String? = null
    private var requestedName: String? = null

    // True while the player is on the answer sheet and wants to stay in the game
    private var shouldStayConnected = false

    // Reconnect only after the server has seated us at least once
    private var hasJoined = false
    private var reconnectJob: Job? = null

    init {
        observeConnection()
        observeMessages()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            connection.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
                if (!connected && hasJoined && shouldStayConnected) {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            connection.incomingMessages.collect { message ->
                handleServerMessage(message)
            }
        }
    }

    fun connect(connectionInfo: ConnectionInfo, playerName: String? = null) {
        requestedName = playerName
        if (connectionInfo != activeGame) {
            // A seat token only means something to the server that issued it
            rejoinToken = null
        }
        activeGame = connectionInfo
        shouldStayConnected = true
        hasJoined = false
        _uiState.update { it.copy(errorMessage = null) }

        reconnectJob?.cancel()
        // Held in reconnectJob so leaveGame() also stops the first-connect retries
        reconnectJob = viewModelScope.launch {
            var attempt = 0
            while (true) {
                try {
                    connection.connectAsPlayer(connectionInfo, rejoinToken, requestedName)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (attempt >= FIRST_CONNECT_RETRIES || !shouldStayConnected) {
                        _uiState.update {
                            it.copy(errorMessage = ConnectErrorMessages.forPlayer(e, connectionInfo))
                        }
                        return@launch
                    }
                    // A brief Wi-Fi blip shouldn't fail the join; back off and try again
                    delay(reconnectDelayMillis(attempt++))
                }
            }
        }
    }

    /** Player left the answer sheet: stop reconnecting and close the socket. The seat token is kept. */
    fun leaveGame() {
        stopReconnecting()
        hasJoined = false
        _uiState.value = AnswerSheetUiState()
        viewModelScope.launch {
            connection.disconnect()
        }
    }

    private fun stopReconnecting() {
        shouldStayConnected = false
        reconnectJob?.cancel()
        reconnectJob = null
    }

    /**
     * The app came back to the foreground (screen unlocked, app reopened). If the socket died
     * while we were away, retry now instead of waiting out the backoff.
     */
    fun onAppForegrounded() {
        if (!shouldStayConnected || !hasJoined || connection.isConnected.value) return
        reconnectJob?.cancel()
        reconnectJob = null
        scheduleReconnect(immediately = true)
    }

    private fun scheduleReconnect(immediately: Boolean = false) {
        if (reconnectJob?.isActive == true) return
        val game = activeGame ?: return

        reconnectJob = viewModelScope.launch {
            _uiState.update { it.copy(isReconnecting = true) }
            var attempt = 0
            var waitFirst = !immediately
            while (shouldStayConnected && !connection.isConnected.value) {
                if (waitFirst) delay(reconnectDelayMillis(attempt++))
                waitFirst = true
                if (!shouldStayConnected) break
                try {
                    connection.connectAsPlayer(game, rejoinToken, requestedName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Host still unreachable; back off and retry
                }
            }
            _uiState.update { it.copy(isReconnecting = false) }
        }
    }

    private fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Joined -> onJoined(message)

            is ServerMessage.JoinRejected -> {
                stopReconnecting()
                _uiState.update {
                    it.copy(errorMessage = message.reason, isReconnecting = false)
                }
            }

            is ServerMessage.GameStateUpdate -> {
                _uiState.update { current ->
                    val isNewGame = message.calledNumbers.isEmpty() && current.calledNumbers.isNotEmpty()
                    val base = if (isNewGame) current.withLineReset() else current
                    base.copy(
                        calledNumbers = message.calledNumbers,
                        currentNumber = message.currentNumber,
                        gameStatus = message.status,
                        lineWinnerName = message.lineWinnerName,
                        userCheckedNumbers = if (isNewGame) emptySet() else current.userCheckedNumbers,
                        claimResult = if (isNewGame) null else current.claimResult,
                        winnerName = if (isNewGame) null else current.winnerName
                    )
                }
            }

            is ServerMessage.GameOver -> {
                _uiState.update { current ->
                    val wasPending = current.claimResult == "pending"
                    val isMe = current.playerId != null && current.playerId == message.winnerPlayerId
                    current.copy(
                        gameStatus = STATUS_COMPLETE,
                        winnerConnectionId = message.winnerConnectionId,
                        winnerName = message.winnerName,
                        claimResult = if (wasPending || isMe) "won" else "game_over",
                        missedFullHouse = message.missedBy.any { it.playerId == current.playerId },
                        isLineClaimPending = false
                    )
                }
            }

            is ServerMessage.LineWon -> onLineWon(message)

            is ServerMessage.ClaimRejected -> when (message.prize) {
                Prize.LINE -> {
                    _uiState.update { it.copy(isLineClaimPending = false) }
                    showFeedback(message.reason, CLAIM_REJECTED_MILLIS)
                }

                Prize.FULL_HOUSE -> {
                    _uiState.update { it.copy(claimResult = "rejected: ${message.reason}") }
                    viewModelScope.launch {
                        delay(CLAIM_REJECTED_MILLIS)
                        if (_uiState.value.claimResult?.startsWith("rejected") == true) {
                            _uiState.update { it.copy(claimResult = null) }
                        }
                    }
                }
            }

            is ServerMessage.ErrorMessage -> {
                _uiState.update { it.copy(errorMessage = message.reason) }
            }

            // Host-facing messages; players don't show the lobby or others' false claims
            is ServerMessage.LobbyUpdate, is ServerMessage.FalseClaim -> Unit
        }
    }

    private fun onLineWon(message: ServerMessage.LineWon) {
        _uiState.update { current ->
            val me = current.playerId
            val outcome = when {
                me != null && me == message.winnerPlayerId -> LineOutcome.WON
                message.missedBy.any { it.playerId == me } -> LineOutcome.MISSED
                else -> LineOutcome.LOST
            }
            current.copy(
                lineWinnerName = message.winnerName,
                lineOutcome = outcome,
                isLineClaimPending = false
            )
        }
    }

    private fun onJoined(message: ServerMessage.Joined) {
        hasJoined = true
        message.rejoinToken?.let { rejoinToken = it }

        _uiState.update { current ->
            // Same ticket means a reconnect: keep the player's marks. A new ticket means a new game.
            val sameTicket = current.ticket != null && current.ticket.id == message.ticket?.id
            // A claim in flight when the socket dropped may never have arrived; let the player retry
            val keptClaim = current.claimResult.takeIf { sameTicket && it != "pending" }
            val base = if (sameTicket) current.copy(isLineClaimPending = false) else current.withLineReset()
            base.copy(
                ticket = message.ticket,
                playerId = message.playerId ?: current.playerId,
                playerName = message.playerName ?: current.playerName,
                userCheckedNumbers = if (sameTicket) current.userCheckedNumbers else emptySet(),
                claimResult = keptClaim,
                feedbackMessage = null,
                errorMessage = null,
                isConnected = true,
                isReconnecting = false,
                isWaitingForNextGame = message.ticket == null
            )
        }
    }

    fun toggleCell(number: Int) {
        val currentState = _uiState.value
        val ticket = currentState.ticket ?: return
        if (!ticket.allNumbers.contains(number)) return

        // Verify if the number has actually been called by the server
        if (!currentState.calledNumbers.contains(number)) {
            showFeedback("Number $number hasn't been called yet!", FEEDBACK_MILLIS)
            return
        }

        _uiState.update { current ->
            val checked = current.userCheckedNumbers
            current.copy(
                userCheckedNumbers = if (number in checked) checked - number else checked + number,
                feedbackMessage = null
            )
        }
    }

    private fun showFeedback(feedback: String, millis: Long) {
        _uiState.update { it.copy(feedbackMessage = feedback) }
        viewModelScope.launch {
            delay(millis)
            if (_uiState.value.feedbackMessage == feedback) {
                _uiState.update { it.copy(feedbackMessage = null) }
            }
        }
    }

    /** Sends a line claim when a row is fully marked. The server re-checks it against called numbers. */
    fun onClaimLineTapped() {
        val currentState = _uiState.value
        val ticket = currentState.ticket ?: return
        if (!currentState.canClaimLine) return
        _uiState.update { it.copy(isLineClaimPending = true) }
        viewModelScope.launch {
            connection.claimLine(ticket.id)
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun onClaimFullHouseTapped() {
        val currentState = _uiState.value
        val ticket = currentState.ticket
        if (currentState.isTicketComplete && ticket != null) {
            _uiState.update { it.copy(claimResult = "pending") }
            viewModelScope.launch {
                connection.claimFullHouse(ticket.id)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopReconnecting()
        viewModelScope.launch {
            connection.disconnect()
        }
    }
}
