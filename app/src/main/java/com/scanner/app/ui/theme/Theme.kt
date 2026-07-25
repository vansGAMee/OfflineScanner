package com.scanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPurple = Color(0xFF9C27B0)
private val LightPurpleContainer = Color(0xFFE1BEE7)
private val LightOnPurpleContainer = Color(0xFF4A148C)
private val LightSecondary = Color(0xFFFFA726)
private val LightSurface = Color(0xFFFAF5FF)
private val LightOnSurface = Color(0xFF1E1A1F)

private val DarkPurple = Color(0xFFCE93D8)
private val DarkPurpleContainer = Color(0xFF6A1B9A)
private val DarkOnPurpleContainer = Color(0xFFE1BEE7)
private val DarkSecondary = Color(0xFFFFCC02)
private val DarkSurface = Color(0xFF1E1A1F)
private val DarkOnSurface = Color(0xFFE6E0E9)

private val LightColorScheme = lightColorScheme(
    primary = LightPurple,
    primaryContainer = LightPurpleContainer,
    onPrimaryContainer = LightOnPurpleContainer,
    secondary = LightSecondary,
    tertiary = Color(0xFF4CAF50),
    surface = LightSurface,
    onSurface = LightOnSurface,
    background = LightSurface,
    onBackground = LightOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPurple,
    primaryContainer = DarkPurpleContainer,
    onPrimaryContainer = DarkOnPurpleContainer,
    secondary = DarkSecondary,
    tertiary = Color(0xFF81C784),
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    background = DarkSurface,
    onBackground = DarkOnSurface
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
