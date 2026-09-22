package com.example.faceid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Constantes auxiliares (declaradas antes de los schemes).
private val ColorSchemeErrorTextDark = Color(0xFFFFC9D4)
private val ColorSecondaryContainerLight = Color(0xFFD9F6FC)
private val ColorOnSecondaryContainerLight = Color(0xFF0E7490)
private val ColorSurfaceHighLight = Color(0xFFE7EBF5)
private val ColorOutlineVariantLight = Color(0xFFD3DAEC)
private val ColorOnErrorLight = Color(0xFF881337)

private val DarkColorScheme = darkColorScheme(
    primary = Iris500,
    onPrimary = DarkTextPrimary,
    primaryContainer = Night600,
    onPrimaryContainer = Iris100,
    secondary = Cyan400,
    onSecondary = Night900,
    secondaryContainer = Night600,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Mint400,
    onTertiary = Night900,
    background = Night900,
    onBackground = DarkTextPrimary,
    surface = Night800,
    onSurface = DarkTextPrimary,
    surfaceVariant = Night700,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Night900,
    surfaceContainerLow = Night800,
    surfaceContainer = Night700,
    surfaceContainerHigh = Night600,
    surfaceContainerHighest = NightBorder,
    outline = NightBorder,
    outlineVariant = Night600,
    error = Danger,
    onError = DarkTextPrimary,
    errorContainer = DangerSoftDark,
    onErrorContainer = ColorSchemeErrorTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = Iris600,
    onPrimary = Color.White,
    primaryContainer = Iris100,
    onPrimaryContainer = Iris600,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = ColorSecondaryContainerLight,
    onSecondaryContainer = ColorOnSecondaryContainerLight,
    tertiary = Success,
    onTertiary = Color.White,
    background = MistBackground,
    onBackground = LightTextPrimary,
    surface = MistSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = MistSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = MistSurface,
    surfaceContainer = MistSurfaceVariant,
    surfaceContainerHigh = ColorSurfaceHighLight,
    surfaceContainerHighest = LightBorder,
    outline = LightBorder,
    outlineVariant = ColorOutlineVariantLight,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoftLight,
    onErrorContainer = ColorOnErrorLight
)

@Composable
fun FACEIDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
