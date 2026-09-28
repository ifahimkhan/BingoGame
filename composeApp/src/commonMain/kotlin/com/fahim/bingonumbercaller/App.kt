package com.fahim.bingonumbercaller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.fahim.bingonumbercaller.ui.AnswerSheetScreen
import com.fahim.bingonumbercaller.ui.CallerColors
import com.fahim.bingonumbercaller.ui.CallerScreen
import com.fahim.bingonumbercaller.ui.JoinScreen
import com.fahim.bingonumbercaller.ui.PlatformBackHandler
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetViewModel
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel
import com.fahim.bingonumbercaller.viewmodel.JoinViewModel
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.fahim.bingonumbercaller.ui.Spacing
import com.fahim.bingonumbercaller.ui.icons.BingoIcons
import com.fahim.bingonumbercaller.ui.icons.GameIcons

enum class AppScreen {
    MODE_SELECTION,
    CALLER,
    JOIN,
    ANSWER_SHEET
}

@Composable
fun App(
    callerViewModel: CallerViewModel = viewModel { CallerViewModel() },
    joinViewModel: JoinViewModel = viewModel { JoinViewModel() },
    answerSheetViewModel: AnswerSheetViewModel = viewModel { AnswerSheetViewModel() }
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MODE_SELECTION) }

    val goHome: () -> Unit = { currentScreen = AppScreen.MODE_SELECTION }
    // Leaving the Caller screen must shut down the embedded server and host socket
    val leaveCaller: () -> Unit = {
        callerViewModel.stopServer()
        goHome()
    }

    // Leaving the answer sheet closes the socket and stops auto-reconnect; the seat token is kept
    val leaveAnswerSheet: () -> Unit = {
        answerSheetViewModel.leaveGame()
        joinViewModel.onReturnedFromGame()
        goHome()
    }

    // System back mirrors each screen's own back control; on the home screen it exits the app
    PlatformBackHandler(enabled = currentScreen != AppScreen.MODE_SELECTION) {
        when (currentScreen) {
            AppScreen.CALLER -> leaveCaller()
            AppScreen.ANSWER_SHEET -> leaveAnswerSheet()
            AppScreen.JOIN -> goHome()
            AppScreen.MODE_SELECTION -> Unit
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
            color = CallerColors.Background
        ) {
            when (currentScreen) {
                AppScreen.MODE_SELECTION -> {
                    ModeSelectionScreen(
                        onHostSelected = { currentScreen = AppScreen.CALLER },
                        onJoinSelected = { currentScreen = AppScreen.JOIN }
                    )
                }

                AppScreen.CALLER -> {
                    CallerScreen(
                        viewModel = callerViewModel,
                        onNavigateBack = leaveCaller
                    )
                }

                AppScreen.JOIN -> {
                    JoinScreen(
                        viewModel = joinViewModel,
                        onNavigateBack = { currentScreen = AppScreen.MODE_SELECTION },
                        onJoinSuccess = { connectionInfo ->
                            answerSheetViewModel.connect(
                                connectionInfo,
                                playerName = joinViewModel.uiState.value.playerName
                            )
                            currentScreen = AppScreen.ANSWER_SHEET
                        }
                    )
                }

                AppScreen.ANSWER_SHEET -> {
                    AnswerSheetScreen(
                        viewModel = answerSheetViewModel,
                        onNavigateBack = leaveAnswerSheet
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionScreen(
    onHostSelected: () -> Unit,
    onJoinSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Badge
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CallerColors.PrimaryGlow, CallerColors.Primary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "B",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Bingo Live",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = CallerColors.TextMain
        )

        Text(
            text = "Real-time multiplayer 90-ball bingo on local Wi-Fi",
            fontSize = 14.sp,
            color = CallerColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Host Card
        ModeCard(
            title = "Host Game (Caller)",
            subtitle = "Draw numbers, run the lottery cage, and broadcast to players via QR code",
            icon = GameIcons.Campaign,
            badgeText = "CALLER SCREEN",
            onClick = onHostSelected
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Join Card
        ModeCard(
            title = "Join Game (Player)",
            subtitle = "Scan the host's QR code, receive your 15-number ticket, and race to Full House",
            icon = GameIcons.ConfirmationNumber,
            badgeText = "ANSWER SHEET",
            onClick = onJoinSelected
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E7FF), RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.lg + Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CallerColors.SurfaceCardHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CallerColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(CallerColors.Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.Primary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMain
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = CallerColors.TextMuted,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = BingoIcons.ArrowForward,
                contentDescription = null,
                tint = CallerColors.Primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        Surface(color = CallerColors.Background) {
            ModeSelectionScreen(
                onHostSelected = {},
                onJoinSelected = {}
            )
        }
    }
}

@Preview
@Composable
fun ModeSelectionScreenPreview() {
    MaterialTheme {
        Surface(color = CallerColors.Background) {
            ModeSelectionScreen(
                onHostSelected = {},
                onJoinSelected = {}
            )
        }
    }
}

@Preview
@Composable
fun ModeCardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(CallerColors.Background)
                .padding(16.dp)
        ) {
            ModeCard(
                title = "Host Game (Caller)",
                subtitle = "Draw numbers, run the lottery cage, and broadcast to players via QR code",
                icon = GameIcons.Campaign,
                badgeText = "CALLER SCREEN",
                onClick = {}
            )
        }
    }
}