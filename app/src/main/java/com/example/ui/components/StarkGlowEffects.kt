package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.DeepReactorBlue
import com.example.ui.theme.HudGridLine
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurfaceElevated

@Composable
fun rememberStarkPulse(
    durationMillis: Int = 2400,
    minValue: Float = 0.35f,
    maxValue: Float = 0.85f
): Float {
    val transition = rememberInfiniteTransition(label = "stark_hud_pulse")
    val pulse by transition.animateFloat(
        initialValue = minValue,
        targetValue = maxValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stark_pulse_value"
    )
    return pulse
}

@Composable
fun StarkHudBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glowPulse = rememberStarkPulse(durationMillis = 3600, minValue = 0.10f, maxValue = 0.22f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBg,
                        DeepReactorBlue,
                        ObsidianBg
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val step = 44.dp.toPx()

            // Subtle Stark blueprint grid
            var x = step
            while (x < w) {
                drawLine(
                    color = HudGridLine.copy(alpha = 0.24f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 0.75.dp.toPx()
                )
                x += step
            }
            var y = step
            while (y < h) {
                drawLine(
                    color = HudGridLine.copy(alpha = 0.20f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 0.75.dp.toPx()
                )
                y += step
            }

            // Upper reactor radial aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyanGlow.copy(alpha = glowPulse),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.14f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.5f, h * 0.14f),
                radius = w * 0.85f
            )

            // Bottom tactical corner vignette glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ArcCyanDim.copy(alpha = glowPulse * 1.4f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.86f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.88f, h * 0.86f),
                radius = w * 0.65f
            )
        }
        content()
    }
}

@Composable
fun StarkGlowText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ArcCyan,
    glowColor: Color = color,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val glowAlpha = rememberStarkPulse(durationMillis = 2400, minValue = 0.35f, maxValue = 0.85f)
    val glowBlur = 10f + (glowAlpha * 10f)

    Text(
        text = text,
        color = color,
        style = style.copy(
            shadow = Shadow(
                color = glowColor.copy(alpha = glowAlpha),
                offset = Offset.Zero,
                blurRadius = glowBlur
            )
        ),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}

fun Modifier.starkPulseGlow(
    glowColor: Color = ArcCyan,
    cornerRadius: Dp = 16.dp,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this
    val pulse = rememberStarkPulse(durationMillis = 2200, minValue = 0.18f, maxValue = 0.55f)
    drawBehind {
        val radiusPx = cornerRadius.toPx()
        drawRoundRect(
            color = glowColor.copy(alpha = pulse * 0.35f),
            cornerRadius = CornerRadius(radiusPx + 4.dp.toPx(), radiusPx + 4.dp.toPx()),
            style = Stroke(width = 5.dp.toPx())
        )
        drawRoundRect(
            color = glowColor.copy(alpha = pulse),
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun StarkGlowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = ArcCyan,
    containerColor: Color = SlateSurfaceElevated,
    contentColor: Color = ArcCyanBright,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(),
        label = "stark_button_scale"
    )
    val glowAlpha = rememberStarkPulse(durationMillis = 2200, minValue = 0.35f, maxValue = 0.85f)

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        contentPadding = contentPadding,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(
                colors = if (enabled) {
                    listOf(
                        accentColor.copy(alpha = glowAlpha),
                        ArcCyanBright.copy(alpha = (glowAlpha + 0.15f).coerceAtMost(1f)),
                        accentColor.copy(alpha = glowAlpha)
                    )
                } else {
                    listOf(HudGridLine, HudGridLine)
                }
            )
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SlateSurfaceElevated.copy(alpha = 0.55f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .scale(scale)
            .starkPulseGlow(
                glowColor = accentColor,
                cornerRadius = 14.dp,
                enabled = enabled
            ),
        content = content
    )
}

@Composable
fun StarkGlowFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = ArcCyan,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val pulse = rememberStarkPulse(durationMillis = 2000, minValue = 0.30f, maxValue = 0.80f)
    Box(
        modifier = modifier
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = pulse * 0.45f),
                            Color.Transparent
                        )
                    ),
                    radius = this.size.maxDimension * 0.78f
                )
            }
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = accentColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            modifier = Modifier.border(
                width = 1.5.dp,
                color = ArcCyanBright.copy(alpha = pulse),
                shape = CircleShape
            ),
            content = content
        )
    }
}

@Composable
fun StarkButtonLabel(
    text: String,
    color: Color = ArcCyanBright,
    glowColor: Color = color
) {
    val glowAlpha = rememberStarkPulse(durationMillis = 2200, minValue = 0.3f, maxValue = 0.75f)
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            shadow = Shadow(
                color = glowColor.copy(alpha = glowAlpha),
                offset = Offset.Zero,
                blurRadius = 10f
            )
        ),
        color = color
    )
}
