package com.fahim.bingonumbercaller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.bingonumbercaller.viewmodel.CallerUiState
import com.fahim.bingonumbercaller.viewmodel.CallerViewModel

enum class HistoryViewMode {
    CALL_ORDER,
    GRID_90
}

object CallerColors {
    val Background = Color(0xFFFAF8FF)
    val SurfaceCard = Color(0xFFF2F3FF)
    val SurfaceCardHigh = Color(0xFFEAEDFF)
    val Primary = Color(0xFFB80035)
    val PrimaryGlow = Color(0xFFE11D48)
    val TextMain = Color(0xFF131B2E)
    val TextMuted = Color(0xFF5C3F40)
    val ActiveGreen = Color(0xFF006848)
    val Amber = Color(0xFFFEA619)
    val AmberDark = Color(0xFF684000)

    fun forNumber(num: Int): Color = when (num) {
        in 1..15 -> Color(0xFF2563EB)   // B - Royal Blue
        in 16..30 -> Color(0xFFE11D48)  // I - Crimson Red
        in 31..45 -> Color(0xFFD97706)  // N - Amber
        in 46..60 -> Color(0xFF059669)  // G - Emerald Green
        in 61..75 -> Color(0xFF7C3AED)  // O - Amethyst Purple
        else -> Color(0xFFB45309)       // 76-90 - Special Tier
    }

    fun letterForNumber(num: Int): String = when (num) {
        in 1..15 -> "B"
        in 16..30 -> "I"
        in 31..45 -> "N"
        in 46..60 -> "G"
        in 61..75 -> "O"
        else -> "★"
    }
}

@Composable
fun CallerScreen(
    viewModel: CallerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(HistoryViewMode.CALL_ORDER) }

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
                calledCount = uiState.calledNumbers.size,
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onResetClick = { showResetDialog = true }
            )


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

@Composable
private fun CallerHeader(
    calledCount: Int,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CallerColors.Primary)
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "B",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }

            Column {
                Text(
                    text = "Bingo Caller",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMain
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Count Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(CallerColors.SurfaceCardHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$calledCount / 90",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CallerColors.Primary
                )
            }

            // Sound Toggle Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSoundEnabled) Color.White else Color(0xFFFFE4E6))
                    .clickable { onToggleSound() }
                    .border(
                        1.dp,
                        if (isSoundEnabled) Color(0xFFE2E8F0) else Color(0xFFFDA4AF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSoundEnabled) "🔊" else "🔇",
                    fontSize = 15.sp
                )
            }

            // Reset Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onResetClick() }
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↺",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun ProgressStatusCard(
    calledCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount > 0) calledCount.toFloat() / totalCount else 0f
    val percentage = (progress * 100).toInt()

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
            Text(
                text = "•",
                fontSize = 12.sp,
                color = CallerColors.TextMuted
            )
            Text(
                text = "Standard 90",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CallerColors.TextMain
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
                text = "$percentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CallerColors.TextMuted
            )
        }
    }
}

