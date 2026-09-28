package com.fahim.bingonumbercaller.viewmodel

import androidx.lifecycle.ViewModel
import com.fahim.bingonumbercaller.domain.PlayerNames
import com.fahim.bingonumbercaller.model.ConnectionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JoinUiState(
    val manualInput: String = "",
    val playerName: String = "",
    val isConnecting: Boolean = false,
    val errorMessage: String? = null
)

class JoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(JoinUiState())
    val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

    /** Capped at the shared limit so the field never holds more than the server will keep. */
    fun onPlayerNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(playerName = newName.take(PlayerNames.MAX_LENGTH))
    }

    fun onManualInputChanged(newInput: String) {
        _uiState.value = _uiState.value.copy(
            manualInput = newInput,
            errorMessage = null
        )
    }

    fun onQrCodeScanned(payload: String, onConnect: (ConnectionInfo) -> Unit) {
        val info = ConnectionInfo.parse(payload)
        if (info != null) {
            _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)
            onConnect(info)
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid QR code. Please scan a valid Bingo Live host code."
            )
        }
    }

    fun onManualConnectClicked(onConnect: (ConnectionInfo) -> Unit) {
        val input = _uiState.value.manualInput.trim()
        if (input.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter the host IP and port (e.g. 192.168.1.5:8080)")
            return
        }

        val info = ConnectionInfo.parse(input)
        if (info != null) {
            _uiState.value = _uiState.value.copy(isConnecting = true, errorMessage = null)
            onConnect(info)
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not parse host address. Use format: IP:PORT (e.g. 192.168.1.10:8080)"
            )
        }
    }

    /** Player came back from a game; the join form must be usable again. */
    fun onReturnedFromGame() {
        _uiState.value = _uiState.value.copy(isConnecting = false, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
