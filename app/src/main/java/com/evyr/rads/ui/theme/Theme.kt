package com.evyr.rads.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = CreamBg,
    secondary = SageAccent,
    onSecondary = CreamBg,
    background = CreamBg,
    onBackground = ClayText,
    surface = CreamCard,
    onSurface = ClayText,
    error = WarnRust,
    onError = CreamBg,
    outline = CreamCardBorder,
)

private val DarkScheme = darkColorScheme(
    primary = TerracottaBright,
    onPrimary = EarthBg,
    secondary = SageAccentBright,
    onSecondary = EarthBg,
    background = EarthBg,
    onBackground = SandText,
    surface = EarthCard,
    onSurface = SandText,
    error = WarnRustBright,
    onError = EarthBg,
    outline = EarthCardBorder,
)

// Serif display font for headings (the "Gut Check" wordmark style), sans/
// default for body -- replaces the old fixed monospace-everywhere terminal
// typography.
@Composable
fun RadsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}

