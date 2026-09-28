package com.fahim.bingonumbercaller.ui

import androidx.compose.runtime.Composable

/**
 * Returns an action that asks for whatever the platform needs to show the "Hosting" notification.
 * Android 13+: POST_NOTIFICATIONS (hosting works without it, but the notification and its
 * "Stop hosting" button stay hidden). Elsewhere: no-op.
 */
@Composable
expect fun rememberHostingPermissionRequest(): () -> Unit
