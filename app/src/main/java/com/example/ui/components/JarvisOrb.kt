package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.JarvisMode
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.HudOutline
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun JarvisArcOrb(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 116.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 2200 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 3400 else 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_counter_rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening || isSpeaking) 650 else 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val ringColor = when {
        isListening -> StarkGold
        isSpeaking -> ArcCyanBright
        isThinking -> NeonCyanGlow
        else -> ArcCyan
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("jarvis_voice_orb")
            .semantics {
                contentDescription = when {
                    isListening -> "Stop listening"
                    isSpeaking -> "Stop speaking"
                    else -> "Speak to JARVIS"
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = (0.34f * pulse).coerceAtMost(0.6f)),
                        ringColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            rotate(degrees = rotation, pivot = center) {
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 105f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = StarkGold.copy(alpha = 0.85f),
                    startAngle = 150f,
                    sweepAngle = 68f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor.copy(alpha = 0.65f),
                    startAngle = 250f,
                    sweepAngle = 82f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            rotate(degrees = counterRotation, pivot = center) {
                drawArc(
                    color = ArcCyanBright.copy(alpha = 0.45f),
                    startAngle = 35f,
                    sweepAngle = 55f,
                    useCenter = false,
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = ArcCyanBright.copy(alpha = 0.45f),
                    startAngle = 215f,
                    sweepAngle = 55f,
                    useCenter = false,
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Image(
            painter = painterResource(id = R.drawable.img_jarvis_core),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .border(1.5.dp, ringColor.copy(alpha = 0.85f), CircleShape)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.80f))
                .border(1.dp, ringColor, CircleShape)
        ) {
            Icon(
                imageVector = when {
                    isListening -> Icons.Default.Stop
                    isSpeaking -> Icons.Default.GraphicEq
                    else -> Icons.Default.Mic
                },
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun JarvisModeSelector(
    activeMode: JarvisMode,
    onModeSelected: (JarvisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val glowPulse = rememberStarkPulse(durationMillis = 2200, minValue = 0.35f, maxValue = 0.8f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        JarvisMode.entries.forEach { mode ->
            val selected = mode == activeMode
            val accent = if (mode == JarvisMode.GOSSIP) StarkGold else ArcCyan
            Box(
                modifier = Modifier
                    .starkPulseGlow(
                        glowColor = accent,
                        cornerRadius = 50.dp,
                        enabled = selected
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) accent.copy(alpha = 0.20f)
                        else SlateSurfaceElevated
                    )
                    .border(
                        width = if (selected) 1.2.dp else 1.dp,
                        color = if (selected) accent else HudOutline,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("mode_chip_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.labelMarathi,
                    style = if (selected) {
                        MaterialTheme.typography.labelLarge.copy(
                            shadow = Shadow(
                                color = accent.copy(alpha = glowPulse),
                                offset = Offset.Zero,
                                blurRadius = 10f
                            )
                        )
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
