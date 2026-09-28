package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.ui.icons.BingoIcons

/**
 * Back, title and the two header actions on one line. Sized to fit a 360dp-wide phone:
 * the called count lives in [ProgressStatusCard] rather than here.
 */
@Composable
internal fun CallerHeader(
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResetClick: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        BackButton(onClick = onBackClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.xs)
        ) {
            Text(
                text = "Bingo Caller",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CallerColors.ActiveGreen)
                )
                Text(
                    text = "LIVE SESSION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.ActiveGreen,
                    letterSpacing = 1.sp
                )
            }
        }

        CircleIconButton(
            icon = if (isSoundEnabled) BingoIcons.VolumeUp else BingoIcons.VolumeOff,
            contentDescription = if (isSoundEnabled) "Mute sounds" else "Unmute sounds",
            onClick = onToggleSound,
            tint = if (isSoundEnabled) CallerColors.TextMain else CallerColors.Primary,
            containerColor = if (isSoundEnabled) Color.White else Color(0xFFFFE4E6),
            borderColor = if (isSoundEnabled) OutlineColor else Color(0xFFFDA4AF)
        )

        CircleIconButton(
            icon = BingoIcons.RestartAlt,
            contentDescription = "New game",
            onClick = onResetClick
        )
    }
}

@Composable
internal fun ProgressStatusCard(
    calledCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) calledCount.toFloat() / totalCount else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CallerColors.SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CallerColors.ActiveGreen)
            )
            Text(
                text = "Cage Active",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.ActiveGreen
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(70.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CallerColors.Primary,
                trackColor = Color(0xFFDCE2F8)
            )
            Text(
                text = "$calledCount / $totalCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.TextMuted
            )
        }
    }
}
