package io.github.nknote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A quiet, slightly-soft type scale. Not the stock Roboto defaults.
private val base = FontFamily.SansSerif

private val display = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp, letterSpacing = (-0.5).sp)
private val headline = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp, letterSpacing = (-0.25).sp)

val NkTypography = Typography(
    displaySmall = display.copy(fontSize = 30.sp),
    headlineLarge = headline.copy(fontSize = 28.sp),
    headlineMedium = headline.copy(fontSize = 24.sp),
    headlineSmall = headline.copy(fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = base, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)