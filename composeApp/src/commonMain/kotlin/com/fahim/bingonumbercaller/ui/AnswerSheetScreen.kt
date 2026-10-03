package com.fahim.bingonumbercaller.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetViewModel

@Composable
fun AnswerSheetScreen(
    viewModel: AnswerSheetViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // A locked phone stops receiving numbers; and when the player comes back, reconnect at once
    KeepScreenOn()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

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
            AnswerSheetHeader(
                playerName = uiState.playerName,
                isConnected = uiState.isConnected,
                gameStatus = uiState.gameStatus,
                onBackClick = onNavigateBack
            )

            // Terminal problems (join rejected, connect failure) replace the connecting banner
            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                FeedbackBanner(message = errorMessage)
            } else if (!uiState.isConnected) {
                ConnectionBanner(isReconnecting = uiState.isReconnecting)
            }

            // Feedback Banner (e.g. tapping an uncalled number)
            AnimatedVisibility(visible = uiState.feedbackMessage != null) {
                uiState.feedbackMessage?.let { msg ->
                    FeedbackBanner(message = msg)
                }
            }

            // Game Result / Claim Status Banner
            ResultBanner(uiState = uiState)

            LineResultBanner(uiState = uiState)

            // Current Number Hero Banner
            CurrentNumberStage(
                currentNumber = uiState.currentNumber,
                calledCount = uiState.calledNumbers.size
            )

            // Live Called Numbers Strip
            CalledNumbersStrip(calledNumbers = uiState.calledNumbers)

            // 9x3 Ticket Card
            TicketCard(
                ticket = uiState.ticket,
                isWaitingForNextGame = uiState.isWaitingForNextGame,
                markedNumbers = uiState.markedNumbers,
                onCellClick = { viewModel.toggleCell(it) }
            )

            ClaimLineButton(
                uiState = uiState,
                onClaim = { viewModel.onClaimLineTapped() }
            )

            // Claim Full House Action Button
            ClaimFullHouseButton(
                uiState = uiState,
                onClaim = { viewModel.onClaimFullHouseTapped() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
