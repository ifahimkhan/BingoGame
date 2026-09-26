package com.fahim.bingonumbercaller.connection

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrScanner(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier
)
