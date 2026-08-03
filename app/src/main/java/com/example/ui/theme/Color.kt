package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Natural Tones Palette (light)
val SagePrimary = Color(0xFF385A43)
val SagePrimaryLight = Color(0xFF8BAE93)
val SagePrimaryContainer = Color(0xFFE2EBE2)

val TerracottaSecondary = Color(0xFFC06346)
val TerracottaSecondaryLight = Color(0xFFE08E73)
val TerracottaSecondaryContainer = Color(0xFFF9EAE1)

val OchreStreak = Color(0xFFC67D0A)
val OchreStreakContainer = Color(0xFFFDEFD9)

val ForestMastery = Color(0xFF2D6A4F)
val ForestMasteryContainer = Color(0xFFD8F3DC)

val EarthAlert = Color(0xFFB93838)
val WarmSandBackground = Color(0xFFFAF8F5)
val WarmSandSurface = Color(0xFFFFFFFF)
val WarmSandSurfaceVariant = Color(0xFFEFECE6)
val WarmSandOutline = Color(0xFFDCD7CE)

val NaturalDarkBackground = Color(0xFF1A1A18)
val NaturalDarkSurface = Color(0xFF262623)
val NaturalDarkSurfaceVariant = Color(0xFF353430)

// The same hues tuned for dark surfaces so accent text/icons keep contrast.
val SagePrimaryDark = Color(0xFF9DC0A5)
val TerracottaSecondaryDark = Color(0xFFF4B7A0)
val OchreStreakDark = Color(0xFFE8A33D)
val ForestDark = Color(0xFF6FCEA7)
val EmeraldDark = Color(0xFF6FCEA7)
val RoseDark = Color(0xFFF0717A)
val IndigoPrimaryDark = Color(0xFF9DC0A5)
val VioletSecondaryDark = Color(0xFFF4B7A0)
val AmberStreakDark = Color(0xFFE8A33D)

// Container roles for dark surfaces (mirror Theme.kt's dark scheme).
val SagePrimaryContainerDark = Color(0xFF233B2B)
val TerracottaSecondaryContainerDark = Color(0xFF422116)
val OchreStreakContainerDark = Color(0xFF3E2A10)
val ForestMasteryContainerDark = Color(0xFF1E3D30)

// Standard theme aliases for component compatibility
val IndigoPrimary = SagePrimary
val IndigoPrimaryLight = SagePrimaryLight
val VioletSecondary = TerracottaSecondary
val VioletSecondaryLight = TerracottaSecondaryLight

val AmberStreak = OchreStreak
val AmberStreakContainer = OchreStreakContainer

val EmeraldMastery = ForestMastery
val EmeraldMasteryContainer = ForestMasteryContainer

val RoseAlert = EarthAlert
val CyanAI = SagePrimaryLight

val DarkBackground = NaturalDarkBackground
val DarkSurface = NaturalDarkSurface
val DarkSurfaceVariant = NaturalDarkSurfaceVariant

val LightBackground = WarmSandBackground
val LightSurface = WarmSandSurface
val LightSurfaceVariant = WarmSandSurfaceVariant

/** True when the active color scheme uses light text on dark surfaces. */
@Composable
fun isDarkColorScheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Picks a color that keeps contrast in the current scheme. */
@Composable
fun adaptive(light: Color, dark: Color): Color =
    if (isDarkColorScheme()) dark else light

/** Theme-aware accents so single code paths work in both modes. */
@Composable
fun adaptivePrimaryAccent(): Color = adaptive(SagePrimary, IndigoPrimaryDark)
@Composable
fun adaptiveSecondaryAccent(): Color = adaptive(TerracottaSecondary, VioletSecondaryDark)
@Composable
fun adaptiveStreakAccent(): Color = adaptive(OchreStreak, OchreStreakDark)
@Composable
fun adaptiveMasteryAccent(): Color = adaptive(ForestMastery, ForestDark)
@Composable
fun adaptiveAlertAccent(): Color = adaptive(EarthAlert, RoseDark)

/** Theme-aware contained backgrounds (bright pastel in light, deep tint in dark). */
@Composable
fun adaptiveContainer(light: Color, dark: Color): Color =
    if (isDarkColorScheme()) dark else light


