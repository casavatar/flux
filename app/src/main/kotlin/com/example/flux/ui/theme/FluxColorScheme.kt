package com.example.flux.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Light palette (ink on paper) ---
private val primaryLight = Color(0xFF2E4057)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFD0E4FF)
private val onPrimaryContainerLight = Color(0xFF001D36)

private val secondaryLight = Color(0xFF5F6B7A)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFE3EBF3)
private val onSecondaryContainerLight = Color(0xFF1B2631)

private val tertiaryLight = Color(0xFF7B5E4A)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFFDCC8)
private val onTertiaryContainerLight = Color(0xFF2E1507)

private val backgroundLight = Color(0xFFFAF9F5)
private val onBackgroundLight = Color(0xFF1A1C1E)
private val surfaceLight = Color(0xFFFAF9F5)
private val onSurfaceLight = Color(0xFF1A1C1E)
private val surfaceVariantLight = Color(0xFFDFE3EB)
private val onSurfaceVariantLight = Color(0xFF43474E)
private val outlineLight = Color(0xFF73777F)

// --- Dark palette ---
private val primaryDark = Color(0xFF9DBDFF)
private val onPrimaryDark = Color(0xFF003060)
private val primaryContainerDark = Color(0xFF1B4880)
private val onPrimaryContainerDark = Color(0xFFD0E4FF)

private val secondaryDark = Color(0xFFBBC8D8)
private val onSecondaryDark = Color(0xFF283644)
private val secondaryContainerDark = Color(0xFF3E505F)
private val onSecondaryContainerDark = Color(0xFFD7E3F3)

private val tertiaryDark = Color(0xFFEFB99A)
private val onTertiaryDark = Color(0xFF462B16)
private val tertiaryContainerDark = Color(0xFF5E4332)
private val onTertiaryContainerDark = Color(0xFFFFDCC8)

private val backgroundDark = Color(0xFF1A1C1E)
private val onBackgroundDark = Color(0xFFE2E2E6)
private val surfaceDark = Color(0xFF1A1C1E)
private val onSurfaceDark = Color(0xFFE2E2E6)
private val surfaceVariantDark = Color(0xFF43474E)
private val onSurfaceVariantDark = Color(0xFFC3C7CF)
private val outlineDark = Color(0xFF8D9199)

val FluxLightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
)

val FluxDarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
)

// --- Sepia palette (warm paper tone) ---
private val primarySepia = Color(0xFF6B4C3B)
private val onPrimarySepia = Color(0xFFFFFFFF)
private val primaryContainerSepia = Color(0xFFE8C9B0)
private val onPrimaryContainerSepia = Color(0xFF2E1507)

private val secondarySepia = Color(0xFF8B6B55)
private val onSecondarySepia = Color(0xFFFFFFFF)
private val secondaryContainerSepia = Color(0xFFD4B49A)
private val onSecondaryContainerSepia = Color(0xFF2E1507)

private val tertiarySepia = Color(0xFF7B5E4A)
private val onTertiarySepia = Color(0xFFFFFFFF)
private val tertiaryContainerSepia = Color(0xFFE8C9A8)
private val onTertiaryContainerSepia = Color(0xFF2E1507)

private val backgroundSepia = Color(0xFFF4EAD5)
private val onBackgroundSepia = Color(0xFF3D2B1F)
private val surfaceSepia = Color(0xFFF4EAD5)
private val onSurfaceSepia = Color(0xFF3D2B1F)
private val surfaceVariantSepia = Color(0xFFE8D5BC)
private val onSurfaceVariantSepia = Color(0xFF5C4033)
private val outlineSepia = Color(0xFF9C7B65)

val FluxSepiaColorScheme = lightColorScheme(
    primary = primarySepia,
    onPrimary = onPrimarySepia,
    primaryContainer = primaryContainerSepia,
    onPrimaryContainer = onPrimaryContainerSepia,
    secondary = secondarySepia,
    onSecondary = onSecondarySepia,
    secondaryContainer = secondaryContainerSepia,
    onSecondaryContainer = onSecondaryContainerSepia,
    tertiary = tertiarySepia,
    onTertiary = onTertiarySepia,
    tertiaryContainer = tertiaryContainerSepia,
    onTertiaryContainer = onTertiaryContainerSepia,
    background = backgroundSepia,
    onBackground = onBackgroundSepia,
    surface = surfaceSepia,
    onSurface = onSurfaceSepia,
    surfaceVariant = surfaceVariantSepia,
    onSurfaceVariant = onSurfaceVariantSepia,
    outline = outlineSepia,
)
