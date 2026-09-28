package com.fahim.bingonumbercaller.ui

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform's system back gesture/button while [enabled].
 * Android: hardware back and predictive back gesture. iOS: no system back, so no-op.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
