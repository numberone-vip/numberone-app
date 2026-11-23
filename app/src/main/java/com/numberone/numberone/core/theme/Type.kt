package com.numberone.numberone.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Number One - Typography System
 *
 * To ship the brand fonts, add Cairo and Inter to res/font and replace the
 * defaults below, for example:
 *   val CairoFontFamily = FontFamily(
 *       Font(R.font.cairo_regular, FontWeight.Normal),
 *       Font(R.font.cairo_semibold, FontWeight.SemiBold),
 *       Font(R.font.cairo_bold, FontWeight.Bold),
 *       Font(R.font.cairo_extrabold, FontWeight.ExtraBold)
 *   )
 */
val CairoFontFamily: FontFamily = FontFamily.Default
val InterFontFamily: FontFamily = FontFamily.Default

private fun createNumberOneTypography(fontFamily: FontFamily, headingTracking: TextUnit): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            letterSpacing = headingTracking
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            letterSpacing = headingTracking
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = headingTracking
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            letterSpacing = headingTracking
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    )
}

/** Arabic has no tracking; Latin headings get a slight negative tracking for a tighter, premium look. */
private val CairoTypography = createNumberOneTypography(CairoFontFamily, headingTracking = 0.sp)
private val InterTypography = createNumberOneTypography(InterFontFamily, headingTracking = (-0.3).sp)

/**
 * Select typography according to app language.
 */
fun getNumberOneTypography(isArabic: Boolean): Typography {
    return if (isArabic) CairoTypography else InterTypography
}
