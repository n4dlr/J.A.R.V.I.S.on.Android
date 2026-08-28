package com.example.jarvis.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisElectricBlue

@Composable
fun JarvisArcReactorOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_immersive")

    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 1800 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 2200 else 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 450 else 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val primaryGlow = when {
        isProcessing -> JarvisAmber
        isListening -> JarvisCyan
        isSpeaking -> JarvisElectricBlue
        else -> JarvisCyanPrimary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val maxRadius = size.toPx() / 2f - 6f

            // Dynamic amplitude radius boost
            val audioBoost = (audioLevel * 16f).coerceAtMost(22f)
            val dynamicRadius = (maxRadius * pulseScale) + audioBoost

            // 1. Ambient Background Hologram Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = glowAlpha * 0.6f),
                        primaryGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // 2. Outermost Concentric Halo Ring
            drawCircle(
                color = primaryGlow.copy(alpha = 0.20f),
                radius = maxRadius * 0.96f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 3. Middle Dashed Hologram Ring
            drawCircle(
                color = primaryGlow.copy(alpha = 0.35f),
                radius = maxRadius * 0.82f,
                center = center,
                style = Stroke(
                    width = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), rotation1 * 0.5f)
                )
            )

            // 4. Segmented Arc Spinner Ring
            val ringRadius = maxRadius * 0.68f
            drawArc(
                color = primaryGlow.copy(alpha = 0.85f),
                startAngle = rotation1,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 2.8f, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryGlow.copy(alpha = 0.85f),
                startAngle = rotation1 + 180f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 2.8f, cap = StrokeCap.Round)
            )

            // 5. Inner Counter-Rotating Orbitals
            val innerRingRadius = maxRadius * 0.52f
            drawArc(
                color = primaryGlow.copy(alpha = 0.7f),
                startAngle = rotation2,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2),
                style = Stroke(width = 2.0f, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryGlow.copy(alpha = 0.7f),
                startAngle = rotation2 + 180f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2),
                style = Stroke(width = 2.0f, cap = StrokeCap.Round)
            )

            // 6. Glowing Tiered Core (Arc Reactor Lens)
            val coreRadius = maxRadius * 0.36f + (audioLevel * 5f)
            drawCircle(
                color = primaryGlow.copy(alpha = 0.25f),
                radius = coreRadius * 1.35f,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryGlow,
                        JarvisCyanDark
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Core Geometric crosshair
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = coreRadius * 0.45f,
                center = center,
                style = Stroke(width = 1.5f)
            )
        }
    }
}

