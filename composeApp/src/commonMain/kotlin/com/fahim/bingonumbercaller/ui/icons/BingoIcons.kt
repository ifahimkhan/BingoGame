/*
 * Icon path data copied from androidx.compose.material:material-icons (Rounded style).
 * Copyright 2020-2024 The Android Open Source Project, licensed under the Apache License 2.0.
 * Only the icons this app uses are included, instead of the multi-megabyte icon library.
 */
package com.fahim.bingonumbercaller.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector

/** Controls and status icons: navigation, playback, sound, confirmation, warnings. Tint via Icon(tint = ...). */
object BingoIcons {
    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack", autoMirror = true) {
            iconPath {
                moveTo(19.0f, 11.0f)
                horizontalLineTo(7.83f)
                lineToRelative(4.88f, -4.88f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0.0f, -1.42f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(-6.59f, 6.59f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(6.59f, 6.59f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(7.83f, 13.0f)
                horizontalLineTo(19.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                close()
            }
        }
    }

    val ArrowForward: ImageVector by lazy {
        icon("ArrowForward", autoMirror = true) {
            iconPath {
                moveTo(5.0f, 13.0f)
                horizontalLineToRelative(11.17f)
                lineToRelative(-4.88f, 4.88f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.42f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineToRelative(6.59f, -6.59f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineToRelative(-6.58f, -6.6f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineTo(16.17f, 11.0f)
                horizontalLineTo(5.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                close()
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check", autoMirror = false) {
            iconPath {
                moveTo(9.0f, 16.17f)
                lineTo(5.53f, 12.7f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(4.18f, 4.18f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineTo(20.29f, 7.71f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineTo(9.0f, 16.17f)
                close()
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close", autoMirror = false) {
            iconPath {
                moveTo(18.3f, 5.71f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineTo(12.0f, 10.59f)
                lineTo(7.11f, 5.7f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineTo(10.59f, 12.0f)
                lineTo(5.7f, 16.89f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineTo(12.0f, 13.41f)
                lineToRelative(4.89f, 4.89f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(13.41f, 12.0f)
                lineToRelative(4.89f, -4.89f)
                curveToRelative(0.38f, -0.38f, 0.38f, -1.02f, 0.0f, -1.4f)
                close()
            }
        }
    }

    val PlayArrow: ImageVector by lazy {
        icon("PlayArrow", autoMirror = false) {
            iconPath {
                moveTo(8.0f, 6.82f)
                verticalLineToRelative(10.36f)
                curveToRelative(0.0f, 0.79f, 0.87f, 1.27f, 1.54f, 0.84f)
                lineToRelative(8.14f, -5.18f)
                curveToRelative(0.62f, -0.39f, 0.62f, -1.29f, 0.0f, -1.69f)
                lineTo(9.54f, 5.98f)
                curveTo(8.87f, 5.55f, 8.0f, 6.03f, 8.0f, 6.82f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause", autoMirror = false) {
            iconPath {
                moveTo(8.0f, 19.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(10.0f, 7.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                close()
                moveTo(14.0f, 7.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(18.0f, 7.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                close()
            }
        }
    }

    val RestartAlt: ImageVector by lazy {
        icon("RestartAlt", autoMirror = false) {
            iconPath {
                moveTo(12.0f, 5.0f)
                verticalLineTo(3.21f)
                curveToRelative(0.0f, -0.45f, -0.54f, -0.67f, -0.85f, -0.35f)
                lineTo(8.35f, 5.65f)
                curveToRelative(-0.2f, 0.2f, -0.2f, 0.51f, 0.0f, 0.71f)
                lineToRelative(2.79f, 2.79f)
                curveTo(11.46f, 9.46f, 12.0f, 9.24f, 12.0f, 8.79f)
                verticalLineTo(7.0f)
                curveToRelative(3.31f, 0.0f, 6.0f, 2.69f, 6.0f, 6.0f)
                curveToRelative(0.0f, 2.72f, -1.83f, 5.02f, -4.31f, 5.75f)
                curveTo(13.27f, 18.87f, 13.0f, 19.27f, 13.0f, 19.7f)
                verticalLineToRelative(0.0f)
                curveToRelative(0.0f, 0.65f, 0.62f, 1.16f, 1.25f, 0.97f)
                curveTo(17.57f, 19.7f, 20.0f, 16.64f, 20.0f, 13.0f)
                curveTo(20.0f, 8.58f, 16.42f, 5.0f, 12.0f, 5.0f)
                close()
            }
            iconPath {
                moveTo(6.0f, 13.0f)
                curveToRelative(0.0f, -1.34f, 0.44f, -2.58f, 1.19f, -3.59f)
                curveToRelative(0.3f, -0.4f, 0.26f, -0.95f, -0.09f, -1.31f)
                lineToRelative(0.0f, 0.0f)
                curveTo(6.68f, 7.68f, 5.96f, 7.72f, 5.6f, 8.2f)
                curveTo(4.6f, 9.54f, 4.0f, 11.2f, 4.0f, 13.0f)
                curveToRelative(0.0f, 3.64f, 2.43f, 6.7f, 5.75f, 7.67f)
                curveTo(10.38f, 20.86f, 11.0f, 20.35f, 11.0f, 19.7f)
                verticalLineToRelative(0.0f)
                curveToRelative(0.0f, -0.43f, -0.27f, -0.83f, -0.69f, -0.95f)
                curveTo(7.83f, 18.02f, 6.0f, 15.72f, 6.0f, 13.0f)
                close()
            }
        }
    }

    val VolumeUp: ImageVector by lazy {
        icon("VolumeUp", autoMirror = true) {
            iconPath {
                moveTo(3.0f, 10.0f)
                verticalLineToRelative(4.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(3.0f)
                lineToRelative(3.29f, 3.29f)
                curveToRelative(0.63f, 0.63f, 1.71f, 0.18f, 1.71f, -0.71f)
                lineTo(12.0f, 6.41f)
                curveToRelative(0.0f, -0.89f, -1.08f, -1.34f, -1.71f, -0.71f)
                lineTo(7.0f, 9.0f)
                lineTo(4.0f, 9.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                close()
                moveTo(16.5f, 12.0f)
                curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(8.05f)
                curveToRelative(1.48f, -0.73f, 2.5f, -2.25f, 2.5f, -4.02f)
                close()
                moveTo(14.0f, 4.45f)
                verticalLineToRelative(0.2f)
                curveToRelative(0.0f, 0.38f, 0.25f, 0.71f, 0.6f, 0.85f)
                curveTo(17.18f, 6.53f, 19.0f, 9.06f, 19.0f, 12.0f)
                reflectiveCurveToRelative(-1.82f, 5.47f, -4.4f, 6.5f)
                curveToRelative(-0.36f, 0.14f, -0.6f, 0.47f, -0.6f, 0.85f)
                verticalLineToRelative(0.2f)
                curveToRelative(0.0f, 0.63f, 0.63f, 1.07f, 1.21f, 0.85f)
                curveTo(18.6f, 19.11f, 21.0f, 15.84f, 21.0f, 12.0f)
                reflectiveCurveToRelative(-2.4f, -7.11f, -5.79f, -8.4f)
                curveToRelative(-0.58f, -0.23f, -1.21f, 0.22f, -1.21f, 0.85f)
                close()
            }
        }
    }

    val VolumeOff: ImageVector by lazy {
        icon("VolumeOff", autoMirror = true) {
            iconPath {
                moveTo(3.63f, 3.63f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineTo(7.29f, 8.7f)
                lineTo(7.0f, 9.0f)
                lineTo(4.0f, 9.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(4.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(3.0f)
                lineToRelative(3.29f, 3.29f)
                curveToRelative(0.63f, 0.63f, 1.71f, 0.18f, 1.71f, -0.71f)
                verticalLineToRelative(-4.17f)
                lineToRelative(4.18f, 4.18f)
                curveToRelative(-0.49f, 0.37f, -1.02f, 0.68f, -1.6f, 0.91f)
                curveToRelative(-0.36f, 0.15f, -0.58f, 0.53f, -0.58f, 0.92f)
                curveToRelative(0.0f, 0.72f, 0.73f, 1.18f, 1.39f, 0.91f)
                curveToRelative(0.8f, -0.33f, 1.55f, -0.77f, 2.22f, -1.31f)
                lineToRelative(1.34f, 1.34f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(5.05f, 3.63f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.42f, 0.0f)
                close()
                moveTo(19.0f, 12.0f)
                curveToRelative(0.0f, 0.82f, -0.15f, 1.61f, -0.41f, 2.34f)
                lineToRelative(1.53f, 1.53f)
                curveToRelative(0.56f, -1.17f, 0.88f, -2.48f, 0.88f, -3.87f)
                curveToRelative(0.0f, -3.83f, -2.4f, -7.11f, -5.78f, -8.4f)
                curveToRelative(-0.59f, -0.23f, -1.22f, 0.23f, -1.22f, 0.86f)
                verticalLineToRelative(0.19f)
                curveToRelative(0.0f, 0.38f, 0.25f, 0.71f, 0.61f, 0.85f)
                curveTo(17.18f, 6.54f, 19.0f, 9.06f, 19.0f, 12.0f)
                close()
                moveTo(10.29f, 5.71f)
                lineToRelative(-0.17f, 0.17f)
                lineTo(12.0f, 7.76f)
                lineTo(12.0f, 6.41f)
                curveToRelative(0.0f, -0.89f, -1.08f, -1.33f, -1.71f, -0.7f)
                close()
                moveTo(16.5f, 12.0f)
                curveToRelative(0.0f, -1.77f, -1.02f, -3.29f, -2.5f, -4.03f)
                verticalLineToRelative(1.79f)
                lineToRelative(2.48f, 2.48f)
                curveToRelative(0.01f, -0.08f, 0.02f, -0.16f, 0.02f, -0.24f)
                close()
            }
        }
    }

    val WarningAmber: ImageVector by lazy {
        icon("WarningAmber", autoMirror = false) {
            iconPath {
                moveTo(12.0f, 5.99f)
                lineTo(19.53f, 19.0f)
                lineTo(4.47f, 19.0f)
                lineTo(12.0f, 5.99f)
                moveTo(2.74f, 18.0f)
                curveToRelative(-0.77f, 1.33f, 0.19f, 3.0f, 1.73f, 3.0f)
                horizontalLineToRelative(15.06f)
                curveToRelative(1.54f, 0.0f, 2.5f, -1.67f, 1.73f, -3.0f)
                lineTo(13.73f, 4.99f)
                curveToRelative(-0.77f, -1.33f, -2.69f, -1.33f, -3.46f, 0.0f)
                lineTo(2.74f, 18.0f)
                close()
                moveTo(11.0f, 11.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                close()
                moveTo(11.0f, 16.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                close()
            }
        }
    }
}
