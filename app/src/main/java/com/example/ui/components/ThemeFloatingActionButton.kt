package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.SagePrimary

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThemeFloatingActionButton(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Smooth continuous rotation angle (0° for Light, 360° for Dark)
    val rotationAngle by animateFloatAsState(
        targetValue = if (isDarkMode) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_rotation_anim"
    )

    // Animated container background color
    val containerColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFEF3C7),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "fab_container_color_anim"
    )

    // Animated icon tint color
    val iconColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFFD97706),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "fab_icon_color_anim"
    )

    // Animated border color for depth glow
    val borderColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "fab_border_color_anim"
    )

    FloatingActionButton(
        onClick = {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Throwable) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleTheme()
        },
        modifier = modifier
            .testTag("theme_fab")
            .border(width = 2.dp, color = borderColor, shape = CircleShape),
        shape = CircleShape,
        containerColor = containerColor,
        contentColor = iconColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isDarkMode,
                transitionSpec = {
                    (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                        .togetherWith(scaleOut() + fadeOut())
                },
                label = "fab_sun_moon_transition"
            ) { dark ->
                if (dark) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Switch to Light Mode (Late-Night Mode Active)",
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Switch to Dark Mode (Natural Tones Mode Active)",
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
