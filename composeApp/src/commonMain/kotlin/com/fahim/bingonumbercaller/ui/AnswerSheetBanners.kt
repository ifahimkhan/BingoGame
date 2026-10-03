package com.fahim.bingonumbercaller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetUiState
import com.fahim.bingonumbercaller.ui.icons.BingoIcons
import com.fahim.bingonumbercaller.ui.icons.GameIcons

@Composable
internal fun AnswerSheetHeader(
    playerName: String?,
    isConnected: Boolean,
    gameStatus: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            BackButton(onClick = onBackClick)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName ?: "Player Ticket",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) CallerColors.ActiveGreen else Color(0xFFDC2626))
                    )
                    Text(
                        text = if (isConnected) "CONNECTED" else "RECONNECTING...",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) CallerColors.ActiveGreen else Color(0xFFDC2626),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(CallerColors.SurfaceCardHigh)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = gameStatusLabel(gameStatus),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CallerColors.Primary
            )
        }
    }
}

@Composable
internal fun ConnectionBanner(isReconnecting: Boolean) {
    StatusBanner(
        message = if (isReconnecting) {
            "Connection lost. Reconnecting to host, your ticket is safe..."
        } else {
            "Connecting to game host on local Wi-Fi..."
        },
        tone = BannerTone.Error,
        icon = BingoIcons.WarningAmber
    )
}

@Composable
internal fun FeedbackBanner(message: String) {
    StatusBanner(message = message, tone = BannerTone.Warning)
}

@Composable
internal fun ResultBanner(uiState: AnswerSheetUiState) {
    val result = uiState.claimResult
    AnimatedVisibility(visible = result != null) {
        when {
            result == "won" -> {
                CelebrationCard(
                    title = "BINGO! FULL HOUSE!",
                    subtitle = "Congratulations, you won the game!",
                    icon = GameIcons.EmojiEvents
                )
            }

            result == "game_over" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Game Over",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallerColors.TextMain
                        )
                        Text(
                            text = uiState.winnerName?.let { "$it claimed Full House." }
                                ?: "Another player claimed Full House first.",
                            fontSize = 13.sp,
                            color = CallerColors.TextMuted
                        )
                        if (uiState.missedFullHouse) {
                            Text(
                                text = "You missed it! All your numbers were called but you didn't claim in time.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BannerTone.Warning.content,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            result?.startsWith("rejected") == true -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF1F2))
                        .border(1.dp, Color(0xFFFDA4AF), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result.removePrefix("rejected: "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Server status codes are protocol values; show people a readable label. */
private fun gameStatusLabel(status: String): String = when (status) {
    "WAITING" -> "Waiting"
    "IN_PROGRESS" -> "Live"
    "COMPLETE" -> "Finished"
    else -> status.lowercase().replaceFirstChar { it.uppercase() }
}
