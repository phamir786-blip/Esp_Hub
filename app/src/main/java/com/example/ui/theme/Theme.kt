package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledDarkColorScheme = darkColorScheme(
    primary = TuyaOrange,
    onPrimary = Color.White,
    primaryContainer = TuyaOrangeContainerDark,
    onPrimaryContainer = TuyaOrangeLight,
    secondary = ElectricCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = ElectricCyanContainerDark,
    onSecondaryContainer = ElectricCyan,
    tertiary = OnlineEmerald,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceElevated,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledBorder,
    outlineVariant = Color(0xFF151C28)
)

private val CrispLightColorScheme = lightColorScheme(
    primary = TuyaOrange,
    onPrimary = Color.White,
    primaryContainer = TuyaOrangeContainerLight,
    onPrimaryContainer = Color(0xFF9A2B00),
    secondary = ElectricCyanDark,
    onSecondary = Color.White,
    secondaryContainer = ElectricCyanContainerLight,
    onSecondaryContainer = Color(0xFF004F5C),
    tertiary = Color(0xFF00B25B),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmoledDarkColorScheme else CrispLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
