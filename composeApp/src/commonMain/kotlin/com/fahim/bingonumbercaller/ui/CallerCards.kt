package com.fahim.bingonumbercaller.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.protocol.PlayerRef
import com.fahim.bingonumbercaller.ui.icons.GameIcons

@Composable
internal fun ResetConfirmationDialog(
    calledCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Start Fresh Game?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "This will clear all $calledCount called numbers, reset the lottery pool to all 90 balls, and start a new session. This action cannot be undone.",
                fontSize = 14.sp,
                color = CallerColors.TextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Start Fresh", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CallerColors.TextMain)
            }
        }
    )
}

@Composable
internal fun ServerErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    StatusBanner(
        message = error,
        tone = BannerTone.Error,
        trailing = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = BannerTone.Error.content, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

@Composable
internal fun WinnerCard(winnerName: String) {
    CelebrationCard(
        title = "FULL HOUSE CLAIMED!",
        subtitle = "Winner: $winnerName",
        icon = GameIcons.EmojiEvents
    )
}

@Composable
internal fun LineWinnerCard(winnerName: String) {
    StatusBanner(
        message = "Line won by $winnerName. Play on for Full House.",
        tone = BannerTone.Success,
        icon = GameIcons.EmojiEvents
    )
}

/** Players who had the prize on their ticket but didn't claim before the winner, so the caller can call it out. */
@Composable
internal fun MissedWinCard(prizeName: String, missedBy: List<PlayerRef>) {
    if (missedBy.isEmpty()) return
    StatusBanner(
        message = "Missed $prizeName: ${missedBy.joinToString(", ") { it.name }}",
        tone = BannerTone.Warning
    )
}
