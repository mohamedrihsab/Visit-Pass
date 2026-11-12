package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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

private val SaitRedBlueLightColorScheme = lightColorScheme(
    primary = SaitLogoBlue,
    onPrimary = Color.White,
    primaryContainer = SaitLogoBlueContainer,
    onPrimaryContainer = SaitLogoOnBlueContainer,
    secondary = SaitLogoRed,
    onSecondary = Color.White,
    secondaryContainer = SaitLogoRedContainer,
    onSecondaryContainer = SaitLogoOnRedContainer,
    tertiary = Color(0xFFB91C1C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF881337),
    background = Color(0xFFF1F5FB),
    onBackground = Color(0xFF00113D),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF00113D),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF1E293B),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF94A3B8)
)

private val SaitRedBlueDarkColorScheme = darkColorScheme(
    primary = SaitDarkLogoBlue,
    onPrimary = Color.White,
    primaryContainer = SaitDarkLogoBlueContainer,
    onPrimaryContainer = SaitDarkLogoOnBlueContainer,
    secondary = SaitDarkLogoRed,
    onSecondary = Color(0xFF450A0A),
    secondaryContainer = SaitDarkLogoRedContainer,
    onSecondaryContainer = SaitDarkLogoOnRedContainer,
    tertiary = Color(0xFFEF4444),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF7F1D1D),
    onTertiaryContainer = Color(0xFFFECDD3),
    background = Color(0xFF03091B),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0A142D),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF14244D),
    onSurfaceVariant = Color(0xFFDBEAFE),
    outline = Color(0xFF93C5FD),
    outlineVariant = Color(0xFF1E3A8A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    paletteStyle: String = com.example.data.KioskPreferences.PALETTE_OCEANIC,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSaitLogoTheme = paletteStyle == com.example.data.KioskPreferences.PALETTE_SAIT_BLUE_RED

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isSaitLogoTheme && darkTheme -> SaitRedBlueDarkColorScheme
        isSaitLogoTheme -> SaitRedBlueLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val liquidPalette = when {
        isSaitLogoTheme && darkTheme -> SaitBlueRedDarkLiquidGlassPalette
        isSaitLogoTheme -> SaitBlueRedLightLiquidGlassPalette
        darkTheme -> DarkLiquidGlassPalette
        else -> LightLiquidGlassPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalLiquidGlassPalette provides liquidPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
