package com.numberone.numberone.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// DARK: obsidian surfaces, champagne accents
private val NumberOneDarkColorScheme = darkColorScheme(
    primary = NumberOneColors.Champagne,
    onPrimary = NumberOneColors.Obsidian,
    primaryContainer = Color(0xFF2B2310),
    onPrimaryContainer = NumberOneColors.GoldLight,
    secondary = NumberOneColors.GoldMid,
    onSecondary = NumberOneColors.Obsidian,
    secondaryContainer = NumberOneColors.WarmBlack,
    onSecondaryContainer = NumberOneColors.GoldLight,
    tertiary = NumberOneColors.Emerald,
    onTertiary = NumberOneColors.Obsidian,
    background = NumberOneColors.DarkBackground,
    onBackground = NumberOneColors.DarkText,
    surface = NumberOneColors.DarkSurface,
    onSurface = NumberOneColors.DarkText,
    surfaceVariant = NumberOneColors.DarkSurfaceHigh,
    onSurfaceVariant = NumberOneColors.DarkMutedText,
    surfaceContainerLowest = NumberOneColors.DarkBackground,
    surfaceContainerLow = NumberOneColors.DarkSurfaceLow,
    surfaceContainer = NumberOneColors.DarkSurface,
    surfaceContainerHigh = NumberOneColors.DarkSurfaceHigh,
    surfaceContainerHighest = NumberOneColors.DarkSurfaceHighest,
    outline = Color(0xFF4A4740),
    outlineVariant = NumberOneColors.DarkBorder,
    error = NumberOneColors.DarkError,
    onError = NumberOneColors.Obsidian
)

// LIGHT: warm ivory surfaces, bronze accents
private val NumberOneLightColorScheme = lightColorScheme(
    primary = NumberOneColors.LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E6C4),
    onPrimaryContainer = Color(0xFF3D2E0C),
    secondary = Color(0xFF6B5420),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5ECD6),
    onSecondaryContainer = Color(0xFF2E2409),
    tertiary = NumberOneColors.LightSuccess,
    onTertiary = Color.White,
    background = NumberOneColors.LightBackground,
    onBackground = NumberOneColors.LightInk,
    surface = NumberOneColors.LightSurface,
    onSurface = NumberOneColors.LightInk,
    surfaceVariant = NumberOneColors.LightSurfaceHigh,
    onSurfaceVariant = NumberOneColors.LightMutedText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = NumberOneColors.LightSurfaceLow,
    surfaceContainer = NumberOneColors.LightSurface,
    surfaceContainerHigh = NumberOneColors.LightSurfaceHigh,
    surfaceContainerHighest = NumberOneColors.LightSurfaceHighest,
    outline = Color(0xFFB9AE95),
    outlineVariant = NumberOneColors.LightBorder,
    error = NumberOneColors.LightError,
    onError = Color.White
)

// Generous, soft radii for a premium feel
private val NumberOneShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NumberOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NumberOneDarkColorScheme else NumberOneLightColorScheme
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getNumberOneTypography(isArabic),
            shapes = NumberOneShapes,
            content = content
        )
    }
}
