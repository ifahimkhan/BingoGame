package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.viewmodel.AutoCallIntervals
import com.fahim.bingonumbercaller.ui.icons.BingoIcons

private val ControlsBorder = Color(0xFFE2E7FF)
private val PauseRed = Color(0xFFDC2626)

/** Auto-call start/pause, its interval, and spoken number calls. */
@Composable
fun AutoCallControls(
    isAutoCalling: Boolean,
    intervalSeconds: Int,
    canDraw: Boolean,
    isVoiceSupported: Boolean,
    isVoiceEnabled: Boolean,
    onToggleAutoCall: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
    onToggleVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, ControlsBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-call", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CallerColors.TextMain)
                Text(
                    text = if (isAutoCalling) "Drawing every $intervalSeconds s" else "Draw numbers on a timer",
                    fontSize = 12.sp,
                    color = CallerColors.TextMuted
                )
            }
            Button(
                onClick = onToggleAutoCall,
                enabled = isAutoCalling || canDraw,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoCalling) PauseRed else CallerColors.Primary
                )
            ) {
                IconLabel(
                    icon = if (isAutoCalling) BingoIcons.Pause else BingoIcons.PlayArrow,
                    text = if (isAutoCalling) "Pause" else "Start"
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutoCallIntervals.OPTIONS_SECONDS.forEach { seconds ->
                IntervalChip(
                    seconds = seconds,
                    isSelected = seconds == intervalSeconds,
                    onClick = { onIntervalSelected(seconds) }
                )
            }
        }

        if (isVoiceSupported) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Announce numbers", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CallerColors.TextMain)
                    Text("e.g. \"4 and 2, 42\"", fontSize = 12.sp, color = CallerColors.TextMuted)
                }
                Switch(
                    checked = isVoiceEnabled,
                    onCheckedChange = { onToggleVoice() },
                    colors = SwitchDefaults.colors(checkedTrackColor = CallerColors.Primary)
                )
            }
        }
    }
}

@Composable
private fun IntervalChip(seconds: Int, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    Text(
        text = "${seconds}s",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color.White else CallerColors.Primary,
        modifier = Modifier
            // 48dp touch target; the visible chip stays compact
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(if (isSelected) CallerColors.Primary else CallerColors.SurfaceCardHigh)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
