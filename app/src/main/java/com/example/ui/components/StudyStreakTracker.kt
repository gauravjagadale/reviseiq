package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DailyStreakEntity
import com.example.ui.theme.EarthAlert
import com.example.ui.theme.ForestMastery
import com.example.ui.theme.ForestMasteryContainer
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.OchreStreakContainer
import com.example.ui.theme.SagePrimary
import com.example.ui.theme.SagePrimaryContainer
import com.example.ui.theme.TerracottaSecondary
import com.example.ui.theme.TerracottaSecondaryContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class DayStreakStatus {
    GOAL_MET,      // Reached daily card goal
    STUDIED,       // Had study activity (at least 1 card/quiz)
    PENDING_TODAY, // Today, no activity yet
    MISSED_RESET,  // Past day missed (caused streak reset)
    FUTURE         // Upcoming days
}

data class CalendarDayStreak(
    val dateString: String,
    val dayLabel: String,
    val dateNumber: String,
    val cardsReviewed: Int,
    val quizzesCompleted: Int,
    val durationMinutes: Int,
    val goalTargetCards: Int,
    val status: DayStreakStatus,
    val isToday: Boolean
)

private data class ConfettiParticle(
    val xRatio: Float,
    var yRatio: Float,
    val size: Float,
    val color: Color,
    val speedY: Float,
    val speedX: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val isStar: Boolean = false
)

