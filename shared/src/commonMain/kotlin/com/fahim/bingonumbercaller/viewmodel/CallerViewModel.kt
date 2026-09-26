package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fahim.bingonumbercaller.audio.SoundEffectPlayer
import com.fahim.bingonumbercaller.data.GameSettingsDataSource
import com.fahim.bingonumbercaller.domain.NumberGenerator
import com.fahim.bingonumbercaller.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallerUiState(
    val gameState: GameState = GameState(),
    val isLoading: Boolean = false,
    val isSoundEnabled: Boolean = true
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
    private val soundPlayer: SoundEffectPlayer = SoundEffectPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallerUiState(isLoading = true))
    val uiState: StateFlow<CallerUiState> = _uiState.asStateFlow()

    init {
        loadSavedGame()
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

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(
            isSoundEnabled = !_uiState.value.isSoundEnabled
        )
    }

    fun drawNumber() {
        val currentState = _uiState.value.gameState
        if (currentState.isGameComplete) {
            return
        }

        val updatedState = NumberGenerator.drawNumber(currentState)
        _uiState.value = _uiState.value.copy(gameState = updatedState)

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playDrawSound()
        }

        viewModelScope.launch {
            dataSource.save(updatedState)
        }
    }

    fun newGame() {
        val fresh = NumberGenerator.freshGame()
        _uiState.value = _uiState.value.copy(gameState = fresh)

        if (_uiState.value.isSoundEnabled) {
            soundPlayer.playNewGameSound()
        }

        viewModelScope.launch {
            dataSource.clear()
            dataSource.save(fresh)
        }
    }
}
