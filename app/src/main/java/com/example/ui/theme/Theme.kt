package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LateNightDarkColorScheme = darkColorScheme(
    primary = SagePrimaryLight,
    onPrimary = Color(0xFF0D1C10),
    primaryContainer = Color(0xFF233B2B),
    onPrimaryContainer = Color(0xFFD4E8DA),
    secondary = TerracottaSecondaryLight,
    onSecondary = Color(0xFF2D1208),
    secondaryContainer = Color(0xFF422116),
    onSecondaryContainer = Color(0xFFFDECE5),
    tertiary = OchreStreak,
    background = Color(0xFF121316),
    surface = Color(0xFF1D2026),
    surfaceVariant = Color(0xFF282B34),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5263)
)

private val NaturalTonesLightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = Color(0xFF18281B),
    secondary = TerracottaSecondary,
    onSecondary = Color.White,
    secondaryContainer = TerracottaSecondaryContainer,
    onSecondaryContainer = Color(0xFF4A1F13),
    tertiary = OchreStreak,
    background = WarmSandBackground,
    surface = WarmSandSurface,
    surfaceVariant = WarmSandSurfaceVariant,
    onBackground = Color(0xFF2C2A29),
    onSurface = Color(0xFF2C2A29),
    onSurfaceVariant = Color(0xFF5A5754),
    outline = WarmSandOutline
)

@Composable
fun ReviseIQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure branded theme consistency
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LateNightDarkColorScheme
        else -> NaturalTonesLightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

