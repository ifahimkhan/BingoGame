package com.fahim.bingonumbercaller.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.ui.CallerColors
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import com.fahim.bingonumbercaller.ui.icons.GameIcons

@Composable
actual fun QrScanner(
    onResult: (String) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CallerColors.SurfaceCard)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = GameIcons.QrCodeScanner,
                contentDescription = null,
                tint = CallerColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "QR Scanner",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = CallerColors.TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Use the manual code entry field below to connect to the host.",
                fontSize = 13.sp,
                color = CallerColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
