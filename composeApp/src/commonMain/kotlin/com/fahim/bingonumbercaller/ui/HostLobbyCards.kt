package com.fahim.bingonumbercaller.ui

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.protocol.PlayerSummary
import com.fahim.bingonumbercaller.protocol.Prize
import com.fahim.bingonumbercaller.protocol.ServerMessage
import androidx.compose.material3.Icon
import com.fahim.bingonumbercaller.ui.icons.BingoIcons
import com.fahim.bingonumbercaller.ui.icons.GameIcons

private val OfflineGrey = Color(0xFF94A3B8)
private val CardBorder = Color(0xFFE2E7FF)
private val WarningBackground = Color(0xFFFFF7ED)
private val WarningBorder = Color(0xFFFB923C)
private val WarningText = Color(0xFF9A3412)

/** Who is seated at the table, whether they are online, and who is waiting for the next game. */
@Composable
fun LobbyCard(players: List<PlayerSummary>, modifier: Modifier = Modifier) {
    val online = players.count { it.isConnected }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(
                imageVector = GameIcons.Groups,
                contentDescription = null,
                tint = CallerColors.Primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Players",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.TextMain,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$online online · ${players.size} seated",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CallerColors.TextMuted
            )
        }
        if (players.isEmpty()) {
            Text(
                text = "No one has joined yet. Players scan the QR code above.",
                fontSize = 12.sp,
                color = CallerColors.TextMuted
            )
        }
        players.forEach { player -> LobbyRow(player) }
    }
}

@Composable
private fun LobbyRow(player: PlayerSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (player.isConnected) CallerColors.ActiveGreen else OfflineGrey)
        )
        Text(
            text = player.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (player.isConnected) CallerColors.TextMain else OfflineGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val status = when {
            !player.isConnected -> "offline"
            !player.hasTicket -> "next game"
            else -> null
        }
        status?.let {
            Text(text = it, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CallerColors.TextMuted)
        }
    }
}

/** A player called Full House too early. The caller can announce it, as in a live game. */
@Composable
fun FalseClaimCard(
    notice: ServerMessage.FalseClaim,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningBackground)
            .border(1.5.dp, WarningBorder, RoundedCornerShape(16.dp))
            .padding(start = Spacing.lg, top = Spacing.md, end = Spacing.sm, bottom = Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(
                imageVector = BingoIcons.WarningAmber,
                contentDescription = null,
                tint = WarningText,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = when (notice.prize) {
                    Prize.LINE -> "False line claim by ${notice.playerName}"
                    Prize.FULL_HOUSE -> "False claim by ${notice.playerName}"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = WarningText
            )
        }
        Text(
            text = "Not yet called: ${notice.uncalledNumbers.joinToString(", ")}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = WarningText
        )
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Dismiss", color = WarningText, fontWeight = FontWeight.Bold)
        }
    }
}
