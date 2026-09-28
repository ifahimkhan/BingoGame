package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CurrentNumberStage(
    currentNumber: Int?,
    calledCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, CallerColors.SurfaceCard)
                )
            )
            .border(1.dp, Color(0xFFE2E7FF), RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LATEST CALL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = CallerColors.TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (currentNumber != null) {
                        val letter = CallerColors.letterForNumber(currentNumber)
                        "$letter - $currentNumber"
                    } else {
                        "Waiting to start"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CallerColors.TextMain
                )
                Text(
                    text = "$calledCount of 90 balls called",
                    fontSize = 12.sp,
                    color = CallerColors.TextMuted
                )
            }

            // Ball Preview
            if (currentNumber != null) {
                val ballColor = CallerColors.forNumber(currentNumber)
                val letter = CallerColors.letterForNumber(currentNumber)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ballColor)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = letter,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = currentNumber.toString(),
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalledNumbersStrip(calledNumbers: List<Int>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Called Numbers History",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.TextMain
            )
            Text(
                text = "${calledNumbers.size} called",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CallerColors.TextMuted
            )
        }

        if (calledNumbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CallerColors.SurfaceCard)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No numbers called yet",
                    fontSize = 12.sp,
                    color = CallerColors.TextMuted
                )
            }
        } else {
            val reversed = remember(calledNumbers) { calledNumbers.reversed() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reversed.forEachIndexed { index, num ->
                    val isLatest = index == 0
                    val ballColor = CallerColors.forNumber(num)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ballColor)
                            .then(
                                if (isLatest) {
                                    Modifier.border(2.dp, CallerColors.PrimaryGlow, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
