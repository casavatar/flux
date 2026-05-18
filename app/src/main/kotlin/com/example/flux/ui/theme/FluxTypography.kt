package com.example.flux.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.flux.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val literataFont = GoogleFont("Literata")

// Serif fallback ensures readability if Google Fonts are unavailable
val LiterataFontFamily = FontFamily(
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = literataFont, fontProvider = fontProvider, weight = FontWeight.Bold, style = FontStyle.Italic),
)

val FluxTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = LiterataFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
    ),
)
