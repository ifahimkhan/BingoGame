package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import com.fahim.bingonumbercaller.domain.QrCodeGenerator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.viewmodel.CallerUiState
import com.fahim.bingonumbercaller.ui.icons.GameIcons

@Composable
internal fun MultiplayerHostSection(
    uiState: CallerUiState,
    onStartGame: () -> Unit,
    onStopGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E7FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        if (!uiState.isServerRunning || uiState.connectionInfo == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Multiplayer Host",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallerColors.TextMain
                    )
                    Text(
                        text = "Start embedded server to let players join via QR code",
                        fontSize = 12.sp,
                        color = CallerColors.TextMuted
                    )
                }

                Button(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(containerColor = CallerColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    IconLabel(icon = GameIcons.WifiTethering, text = "Start Host", fontSize = 13)
                }
            }
        } else {
            val info = uiState.connectionInfo
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "LOBBY ACTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = CallerColors.ActiveGreen,
                        )
                    }

                    TextButton(onClick = onStopGame) {
                        Text("Stop Server", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // QR Code Canvas
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCanvas(
                        payload = info.toPayloadString(),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Fallback details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Manual Join Address:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CallerColors.TextMuted
                    )
                    Text(
                        text = "${info.host}:${info.port}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CallerColors.TextMain
                    )
                }
            }
        }
    }
}

@Composable
fun QrCodeCanvas(
    payload: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { QrCodeGenerator.generateQrMatrix(payload) }
    if (matrix.isEmpty()) return
    val matrixRows = matrix.size
    val matrixCols = matrix[0].size
    val quietZone = 2
    val totalRows = matrixRows + quietZone * 2
    val totalCols = matrixCols + quietZone * 2

    Canvas(modifier = modifier) {
        drawRect(color = Color.White, size = size)
        val cellWidth = size.width / totalCols
        val cellHeight = size.height / totalRows
        for (r in 0 until matrixRows) {
            for (c in 0 until matrixCols) {
                if (matrix[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset((c + quietZone) * cellWidth, (r + quietZone) * cellHeight),
                        size = androidx.compose.ui.geometry.Size(cellWidth + 0.5f, cellHeight + 0.5f)
                    )
                }
            }
        }
    }
}