@Composable
private fun HeroBallStage(
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
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ballColor.copy(alpha = 0.9f),
                                    ballColor,
                                    ballColor.copy(red = ballColor.red * 0.7f, green = ballColor.green * 0.7f, blue = ballColor.blue * 0.7f)
                                )
                            )
                        )
                        .shadow(12.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner White Inset Disk
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape),
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
private fun DrawNumberButton(
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
                    Text(
                        text = if (!isGameComplete) "▶" else "✓",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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

@Composable
private fun HistorySection(
    uiState: CallerUiState,
    viewMode: HistoryViewMode,
    onViewModeChange: (HistoryViewMode) -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header with Segmented Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Called History",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMain
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(CallerColors.Primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${uiState.calledNumbers.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.Primary
                    )
                }
            }

            // Segmented Switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(CallerColors.SurfaceCard)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val callOrderBg = if (viewMode == HistoryViewMode.CALL_ORDER) Color.White else Color.Transparent
                val callOrderText = if (viewMode == HistoryViewMode.CALL_ORDER) CallerColors.Primary else CallerColors.TextMuted

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(callOrderBg)
                        .clickable { onViewModeChange(HistoryViewMode.CALL_ORDER) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Call Order",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = callOrderText
                    )
                }

                val gridBg = if (viewMode == HistoryViewMode.GRID_90) Color.White else Color.Transparent
                val gridText = if (viewMode == HistoryViewMode.GRID_90) CallerColors.Primary else CallerColors.TextMuted

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(gridBg)
                        .clickable { onViewModeChange(HistoryViewMode.GRID_90) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Grid 1-90",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = gridText
                    )
                }
            }
        }

        // Ledger Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CallerColors.SurfaceCard)
                .padding(14.dp)
        ) {
            if (uiState.calledNumbers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No numbers called yet. Tap 'Draw Next Number' to begin.",
                        fontSize = 13.sp,
                        color = CallerColors.TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                when (viewMode) {
                    HistoryViewMode.CALL_ORDER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Recent Calls Horizontal Scroll
                            val reversedCalls = remember(uiState.calledNumbers) {
                                uiState.calledNumbers.reversed()
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                reversedCalls.forEachIndexed { index, number ->
                                    val isLatest = index == 0
                                    val ballColor = CallerColors.forNumber(number)
                                    val letter = CallerColors.letterForNumber(number)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (isLatest) 54.dp else 48.dp)
                                                .clip(CircleShape)
                                                .background(ballColor)
                                                .then(
                                                    if (isLatest) {
                                                        Modifier.border(2.dp, CallerColors.PrimaryGlow, CircleShape)
                                                    } else Modifier
                                                )
                                                .shadow(3.dp, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = letter,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = if (isLatest) 9.sp else 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = number.toString(),
                                                    color = Color.White,
                                                    fontSize = if (isLatest) 16.sp else 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isLatest) "Latest" else "#${uiState.calledNumbers.size - index}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isLatest) CallerColors.Primary else CallerColors.TextMuted
                                        )
                                    }
                                }
                            }

                            // Column summaries
                            ColumnCountsSummary(calledNumbers = uiState.calledNumbers)
                        }
                    }

                    HistoryViewMode.GRID_90 -> {
                        GridBoard(calledNumbers = uiState.calledNumbers)
                    }
                }
            }
        }

        // Secondary Action: Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onResetClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Text(
                    text = "Start New Game",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ColumnCountsSummary(calledNumbers: List<Int>) {
    val bCount = calledNumbers.count { it in 1..15 }
    val iCount = calledNumbers.count { it in 16..30 }
    val nCount = calledNumbers.count { it in 31..45 }
    val gCount = calledNumbers.count { it in 46..60 }
    val oCount = calledNumbers.count { it in 61..75 }
    val starCount = calledNumbers.count { it in 76..90 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ColumnBadge(letter = "B", count = bCount, total = 15, color = CallerColors.forNumber(1))
        ColumnBadge(letter = "I", count = iCount, total = 15, color = CallerColors.forNumber(16))
        ColumnBadge(letter = "N", count = nCount, total = 15, color = CallerColors.forNumber(31))
        ColumnBadge(letter = "G", count = gCount, total = 15, color = CallerColors.forNumber(46))
        ColumnBadge(letter = "O", count = oCount, total = 15, color = CallerColors.forNumber(61))
        ColumnBadge(letter = "★", count = starCount, total = 15, color = CallerColors.forNumber(76))
    }
}

@Composable
private fun ColumnBadge(
    letter: String,
    count: Int,
    total: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = letter,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = color
            )
            Text(
                text = "$count/$total",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CallerColors.TextMain
            )
        }
    }
}

@Composable
private fun GridBoard(calledNumbers: List<Int>) {
    val calledSet = remember(calledNumbers) { calledNumbers.toSet() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until 9) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 1..10) {
                    val num = row * 10 + col
                    val isCalled = calledSet.contains(num)
                    val color = if (isCalled) CallerColors.forNumber(num) else Color.White

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(
                                width = 1.dp,
                                color = if (isCalled) Color.Transparent else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = num.toString(),
                            fontSize = 11.sp,
                            fontWeight = if (isCalled) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCalled) Color.White else CallerColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
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
