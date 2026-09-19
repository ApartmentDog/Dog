package com.evyr.rads.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RadsColorScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Color.White,
    primaryContainer = RowHighlight,
    onPrimaryContainer = AmberBright,
    secondary = AmberDim,
    onSecondary = Color.White,
    background = ScreenBlackDeep,
    onBackground = TextPrimary,
    surface = ScreenBlack,
    onSurface = TextPrimary,
    surfaceVariant = ScreenInk,
    onSurfaceVariant = TextSecondary,
    error = AmberWarn,
    onError = Color.White,
    outline = AmberHairline,
    outlineVariant = AmberHairline,
)

@Composable
fun RadsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RadsColorScheme,
        content = content
    )
}
