package com.fahim.bingonumbercaller.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberHostingPermissionRequest(): () -> Unit = remember { {} }
