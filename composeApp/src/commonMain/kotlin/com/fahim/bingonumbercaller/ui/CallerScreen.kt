package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel

@Composable
fun CallerScreen(
    viewModel: CallerViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(HistoryViewMode.CALL_ORDER) }
    val requestHostingPermission = rememberHostingPermissionRequest()

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CallerColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            CallerHeader(
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onResetClick = { showResetDialog = true },
                onBackClick = onNavigateBack
            )

            // Server Error Banner
            if (uiState.serverError != null) {
                ServerErrorCard(
                    error = uiState.serverError ?: "",
                    onDismiss = { viewModel.clearError() }
                )
            }

            // Winner Banner
            if (uiState.winnerConnectionId != null) {
                WinnerCard(winnerName = uiState.winnerName ?: "A player")
            }

            // Bogus Full House call, for the caller to announce
            uiState.falseClaim?.let { notice ->
                FalseClaimCard(notice = notice, onDismiss = { viewModel.dismissFalseClaim() })
            }

            // Multiplayer Host & QR Discovery Section
            MultiplayerHostSection(
                uiState = uiState,
                onStartGame = {
                    requestHostingPermission()
                    viewModel.startServerAndGame()
                },
                onStopGame = { viewModel.stopServer() }
            )

            if (uiState.isServerRunning) {
                LobbyCard(players = uiState.players)
            }

            // Progress Bar
            ProgressStatusCard(
                calledCount = uiState.calledNumbers.size,
                totalCount = 90
            )

            // Hero Bingo Ball Stage
            HeroBallStage(
                uiState = uiState
            )

            // Primary Draw Action Button
            DrawNumberButton(
                isGameComplete = uiState.isGameComplete,
                remainingCount = uiState.remainingPool.size,
                onClick = { viewModel.drawNumber() }
            )

            AutoCallControls(
                isAutoCalling = uiState.isAutoCalling,
                intervalSeconds = uiState.autoCallIntervalSeconds,
                canDraw = !uiState.isGameComplete && uiState.winnerConnectionId == null,
                isVoiceSupported = uiState.isVoiceSupported,
                isVoiceEnabled = uiState.isVoiceEnabled,
                onToggleAutoCall = { viewModel.toggleAutoCall() },
                onIntervalSelected = { viewModel.setAutoCallInterval(it) },
                onToggleVoice = { viewModel.toggleVoice() }
            )

            // Called Numbers Ledger
            HistorySection(
                uiState = uiState,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onResetClick = { showResetDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showResetDialog) {
            ResetConfirmationDialog(
                calledCount = uiState.calledNumbers.size,
                onConfirm = {
                    viewModel.newGame()
                    showResetDialog = false
                },
                onDismiss = { showResetDialog = false }
            )
        }
    }
}
