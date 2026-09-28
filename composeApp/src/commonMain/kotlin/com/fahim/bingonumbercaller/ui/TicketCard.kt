package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.model.Ticket
import com.fahim.bingonumbercaller.viewmodel.AnswerSheetUiState
import androidx.compose.material3.Icon
import com.fahim.bingonumbercaller.ui.icons.BingoIcons
import com.fahim.bingonumbercaller.ui.icons.GameIcons

@Composable
internal fun TicketCard(
    ticket: Ticket?,
    isWaitingForNextGame: Boolean,
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
                if (isWaitingForNextGame) {
                    Text(
                        text = "Game already in progress.\nYou'll get a ticket when the host starts the next game.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CallerColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    CircularProgressIndicator(color = CallerColors.Primary)
                }
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
                            verticalAlignment = Alignment.CenterVertically,
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
internal fun TicketCell(
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
                // Shadow before clip so the lift on marked cells is drawn outside the cell
                .then(
                    if (isMarked) {
                        Modifier.shadow(2.dp, RoundedCornerShape(6.dp))
                    } else Modifier
                )
                .clip(RoundedCornerShape(6.dp))
                .background(cellColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
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
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = BingoIcons.Check,
                        contentDescription = null,
                        tint = CallerColors.forNumber(number),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ClaimFullHouseButton(
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
                if (uiState.isTicketComplete) {
                    Icon(
                        imageVector = GameIcons.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                }
                Text(
                    text = if (uiState.isTicketComplete) "CLAIM FULL HOUSE!" else "CLAIM FULL HOUSE (15/15 Needed)",
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
