package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.ForestMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.SagePrimary

@Composable
fun AnimatedDailyGoalProgressBar(
    currentCount: Int,
    targetGoal: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
    titleText: String = "Today's Study Target",
    showCelebrationOnComplete: Boolean = true
) {
    AnimatedDailyGoalProgressBar(
        currentFloat = currentCount.toFloat(),
        targetFloat = targetGoal.toFloat(),
        unitLabel = "cards",
        modifier = modifier,
        barHeight = barHeight,
        titleText = titleText,
        showCelebrationOnComplete = showCelebrationOnComplete,
        isIntegerMode = true
    )
}

@Composable
fun AnimatedDailyGoalProgressBar(
    currentFloat: Float,
    targetFloat: Float,
    unitLabel: String,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
    titleText: String = "Study Goal Progress",
    showCelebrationOnComplete: Boolean = true,
    isIntegerMode: Boolean = false
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val rawRatio = if (targetFloat > 0f) (currentFloat / targetFloat) else 0f
    val clampedRatio = rawRatio.coerceIn(0f, 1f)
    val isGoalCompleted = currentFloat >= targetFloat && targetFloat > 0f

    // Smooth Spring-driven Animated Fill Fraction
    val animatedProgressRatio by animateFloatAsState(
        targetValue = clampedRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress_ratio_fill_anim"
    )

    // Animated Percentage Display Counter
    val animatedPercentage by animateIntAsState(
        targetValue = (clampedRatio * 100).toInt(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress_percentage_anim"
    )

    // Animated Float Display
    val animatedFloatValue by animateFloatAsState(
        targetValue = currentFloat,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress_float_anim"
    )

    // Trigger haptic feedback when goal is reached
    var hasFiredGoalHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(isGoalCompleted) {
        if (isGoalCompleted && !hasFiredGoalHaptic) {
            hasFiredGoalHaptic = true
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (_: Throwable) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (!isGoalCompleted) {
            hasFiredGoalHaptic = false
        }
    }

    // Celebration Bounce Scale
    val badgeScale by animateFloatAsState(
        targetValue = if (isGoalCompleted) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badge_scale_anim"
    )

    // Continuous Shimmer Sweep Animation for filling visual satisfaction
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val activeGradientColors = if (isGoalCompleted) {
        listOf(
            Color(0xFF059669), // Emerald
            Color(0xFF10B981), // Bright Emerald
            Color(0xFF34D399)  // Mint Highlight
        )
    } else {
        listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFFF59E0B)  // Amber Golden Accent
        )
    }

    val displayCurrentText = if (isIntegerMode) {
        "${animatedFloatValue.toInt()} / ${targetFloat.toInt()} $unitLabel"
    } else {
        String.format(java.util.Locale.US, "%.1f / %.1f %s", animatedFloatValue, targetFloat, unitLabel)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animated_daily_goal_progress_bar")
    ) {
        // Header Info Row: Title, Cards/Hours count, Percentage Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGoalCompleted) Icons.Default.CheckCircle else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (isGoalCompleted) EmeraldMastery else OchreStreak,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = titleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayCurrentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .scale(badgeScale)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isGoalCompleted) EmeraldMastery.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$animatedPercentage%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isGoalCompleted) EmeraldMastery else MaterialTheme.colorScheme.primary
                        )
                        if (isGoalCompleted && showCelebrationOnComplete) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("🎉", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Canvas Filled Progress Bar with Smooth Shimmer & Leading Edge Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .border(
                    width = 1.dp,
                    color = if (isGoalCompleted) EmeraldMastery.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(barHeight / 2)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val fillWidth = canvasWidth * animatedProgressRatio

                if (fillWidth > 0f) {
                    val cornerRadius = CornerRadius(canvasHeight / 2, canvasHeight / 2)

                    // Base Fill Gradient
                    val fillBrush = Brush.horizontalGradient(
                        colors = activeGradientColors,
                        startX = 0f,
                        endX = fillWidth.coerceAtLeast(1f)
                    )

                    drawRoundRect(
                        brush = fillBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(fillWidth, canvasHeight),
                        cornerRadius = cornerRadius
                    )

                    // Moving Shimmer Overlay Highlight
                    val shimmerWidth = 150f
                    val shimmerX = shimmerOffset.rem(fillWidth + shimmerWidth) - shimmerWidth
                    if (shimmerX < fillWidth) {
                        val shimmerBrush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            startX = shimmerX,
                            endX = shimmerX + shimmerWidth
                        )

                        drawRoundRect(
                            brush = shimmerBrush,
                            topLeft = Offset(0f, 0f),
                            size = Size(fillWidth, canvasHeight),
                            cornerRadius = cornerRadius
                        )
                    }

                    // Leading Edge Pulse Sparkle Dot at the tip
                    if (animatedProgressRatio in 0.02f..0.98f) {
                        val tipX = fillWidth
                        val centerY = canvasHeight / 2

                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = canvasHeight * 0.35f,
                            center = Offset(tipX - canvasHeight * 0.3f, centerY)
                        )
                    }
                }
            }
        }

        // Optional Satisfying Completion Message Banner
        AnimatedVisibility(
            visible = isGoalCompleted && showCelebrationOnComplete,
            enter = fadeIn() + scaleIn()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = EmeraldMastery.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Goal Met",
                        tint = EmeraldMastery,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Daily Goal Completed! Excellent momentum 🔥",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldMastery
                    )
                }
            }
        }
    }
}
