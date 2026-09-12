package com.evyr.rads.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val RadsColorScheme = darkColorScheme(
    primary = AmberBright,
    onPrimary = TerminalBlack,
    secondary = AmberDim,
    onSecondary = TerminalBlack,
    background = TerminalBlack,
    onBackground = AmberBright,
    surface = TerminalBlackElevated,
    onSurface = AmberBright,
    error = AmberWarn,
    onError = TerminalBlack,
    outline = AmberFaint,
)

// Monospace everywhere — this is a terminal, not an app.
val TerminalTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
)

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
