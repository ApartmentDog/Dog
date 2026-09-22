package com.evyr.rads.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Only one theme -- the source PWA (gut-check-v3-1.html) never actually
// defined dark-mode colors (its theme toggle button was never wired to
// real values), so there's no "exact dark version" to match. Rather than
// invent one, RADS renders this single light palette regardless of system
// theme, matching the HTML precisely instead of drifting from it.
private val LightScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = CreamBg,
    primaryContainer = Terracotta,
    onPrimaryContainer = CreamBg,
    secondary = SageAccent,
    onSecondary = CreamBg,
    secondaryContainer = SageAccentSoft,
    onSecondaryContainer = ClayText,
    background = CreamBg,
    onBackground = ClayText,
    surface = CreamCard,
    onSurface = ClayText,
    surfaceVariant = CreamCard2,
    onSurfaceVariant = ClayTextSoft,
    error = WarnRust,
    onError = CreamBg,
    outline = CreamCardBorder,
)

@Composable
fun RadsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content
    )
}

