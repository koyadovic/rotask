package com.rotask.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RotaskColors = darkColorScheme(
    primary = RotaskAccent,
    onPrimary = RotaskBlueDeep,
    primaryContainer = RotaskAccentDim,
    onPrimaryContainer = Color.White,
    background = RotaskBlue,
    onBackground = Color.White,
    surface = RotaskBlueLight,
    onSurface = Color.White,
    surfaceVariant = RotaskBlueLight,
    onSurfaceVariant = Color.White,
    secondary = Color(0xFF3DA5FF),
    onSecondary = RotaskBlueDeep,
)

@Composable
fun RotaskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RotaskColors,
        typography = AppTypography,
        content = content
    )
}
