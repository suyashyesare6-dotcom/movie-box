package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JarvisColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = TextOnAccent,
    primaryContainer = ArcCyanDim,
    onPrimaryContainer = ArcCyanBright,
    secondary = StarkGold,
    onSecondary = TextOnAccent,
    secondaryContainer = StarkGoldDim,
    onSecondaryContainer = TextPrimary,
    tertiary = SecureEmerald,
    onTertiary = TextOnAccent,
    tertiaryContainer = SecureEmeraldDim,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceElevated,
    surfaceContainerHighest = SlateSurfaceBright,
    outline = HudOutline,
    outlineVariant = HudGridLine,
    error = AlertCrimson,
    onError = TextPrimary,
    errorContainer = AlertCrimsonDim,
    onErrorContainer = TextPrimary
)

val JarvisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content
    )
}
