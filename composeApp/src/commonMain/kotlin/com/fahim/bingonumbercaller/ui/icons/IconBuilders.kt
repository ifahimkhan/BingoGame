package com.fahim.bingonumbercaller.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Same 24x24 grid and black fill as the Material icon library, so icons tint cleanly.
private const val ICON_SIZE = 24f

internal fun icon(name: String, autoMirror: Boolean, paths: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = "Rounded.$name",
        defaultWidth = ICON_SIZE.dp,
        defaultHeight = ICON_SIZE.dp,
        viewportWidth = ICON_SIZE,
        viewportHeight = ICON_SIZE,
        autoMirror = autoMirror
    ).apply(paths).build()

internal fun ImageVector.Builder.iconPath(
    fillAlpha: Float = 1f,
    strokeAlpha: Float = 1f,
    pathFillType: PathFillType = PathFillType.NonZero,
    pathBuilder: PathBuilder.() -> Unit
): ImageVector.Builder = path(
    fill = SolidColor(Color.Black),
    fillAlpha = fillAlpha,
    strokeAlpha = strokeAlpha,
    pathFillType = pathFillType,
    pathBuilder = pathBuilder
)
