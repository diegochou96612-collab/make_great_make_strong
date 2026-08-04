package com.petmed.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = White,
    secondary = Green,
    onSecondary = White,
    tertiary = Blue,
    background = Cream,
    surface = White,
    onBackground = Brown,
    onSurface = Brown,
)

@Composable
fun PetMedAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
