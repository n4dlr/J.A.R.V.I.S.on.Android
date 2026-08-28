package com.example.jarvis.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.core.MemoryTelemetry
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ProviderHealth
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceGlass
import com.example.ui.theme.JarvisSurfaceGlassBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun TelemetryHudCard(
    telemetry: MemoryTelemetry?,
    providerType: AIProviderType,
    providerHealth: ProviderHealth?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_hud")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisSurfaceGlass)
            .border(1.dp, JarvisSurfaceGlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                (if (providerHealth?.isAvailable == true) JarvisGreen else JarvisAmber)
                                    .copy(alpha = dotAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (providerType) {
                            AIProviderType.LOCAL_SLM -> "OFFLINE SLM"
                            AIProviderType.GEMINI_CLOUD -> "GEMINI CLOUD"
                            AIProviderType.FALLBACK_HYBRID -> "HYBRID AUTO"
                        },
                        color = if (providerHealth?.isAvailable == true) JarvisGreen else JarvisAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                val ramUsedGb = String.format("%.1f", (telemetry?.usedRamMb ?: 1200) / 1024f)
                val ramTotalGb = String.format("%.1f", (telemetry?.totalRamMb ?: 4096) / 1024f)
                Text(
                    text = "RAM: ${ramUsedGb}GB / ${ramTotalGb}GB",
                    color = JarvisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // RAM Info
                val ramPct = telemetry?.ramUsagePercent ?: 30
                HudStatItem(
                    icon = Icons.Default.Memory,
                    label = "RAM TƏZYİQİ",
                    value = "$ramPct%",
                    subValue = if (telemetry?.isLowRamDevice == true) "4GB Low-RAM" else "Optimal",
                    modifier = Modifier.weight(1f)
                )

                // Storage Info
                val storageFree = telemetry?.availableStorageMb?.let { it / 1024 } ?: 0
                HudStatItem(
                    icon = Icons.Default.SdStorage,
                    label = "YADDAŞ",
                    value = "${storageFree} GB",
                    subValue = "Boş Sahə",
                    modifier = Modifier.weight(1f)
                )

                // Provider Status Info
                HudStatItem(
                    icon = Icons.Default.SmartToy,
                    label = "GECİKMƏ",
                    value = providerHealth?.latencyMs?.let { "${it}ms" } ?: "0ms",
                    subValue = "Yerli NLU",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HudStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisCyan.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                color = JarvisTextMuted,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = JarvisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subValue,
                color = JarvisTextSecondary,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

