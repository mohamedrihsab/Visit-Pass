package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SaitDarkPrimary,
    onPrimary = SaitDarkOnPrimary,
    primaryContainer = SaitDarkPrimaryContainer,
    onPrimaryContainer = SaitDarkOnPrimaryContainer,
    secondary = SaitDarkSecondary,
    onSecondary = SaitDarkOnSecondary,
    secondaryContainer = SaitDarkSecondaryContainer,
    onSecondaryContainer = SaitDarkOnSecondaryContainer,
    tertiary = SaitDarkTertiary,
    onTertiary = SaitDarkOnTertiary,
    tertiaryContainer = SaitDarkTertiaryContainer,
    onTertiaryContainer = SaitDarkOnTertiaryContainer,
    background = SaitDarkBackground,
    onBackground = SaitDarkOnBackground,
    surface = SaitDarkSurface,
    onSurface = SaitDarkOnSurface,
    surfaceVariant = SaitDarkSurfaceVariant,
    onSurfaceVariant = SaitDarkOnSurfaceVariant,
    outline = SaitDarkOutline,
    outlineVariant = SaitDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SaitPrimary,
    onPrimary = SaitOnPrimary,
    primaryContainer = SaitPrimaryContainer,
    onPrimaryContainer = SaitOnPrimaryContainer,
    secondary = SaitSecondary,
    onSecondary = SaitOnSecondary,
    secondaryContainer = SaitSecondaryContainer,
    onSecondaryContainer = SaitOnSecondaryContainer,
    tertiary = SaitTertiary,
    onTertiary = SaitOnTertiary,
    tertiaryContainer = SaitTertiaryContainer,
    onTertiaryContainer = SaitOnTertiaryContainer,
    background = SaitBackground,
    onBackground = SaitOnBackground,
    surface = SaitSurface,
    onSurface = SaitOnSurface,
    surfaceVariant = SaitSurfaceVariant,
    onSurfaceVariant = SaitOnSurfaceVariant,
    outline = SaitOutline,
    outlineVariant = SaitOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // When in light theme, status bar & notification icons must be dark so they are clearly visible
                // When in dark theme, status bar & notification icons must be light
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