@Composable
fun CelebrationConfettiCanvas(
    modifier: Modifier = Modifier,
    isCelebrating: Boolean = true
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFB703), // Ochre gold
            Color(0xFFFB8500), // Fire orange
            Color(0xFF2A9D8F), // Forest sage
            Color(0xFFE76F51), // Terracotta
            Color(0xFFFFD166), // Bright yellow
            Color(0xFF06D6A0), // Emerald
            Color(0xFF118AB2)  // Sapphire
        )
        List(60) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat() * -0.5f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors[Random.nextInt(colors.size)],
                speedY = Random.nextFloat() * 0.008f + 0.004f,
                speedX = (Random.nextFloat() - 0.5f) * 0.004f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f,
                isStar = Random.nextBoolean()
            )
        }
    }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(isCelebrating) {
        if (isCelebrating) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
            )
        }
    }

    if (animProgress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val progress = animProgress.value

            particles.forEach { particle ->
                val currentY = (particle.yRatio + progress * particle.speedY * 120f) % 1.2f
                val currentX = (particle.xRatio + progress * particle.speedX * 60f).coerceIn(0f, 1f)
                val currentRot = particle.rotation + progress * particle.rotationSpeed * 100f

                val px = currentX * canvasWidth
                val py = currentY * canvasHeight
                val pSize = particle.size * (1f - progress * 0.3f)

                if (currentY in 0f..1f) {
                    rotate(degrees = currentRot, pivot = Offset(px, py)) {
                        if (particle.isStar) {
                            val path = Path().apply {
                                moveTo(px, py - pSize)
                                lineTo(px + pSize * 0.3f, py - pSize * 0.3f)
                                lineTo(px + pSize, py)
                                lineTo(px + pSize * 0.3f, py + pSize * 0.3f)
                                lineTo(px, py + pSize)
                                lineTo(px - pSize * 0.3f, py + pSize * 0.3f)
                                lineTo(px - pSize, py)
                                lineTo(px - pSize * 0.3f, py - pSize * 0.3f)
                                close()
                            }
                            drawPath(path, color = particle.color.copy(alpha = (1f - progress * 0.4f).coerceIn(0f, 1f)))
                        } else {
                            drawRect(
                                color = particle.color.copy(alpha = (1f - progress * 0.4f).coerceIn(0f, 1f)),
                                topLeft = Offset(px - pSize / 2, py - pSize / 2),
                                size = Size(pSize, pSize * 1.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudyStreakTracker(
    dailyStreaks: List<DailyStreakEntity>,
    targetDailyGoal: Int = 20,
    streakShieldsCount: Int = 1,
    onLogStudyActivity: (cardsCount: Int) -> Unit = {},
    onOpenShieldModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val dateNumFormat = remember { SimpleDateFormat("d", Locale.US) }

    val todayStr = remember { dateFormat.format(Date()) }

    // Map streak entities by date
    val streakMap = remember(dailyStreaks) {
        dailyStreaks.associateBy { it.dateString }
    }

    // Build last 7 days calendar window
    val last7Days = remember(streakMap, todayStr) {
        val list = mutableListOf<CalendarDayStreak>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(c.time)
            val dayLabel = dayNameFormat.format(c.time)
            val dateNumber = dateNumFormat.format(c.time)
            val isToday = dStr == todayStr

            val entry = streakMap[dStr]
            val reviewed = entry?.cardsReviewed ?: 0
            val quizzes = entry?.quizzesCompleted ?: 0
            val mins = entry?.studyDurationMinutes ?: 0
            val target = entry?.goalTargetCards ?: targetDailyGoal

            val status = when {
                reviewed >= target -> DayStreakStatus.GOAL_MET
                reviewed > 0 || quizzes > 0 -> DayStreakStatus.STUDIED
                isToday -> DayStreakStatus.PENDING_TODAY
                else -> DayStreakStatus.MISSED_RESET
            }

            list.add(
                CalendarDayStreak(
                    dateString = dStr,
                    dayLabel = dayLabel,
                    dateNumber = dateNumber,
                    cardsReviewed = reviewed,
                    quizzesCompleted = quizzes,
                    durationMinutes = mins,
                    goalTargetCards = target,
                    status = status,
                    isToday = isToday
                )
            )
        }
        list
    }

    // Compute active streak & longest streak
    val (activeStreak, longestStreak, todayCompleted) = remember(streakMap, todayStr) {
        var current = 0
        var maxStreak = 0
        var tempStreak = 0

        val cal = Calendar.getInstance()

        // Check today's status
        val todayEntry = streakMap[todayStr]
        val isTodayActive = (todayEntry?.cardsReviewed ?: 0) > 0 || (todayEntry?.quizzesCompleted ?: 0) > 0

        if (isTodayActive) {
            current++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Count back consecutive days
        while (true) {
            val dStr = dateFormat.format(cal.time)
            val entry = streakMap[dStr]
            if (entry != null && (entry.cardsReviewed > 0 || entry.quizzesCompleted > 0)) {
                current++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Calculate historical max streak
        val allCals = Calendar.getInstance()
        for (i in 0..60) {
            val dStr = dateFormat.format(allCals.time)
            val entry = streakMap[dStr]
            if (entry != null && (entry.cardsReviewed > 0 || entry.quizzesCompleted > 0)) {
                tempStreak++
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } else {
                tempStreak = 0
            }
            allCals.add(Calendar.DAY_OF_YEAR, -1)
        }

        Triple(current, maxOf(current, maxStreak, 3), isTodayActive)
    }

    val todayStreak = streakMap[todayStr]
    val todayReviewed = todayStreak?.cardsReviewed ?: 0
    val progressRatio = (todayReviewed.toFloat() / targetDailyGoal.toFloat()).coerceIn(0f, 1f)

    var selectedDayDetails by remember { mutableStateOf<CalendarDayStreak?>(null) }
    var showStreakInfoDialog by remember { mutableStateOf(false) }
    var showMilestoneCelebrationDialog by remember { mutableStateOf(false) }
    var triggersConfettiCelebration by remember { mutableStateOf(false) }

    // Pulsing animation for active streak flame
    val infiniteTransition = rememberInfiniteTransition(label = "streak_flame_pulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    // Check for 7-day milestone reach
    val is7DayMilestoneReached = activeStreak >= 7 || (last7Days.count { it.status == DayStreakStatus.GOAL_MET || it.status == DayStreakStatus.STUDIED } >= 7)

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("study_streak_tracker"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Bar: Flame Banner Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeStreak > 0) OchreStreakContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Flame",
                                tint = if (activeStreak > 0) OchreStreak else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(34.dp)
                                    .scale(if (activeStreak > 0) flameScale else 1.0f)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$activeStreak DAY STREAK",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (activeStreak > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Active Streak",
                                        tint = OchreStreak,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = when {
                                    todayCompleted && todayReviewed >= targetDailyGoal -> "Daily Goal Mastered! 🎉"
                                    todayCompleted -> "Streak Maintained for Today! 🔥"
                                    activeStreak > 0 -> "Keep your streak alive—study today!"
                                    else -> "Start your study streak today!"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Longest Streak & Streak Shield Badge Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(OchreStreakContainer)
                                .clickable { onOpenShieldModal() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("study_tracker_streak_shield_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = OchreStreak,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$streakShieldsCount 🛡️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OchreStreak
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ForestMasteryContainer)
                                .clickable {
                                    triggersConfettiCelebration = true
                                    showMilestoneCelebrationDialog = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Record",
                                    tint = ForestMastery,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Best: $longestStreak",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestMastery
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Milestone Banner Notification if 7-day milestone is achieved or active
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(OchreStreakContainer, ForestMasteryContainer)
                            )
                        )
                        .clickable {
                            triggersConfettiCelebration = true
                            showMilestoneCelebrationDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = "Milestone Banner",
                                tint = OchreStreak,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (is7DayMilestoneReached) "7-Day Streak Milestone Mastered!" else "7-Day Streak Goal in Progress",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to launch celebration fireworks & claim badges 🎉",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Celebrate",
                            tint = ForestMastery,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Calendar Activity Strip (Last 7 Days)
                Text(
                    text = "CALENDAR ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    last7Days.forEach { dayStreak ->
                        val (cellBg, borderColor, iconTint) = when (dayStreak.status) {
                            DayStreakStatus.GOAL_MET -> Triple(ForestMasteryContainer, ForestMastery, ForestMastery)
                            DayStreakStatus.STUDIED -> Triple(OchreStreakContainer, OchreStreak, OchreStreak)
                            DayStreakStatus.PENDING_TODAY -> Triple(SagePrimaryContainer, SagePrimary, SagePrimary)
                            DayStreakStatus.MISSED_RESET -> Triple(TerracottaSecondaryContainer, TerracottaSecondary, EarthAlert)
                            DayStreakStatus.FUTURE -> Triple(MaterialTheme.colorScheme.surfaceVariant, Color.Transparent, Color.Gray)
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clickable { selectedDayDetails = dayStreak }
                        ) {
                            Text(
                                text = dayStreak.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = if (dayStreak.isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (dayStreak.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cellBg)
                                    .then(
                                        if (dayStreak.isToday) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayStreak.dateNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayStreak.status == DayStreakStatus.MISSED_RESET) EarthAlert else MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    when (dayStreak.status) {
                                        DayStreakStatus.GOAL_MET -> Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Goal Met",
                                            tint = ForestMastery,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        DayStreakStatus.STUDIED -> Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Studied",
                                            tint = OchreStreak,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        DayStreakStatus.PENDING_TODAY -> Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(SagePrimary)
                                        )
                                        DayStreakStatus.MISSED_RESET -> Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Missed Day (Reset)",
                                            tint = EarthAlert,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        DayStreakStatus.FUTURE -> {}
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Daily Target Progress Bar & Action Controls
                AnimatedDailyGoalProgressBar(
                    currentCount = todayReviewed,
                    targetGoal = targetDailyGoal,
                    titleText = "Today's Study Goal Target",
                    barHeight = 16.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Log Study & Celebration Trigger Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onLogStudyActivity(5)
                            triggersConfettiCelebration = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Cards",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log 5 Cards (+1 Day)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            triggersConfettiCelebration = true
                            showMilestoneCelebrationDialog = true
                        },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Celebration,
                            contentDescription = "Celebrate Milestone",
                            tint = OchreStreak,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Celebrate 🎉", fontSize = 12.sp, color = OchreStreak)
                    }
                }
            }
        }

        // Overlay Confetti Canvas when triggered
        if (triggersConfettiCelebration) {
            CelebrationConfettiCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp)),
                isCelebrating = true
            )
        }
    }

    // Day Details Dialog when tapping a day cell
    selectedDayDetails?.let { dayDetails ->
        AlertDialog(
            onDismissRequest = { selectedDayDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (dayDetails.status) {
                            DayStreakStatus.GOAL_MET -> Icons.Default.CheckCircle
                            DayStreakStatus.STUDIED -> Icons.Default.LocalFireDepartment
                            DayStreakStatus.MISSED_RESET -> Icons.Default.Close
                            else -> Icons.Default.LocalFireDepartment
                        },
                        contentDescription = null,
                        tint = when (dayDetails.status) {
                            DayStreakStatus.GOAL_MET -> ForestMastery
                            DayStreakStatus.STUDIED -> OchreStreak
                            DayStreakStatus.MISSED_RESET -> EarthAlert
                            else -> SagePrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dayDetails.isToday) "Today's Study Log" else "Activity on ${dayDetails.dateString}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = when (dayDetails.status) {
                            DayStreakStatus.GOAL_MET -> "Daily goal of ${dayDetails.goalTargetCards} cards was completed! Great work."
                            DayStreakStatus.STUDIED -> "Study session recorded. Streak maintained."
                            DayStreakStatus.PENDING_TODAY -> "No study activity recorded yet today. Complete cards to increment your streak!"
                            DayStreakStatus.MISSED_RESET -> "No activity recorded on this day. Consecutive streak was reset."
                            DayStreakStatus.FUTURE -> "Upcoming day."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Cards", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${dayDetails.cardsReviewed}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Quizzes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${dayDetails.quizzesCompleted}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Duration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${dayDetails.durationMinutes}m", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedDayDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Milestone Celebration Modal Dialog with Lottie-style animation
    if (showMilestoneCelebrationDialog) {
        AlertDialog(
            onDismissRequest = {
                showMilestoneCelebrationDialog = false
                triggersConfettiCelebration = false
            },
            title = null,
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Confetti canvas inside dialog
                    CelebrationConfettiCanvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        isCelebrating = true
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(OchreStreakContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = OchreStreak,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "STREAK MILESTONE UNLOCKED! 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You've maintained an active study habit across calendar days! Outstanding consistency.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ForestMasteryContainer)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Active Streak", fontSize = 11.sp, color = ForestMastery)
                                Text("$activeStreak Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ForestMastery)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Best Record", fontSize = 11.sp, color = ForestMastery)
                                Text("$longestStreak Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ForestMastery)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMilestoneCelebrationDialog = false
                        triggersConfettiCelebration = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep Going! 🔥", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Streak Rules & Freeze Info Dialog
    if (showStreakInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStreakInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = OchreStreak,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Streak Rules & Protections", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🔥 Streak Logic Rules:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "• Increment: Complete at least 1 card review or practice quiz daily to keep your flame active.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "• Reset: If a full calendar day passes without any revision activity, the current streak count resets to 0.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "• Goal Mastery: Reaching your target of $targetDailyGoal cards unlocks the green Goal Met badge.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OchreStreakContainer)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Streak Freeze",
                                tint = OchreStreak,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1 Streak Freeze Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OchreStreak
                                )
                                Text(
                                    text = "Automatically protects 1 missed calendar day per month.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStreakInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Got It!")
                }
            }
        )
    }
}
