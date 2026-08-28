package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisDarkVoid,
    primaryContainer = JarvisDarkNavy,
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = JarvisSurfaceCard,
    onSecondaryContainer = JarvisBlue,
    tertiary = JarvisAmber,
    onTertiary = JarvisDarkVoid,
    background = JarvisDarkVoid,
    onBackground = JarvisTextPrimary,
    surface = JarvisDarkNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceCard,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisSurfaceCardBorder,
    error = JarvisCrimson,
    onError = Color.White
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
