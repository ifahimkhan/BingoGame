package com.fahim.bingonumbercaller.ui

import androidx.compose.ui.graphics.Color

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
