package com.example.v2rayng.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0B5FF),
    onPrimary = Color(0xFF301451),
    primaryContainer = Color(0xFF4C2876),
    onPrimaryContainer = Color(0xFFEEDFFF),
    secondary = Color(0xFFCBBBDC),
    tertiary = Color(0xFFF2B4D6),
    background = Color(0xFF100C19),
    onBackground = Color(0xFFF6F0FF),
    surface = Color(0xFF1B1526),
    onSurface = Color(0xFFF6F0FF),
    surfaceVariant = Color(0xFF2B2239),
    onSurfaceVariant = Color(0xFFC1B6CF),
    outline = Color(0xFF81718F),
    outlineVariant = Color(0xFF40334F)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7041A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEDCFF),
    onPrimaryContainer = Color(0xFF301451),
    secondary = Color(0xFF685777),
    tertiary = Color(0xFF984875),
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF21172E),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF21172E),
    surfaceVariant = Color(0xFFF0E8F7),
    onSurfaceVariant = Color(0xFF6C6078),
    outline = Color(0xFF897895),
    outlineVariant = Color(0xFFDED2E8)
)

@Composable
fun AetherTheme(
    themeMode: String = "Dark",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
