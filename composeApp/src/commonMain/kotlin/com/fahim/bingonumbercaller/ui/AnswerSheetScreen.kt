package com.fahim.bingonumbercaller.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetUiState
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetViewModel

@Composable
fun AnswerSheetScreen(
    viewModel: AnswerSheetViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            AnswerSheetHeader(
                isConnected = uiState.isConnected,
                gameStatus = uiState.gameStatus,
                onBackClick = onNavigateBack
            )

            // Reconnecting/Connection Warning Banner
            if (!uiState.isConnected) {
                ConnectionBanner()
            }

            // Feedback Banner (e.g. tapping an uncalled number)
            AnimatedVisibility(visible = uiState.feedbackMessage != null) {
                uiState.feedbackMessage?.let { msg ->
                    FeedbackBanner(message = msg)
                }
            }

            // Game Result / Claim Status Banner
            ResultBanner(uiState = uiState)

            // Current Number Hero Banner
            CurrentNumberStage(
                currentNumber = uiState.currentNumber,
                calledCount = uiState.calledNumbers.size
            )

            // Live Called Numbers Strip
            CalledNumbersStrip(calledNumbers = uiState.calledNumbers)

            // 9x3 Ticket Card
            TicketCard(
                ticket = uiState.ticket,
                markedNumbers = uiState.markedNumbers,
                onCellClick = { viewModel.toggleCell(it) }
            )

            // Claim Full House Action Button
            ClaimFullHouseButton(
                uiState = uiState,
                onClaim = { viewModel.onClaimFullHouseTapped() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AnswerSheetHeader(
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBackClick) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.TextMain
                    )
                }
            }

            Column {
                Text(
                    text = "Player Ticket",
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
                text = gameStatus,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = CallerColors.Primary
            )
        }
    }
}

@Composable
private fun ConnectionBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "⚠️", fontSize = 16.sp)
            Text(
                text = "Connecting to game host on local Wi-Fi...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB91C1C)
            )
        }
    }
}

@Composable
private fun FeedbackBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF3C7))
            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "⚠️", fontSize = 16.sp)
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF92400E),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultBanner(uiState: AnswerSheetUiState) {
    val result = uiState.claimResult
    AnimatedVisibility(visible = result != null) {
        when {
            result == "won" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFECFDF5))
                        .border(1.5.dp, CallerColors.ActiveGreen, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎉 BINGO! FULL HOUSE! 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = CallerColors.ActiveGreen
                        )
                        Text(
                            text = "Congratulations! You won the game!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CallerColors.ActiveGreen
                        )
                    }
                }
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
                            text = "Another player claimed Full House first.",
                            fontSize = 13.sp,
                            color = CallerColors.TextMuted
                        )
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

@Composable
private fun CurrentNumberStage(
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
private fun CalledNumbersStrip(calledNumbers: List<Int>) {
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

@Composable
private fun TicketCard(
    ticket: Ticket?,
    markedNumbers: Set<Int>,
    onCellClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your 90-Ball Ticket",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CallerColors.TextMain
                )
                Text(
                    text = "Tap numbers to mark when called",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CallerColors.TextMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (markedNumbers.size == 15) Color(0xFFECFDF5) else CallerColors.SurfaceCardHigh
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${markedNumbers.size} / 15 Marked",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (markedNumbers.size == 15) CallerColors.ActiveGreen else CallerColors.Primary
                )
            }
        }

        if (ticket == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CallerColors.SurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CallerColors.Primary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, CallerColors.SurfaceCardHigh, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until 9) {
                                val number = ticket.cells.getOrNull(row)?.getOrNull(col)
                                val isMarked = number != null && markedNumbers.contains(number)

                                TicketCell(
                                    number = number,
                                    isMarked = isMarked,
                                    onClick = { if (number != null) onCellClick(number) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCell(
    number: Int?,
    isMarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (number == null) {
        // Blank cell
        Box(
            modifier = modifier
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(6.dp))
                .background(CallerColors.SurfaceCard)
        )
    } else {
        val cellColor = if (isMarked) CallerColors.forNumber(number) else Color.White
        val borderColor = if (isMarked) Color.Transparent else Color(0xFFE2E8F0)
        val textColor = if (isMarked) Color.White else CallerColors.TextMain

        Box(
            modifier = modifier
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(6.dp))
                .background(cellColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
                )
                .then(
                    if (isMarked) {
                        Modifier.shadow(2.dp, RoundedCornerShape(6.dp))
                    } else Modifier
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // The number is ALWAYS centered and prominently visible even after checked!
            Text(
                text = number.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            // When checked: check badge in the top-right corner without overlapping or hiding the number
            if (isMarked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = CallerColors.forNumber(number),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimFullHouseButton(
    uiState: AnswerSheetUiState,
    onClaim: () -> Unit
) {
    val canClaim = uiState.isTicketComplete && uiState.claimResult != "pending" && uiState.claimResult != "won"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onClaim,
            enabled = canClaim,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (canClaim) 8.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CallerColors.Primary,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            if (uiState.claimResult == "pending") {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "VALIDATING WITH HOST...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = if (uiState.isTicketComplete) "🏆 CLAIM FULL HOUSE!" else "CLAIM FULL HOUSE (15/15 Needed)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Text(
            text = if (uiState.isTicketComplete) {
                "All 15 numbers marked! Tap to claim your win."
            } else {
                "Claim button will enable once all 15 numbers on your ticket are called."
            },
            fontSize = 12.sp,
            color = CallerColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
