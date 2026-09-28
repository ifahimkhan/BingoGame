package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.viewmodel.CallerUiState

enum class HistoryViewMode {
    CALL_ORDER,
    GRID_90
}

@Composable
internal fun HistorySection(
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
                verticalAlignment = Alignment.CenterVertically,
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
                                verticalAlignment = Alignment.CenterVertically,
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
            verticalAlignment = Alignment.CenterVertically,
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
internal fun ColumnCountsSummary(calledNumbers: List<Int>) {
    val bCount = calledNumbers.count { it in 1..15 }
    val iCount = calledNumbers.count { it in 16..30 }
    val nCount = calledNumbers.count { it in 31..45 }
    val gCount = calledNumbers.count { it in 46..60 }
    val oCount = calledNumbers.count { it in 61..75 }
    val starCount = calledNumbers.count { it in 76..90 }

    Row(
        verticalAlignment = Alignment.CenterVertically,
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
internal fun ColumnBadge(
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
internal fun GridBoard(calledNumbers: List<Int>) {
    val calledSet = remember(calledNumbers) { calledNumbers.toSet() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until 9) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
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
