package com.tushar.sonora.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = darkColorScheme(
    primary = Color(0xFFB9F66A),
    onPrimary = Color(0xFF17200C),
    secondary = Color(0xFFB6C5A6),
    background = Color(0xFF090B0E),
    surface = Color(0xFF11151A),
    surfaceVariant = Color(0xFF1A2027),
    onBackground = Color(0xFFF2F4EE),
    onSurface = Color(0xFFF2F4EE),
    onSurfaceVariant = Color(0xFFA7B0B7),
    outline = Color(0xFF333B42),
)

private val Day = lightColorScheme(
    primary = Color(0xFF526D23),
    onPrimary = Color.White,
    secondary = Color(0xFF536146),
    background = Color(0xFFF5F6F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9ECE5),
    onBackground = Color(0xFF171B15),
    onSurface = Color(0xFF171B15),
    onSurfaceVariant = Color(0xFF5D655A),
    outline = Color(0xFFD2D8CC),
)

@Composable
fun SonoraTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) Night else Day, content = content)
}
