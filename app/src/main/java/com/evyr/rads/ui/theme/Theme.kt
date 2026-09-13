package com.evyr.rads.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

private val RadsColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = ScreenBlack,
    secondary = AmberDim,
    onSecondary = ScreenBlack,
    background = ScreenBlack,
    onBackground = Amber,
    surface = ScreenInk,
    onSurface = Amber,
    error = AmberWarn,
    onError = ScreenBlack,
    outline = AmberFaint,
)

// Monospace everywhere — this is a terminal, not an app.
@Composable
fun RadsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RadsColorScheme,
        typography = MaterialTheme.typography.copy(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
        ),
        content = content
    )
}
