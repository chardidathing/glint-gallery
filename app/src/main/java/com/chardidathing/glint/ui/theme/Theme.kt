package com.chardidathing.glint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.chardidathing.glint.data.preferences.ThemePreferences

private val GlintColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Black,
    primaryContainer = AccentPurpleDark,
    onPrimaryContainer = White,
    secondary = GrayText,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray,
    error = ErrorRed,
    onError = Black,
)

@Composable
fun GlintTheme(content: @Composable () -> Unit) {
    val colorScheme = if (ThemePreferences.useMaterialYou) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        GlintColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
