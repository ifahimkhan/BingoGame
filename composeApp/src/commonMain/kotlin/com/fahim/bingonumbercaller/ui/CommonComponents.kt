package com.fahim.bingonumbercaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fahim.bingonumbercaller.ui.icons.BingoIcons
import androidx.compose.foundation.layout.Column

/** Spacing scale used across screens so paddings line up. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Neutral border used by cards and outlined controls. */
val OutlineColor = Color(0xFFE2E8F0)

/**
 * Round icon button with the icon exactly centred. The visible circle is [size]; the touch target
 * is at least 48dp (Material minimum) without changing how it looks.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = CallerColors.TextMain,
    containerColor: Color = Color.White,
    borderColor: Color = OutlineColor
) {
    Box(
        modifier = modifier.minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, borderColor, CircleShape)
                .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * ICON_TO_BUTTON_RATIO)
            )
        }
    }
}

/** Round back button used at the top-left of every screen. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CircleIconButton(
        icon = BingoIcons.ArrowBack,
        contentDescription = "Back",
        onClick = onClick,
        modifier = modifier
    )
}

enum class BannerTone(val background: Color, val border: Color, val content: Color) {
    Warning(Color(0xFFFFFBEB), Color(0xFFF59E0B), Color(0xFF92400E)),
    Error(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFB91C1C)),
    Success(Color(0xFFECFDF5), Color(0xFF34D399), Color(0xFF065F46)),
    Neutral(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF334155))
}

/** One-line or wrapping status message with a leading icon, vertically centred. */
@Composable
fun StatusBanner(
    message: String,
    tone: BannerTone,
    modifier: Modifier = Modifier,
    icon: ImageVector = BingoIcons.WarningAmber,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.background)
            .border(1.dp, tone.border, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(20.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = tone.content,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Icon + label for use inside Buttons, centred on one line. */
@Composable
fun IconLabel(
    icon: ImageVector,
    text: String,
    color: Color = Color.White,
    iconSize: Dp = 18.dp,
    fontSize: Int = 14
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
        Text(text = text, color = color, fontWeight = FontWeight.Bold, fontSize = fontSize.sp)
    }
}

/** Big success card: icon badge, headline and a supporting line, all centred. */
@Composable
fun CelebrationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = CallerColors.ActiveGreen
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BannerTone.Success.background)
            .border(1.5.dp, accent, shape)
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = accent)
        Text(text = subtitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

private const val ICON_TO_BUTTON_RATIO = 0.5f
