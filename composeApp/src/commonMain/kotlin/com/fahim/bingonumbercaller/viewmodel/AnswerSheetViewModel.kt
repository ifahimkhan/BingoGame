package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.model.allNumbers
import com.fahim.bingonumbercaller.network.GameSocketClient
import com.fahim.bingonumbercaller.protocol.ServerMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnswerSheetUiState(
    val ticket: Ticket? = null,
    val calledNumbers: List<Int> = emptyList(),
    val currentNumber: Int? = null,
    val gameStatus: String = "WAITING",
    val claimResult: String? = null, // null, "pending", "rejected: <reason>", "won", "game_over"
    val winnerConnectionId: String? = null,
    val isConnected: Boolean = false,
    val errorMessage: String? = null,
    val userCheckedNumbers: Set<Int> = emptySet(),
    val feedbackMessage: String? = null
) {
    // Only numbers that the player manually checked AND that have actually been called by the server
    val markedNumbers: Set<Int>
        get() = userCheckedNumbers.intersect(calledNumbers.toSet())

    val isTicketComplete: Boolean
        get() = ticket != null && ticket.allNumbers.isNotEmpty() &&
                markedNumbers.size == ticket.allNumbers.size &&
                ticket.allNumbers.all { markedNumbers.contains(it) }

    fun isNumberCalled(num: Int): Boolean = calledNumbers.contains(num)

    fun isNumberChecked(num: Int): Boolean = userCheckedNumbers.contains(num)
}

class AnswerSheetViewModel(
    private val socketClient: GameSocketClient = GameSocketClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnswerSheetUiState())
    val uiState: StateFlow<AnswerSheetUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        observeMessages()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            socketClient.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isConnected = connected)
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            socketClient.incomingMessages.collect { message ->
                handleServerMessage(message)
            }
        }
    }

    fun connect(connectionInfo: ConnectionInfo) {
        viewModelScope.launch {
            try {
                socketClient.connectAsPlayer(connectionInfo)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to connect to host: ${e.message ?: "network error"}"
                )
            }
        }
    }

    private fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Joined -> {
                _uiState.value = _uiState.value.copy(
                    ticket = message.ticket,
                    userCheckedNumbers = emptySet(),
                    feedbackMessage = null,
                    claimResult = null,
                    isConnected = true
                )
            }

            is ServerMessage.GameStateUpdate -> {
                val isNewGame = message.calledNumbers.isEmpty() && _uiState.value.calledNumbers.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    calledNumbers = message.calledNumbers,
                    currentNumber = message.currentNumber,
                    gameStatus = message.status,
                    userCheckedNumbers = if (isNewGame) emptySet() else _uiState.value.userCheckedNumbers,
                    claimResult = if (isNewGame) null else _uiState.value.claimResult
                )
            }

            is ServerMessage.GameOver -> {
                val wasPending = _uiState.value.claimResult == "pending"
                _uiState.value = _uiState.value.copy(
                    gameStatus = "COMPLETE",
                    winnerConnectionId = message.winnerConnectionId,
                    claimResult = if (wasPending) "won" else "game_over"
                )
            }

            is ServerMessage.ClaimRejected -> {
                _uiState.value = _uiState.value.copy(
                    claimResult = "rejected: ${message.reason}"
                )
                viewModelScope.launch {
                    delay(3500)
                    if (_uiState.value.claimResult?.startsWith("rejected") == true) {
                        _uiState.value = _uiState.value.copy(claimResult = null)
                    }
                }
            }

            is ServerMessage.ErrorMessage -> {
                _uiState.value = _uiState.value.copy(errorMessage = message.reason)
            }
        }
    }

    fun toggleCell(number: Int) {
        val currentState = _uiState.value
        val ticket = currentState.ticket ?: return
        if (!ticket.allNumbers.contains(number)) return

        // Verify if the number has actually been called by the server
        if (!currentState.calledNumbers.contains(number)) {
            _uiState.value = currentState.copy(
                feedbackMessage = "Number $number hasn't been called yet!"
            )
            viewModelScope.launch {
                delay(2000)
                if (_uiState.value.feedbackMessage == "Number $number hasn't been called yet!") {
                    _uiState.value = _uiState.value.copy(feedbackMessage = null)
                }
            }
            return
        }

        // Toggle user check
        val currentChecked = currentState.userCheckedNumbers
        val newChecked = if (currentChecked.contains(number)) {
            currentChecked - number
        } else {
            currentChecked + number
        }

        _uiState.value = currentState.copy(
            userCheckedNumbers = newChecked,
            feedbackMessage = null
        )
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    fun onClaimFullHouseTapped() {
        val currentState = _uiState.value
        val ticket = currentState.ticket
        if (currentState.isTicketComplete && ticket != null) {
            _uiState.value = currentState.copy(claimResult = "pending")
            viewModelScope.launch {
                socketClient.claimFullHouse(ticket.id)
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            socketClient.disconnect()
        }
    }
}
