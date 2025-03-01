package com.example.homework_kotlin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val hue = 220F
val sat = 0F
private val DarkColorScheme = darkColorScheme(
    primary = Color.hsl(hue,.5F*sat,.4F),
    secondary = Color.hsl(hue,.2F*sat,.4F, .5F),
    onPrimary = Color.hsl(hue,.5F*sat,.95F),
    background = Color.hsl(hue,.0F,.1F),
    surface = Color.hsl(hue,.0F,.1F),
)

private val LightColorScheme = lightColorScheme(
    primary = Color.hsl(hue,.5F*sat,.4F),
    secondary = Color.hsl(hue,.2F*sat,.4F, .5F),
    onPrimary = Color.hsl(hue,.5F*sat,.95F),
    background = Color.hsl(hue,.0F,.9F),
    surface = Color.hsl(hue,.0F,.9F),
)

@Composable
fun DefaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}