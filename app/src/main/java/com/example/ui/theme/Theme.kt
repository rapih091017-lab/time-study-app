package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PlayfulBlack,
    onPrimary = Color.White,
    primaryContainer = PlayfulLavenderLight,
    onPrimaryContainer = PlayfulBlack,
    secondary = PlayfulTeal,
    onSecondary = Color.White,
    secondaryContainer = PlayfulTealLight,
    onSecondaryContainer = PlayfulTeal,
    tertiary = PlayfulCoral,
    onTertiary = Color.White,
    background = PlayfulBackground,
    onBackground = PlayfulBlack,
    surface = PlayfulWhite,
    onSurface = PlayfulBlack,
    surfaceVariant = PlayfulCream,
    onSurfaceVariant = PlayfulBlack,
    outline = PlayfulBorder,
    error = PlayfulCoral,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
