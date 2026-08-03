package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary

@Composable
fun WeeklyGoalProgressCard(
    viewModel: ReviseViewModel,
    modifier: Modifier = Modifier
) {
    val weeklyGoalHours by viewModel.weeklyStudyGoalHours.collectAsState()
    val currentWeeklyHours by viewModel.weeklyStudyHoursProgress.collectAsState()

    var showGoalModal by remember { mutableStateOf(false) }

    WeeklyGoalContent(
        weeklyGoalHours = weeklyGoalHours,
        currentWeeklyHours = currentWeeklyHours,
        onEditGoal = { showGoalModal = true },
        onQuickLogMinutes = { viewModel.addQuickStudyMinutes(it) },
        modifier = modifier
    )

    if (showGoalModal) {
        WeeklyGoalSettingModal(
            viewModel = viewModel,
            onDismissRequest = { showGoalModal = false }
        )
    }
}

@Composable
fun WeeklyGoalContent(
    weeklyGoalHours: Float,
    currentWeeklyHours: Float,
    onEditGoal: () -> Unit,
    onQuickLogMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val remainingHours = (weeklyGoalHours - currentWeeklyHours).coerceAtLeast(0f)
    val isGoalAchieved = currentWeeklyHours >= weeklyGoalHours && weeklyGoalHours > 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_goal_progress_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = "Weekly Goal Icon",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Weekly Study Target",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isGoalAchieved) "Target Reached! Keep building momentum 🔥"
                                   else String.format(java.util.Locale.US, "%.1f hrs remaining this week", remainingHours),
                            fontSize = 12.sp,
                            color = if (isGoalAchieved) EmeraldMastery else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            } catch (_: Throwable) {}
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditGoal()
                        }
                        .testTag("edit_weekly_goal_button"),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit Goal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedDailyGoalProgressBar(
                currentFloat = currentWeeklyHours,
                targetFloat = weeklyGoalHours,
                unitLabel = "hrs",
                titleText = "Weekly Study Progress",
                barHeight = 16.dp,
                showCelebrationOnComplete = true,
                isIntegerMode = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quick Log Practice:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickLogChip(
                    label = "+15m",
                    icon = "⏱️",
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        onQuickLogMinutes(15)
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickLogChip(
                    label = "+30m",
                    icon = "⏱️",
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        onQuickLogMinutes(30)
                    },
                    modifier = Modifier.weight(1f)
                )

                QuickLogChip(
                    label = "+1h",
                    icon = "🚀",
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        onQuickLogMinutes(60)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickLogChip(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
