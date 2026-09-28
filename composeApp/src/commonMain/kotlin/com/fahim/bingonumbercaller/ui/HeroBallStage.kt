package com.fahim.bingonumbercaller.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.viewmodel.CallerUiState
import androidx.compose.material3.Icon
import com.fahim.bingonumbercaller.ui.icons.BingoIcons

@Composable
internal fun HeroBallStage(
    uiState: CallerUiState
) {
    val currentNumber = uiState.currentNumber
    val previousNumber = if (uiState.calledNumbers.size >= 2) {
        uiState.calledNumbers[uiState.calledNumbers.size - 2]
    } else null

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, CallerColors.SurfaceCard)
                )
            )
            .border(1.dp, Color(0xFFE2E7FF), RoundedCornerShape(28.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Previous call indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (previousNumber != null) {
                    val prevLetter = CallerColors.letterForNumber(previousNumber)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(CallerColors.SurfaceCardHigh)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Previous: $prevLetter-$previousNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallerColors.TextMuted
                        )
                    }
                } else {
                    Text(
                        text = "Game in progress",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CallerColors.TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFE6F4EA))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "90-Ball Cage",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.ActiveGreen
                    )
                }
            }

            // Hero Ball Sphere
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                if (currentNumber != null) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(CallerColors.PrimaryGlow.copy(alpha = pulseAlpha))
                    )
                }

                // Ball Outer Body
                val ballColor = if (currentNumber != null) {
                    CallerColors.forNumber(currentNumber)
                } else {
                    Color(0xFF64748B)
                }

                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ballColor.copy(alpha = 0.9f),
                                    ballColor,
                                    ballColor.copy(red = ballColor.red * 0.7f, green = ballColor.green * 0.7f, blue = ballColor.blue * 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner White Inset Disk
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (currentNumber != null) {
                                val letter = CallerColors.letterForNumber(currentNumber)
                                Text(
                                    text = letter,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ballColor,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = currentNumber.toString(),
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CallerColors.TextMain,
                                    lineHeight = 52.sp
                                )
                            } else {
                                Text(
                                    text = "--",
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            // Announcement Reading Banner
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(99.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val callText = if (currentNumber != null) {
                    val letter = CallerColors.letterForNumber(currentNumber)
                    "Called: $letter - $currentNumber"
                } else {
                    "Ready to draw first ball"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (currentNumber != null) CallerColors.Primary else Color(0xFF94A3B8))
                    )
                    Text(
                        text = callText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.TextMain
                    )
                }
            }
        }
    }
}

@Composable
internal fun DrawNumberButton(
    isGameComplete: Boolean,
    remainingCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = !isGameComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (!isGameComplete) 8.dp else 0.dp,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CallerColors.Primary,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isGameComplete) BingoIcons.PlayArrow else BingoIcons.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = if (!isGameComplete) "DRAW NEXT NUMBER" else "ALL 90 NUMBERS DRAWN",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (!isGameComplete) "Tap to pick random ball" else "Game complete! Reset to play again",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Text(
            text = "$remainingCount balls remaining in lottery cage",
            fontSize = 12.sp,
            color = CallerColors.TextMuted,
            fontWeight = FontWeight.Medium
        )
    }
}
