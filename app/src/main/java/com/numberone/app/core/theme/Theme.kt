package com.numberone.app.core.theme

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

private val ColorWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = MidnightNavy,
    primaryContainer = Color(0xFF4A371B),
    onPrimaryContainer = Color(0xFFFFE8C1),
    secondary = TealAccent,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFF123D38),
    onSecondaryContainer = Color(0xFFC6F7E9),
    tertiary = ElectricBlue,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = ColorWhite,
    surface = MidnightSurface,
    onSurface = ColorWhite,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = Color(0xFFB6C2D4),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF2A3A50),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3A0808)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B5E1C),
    onPrimary = ColorWhite,
    primaryContainer = Color(0xFFFFE8C1),
    onPrimaryContainer = Color(0xFF2D1A03),
    secondary = Color(0xFF087E69),
    onSecondary = ColorWhite,
    secondaryContainer = Color(0xFFD0F3E9),
    onSecondaryContainer = Color(0xFF073A32),
    tertiary = Color(0xFF315FA1),
    onTertiary = ColorWhite,
    background = SoftCloud,
    onBackground = InkNavy,
    surface = ColorWhite,
    onSurface = InkNavy,
    surfaceVariant = Color(0xFFEBF0F6),
    onSurfaceVariant = Color(0xFF596A80),
    outline = Color(0xFF9AA8BC),
    outlineVariant = LightOutline,
    error = Color(0xFFB3261E),
    onError = ColorWhite
)

@Composable
fun NumberOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
