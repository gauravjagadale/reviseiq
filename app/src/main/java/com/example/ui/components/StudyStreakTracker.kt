package com.example.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DailyStreakEntity
import com.example.ui.DailyTask
import com.example.ui.theme.OchreStreak
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class DayStreakStatus {
    GOAL_MET,
    STUDIED,
    PENDING_TODAY,
    MISSED_RESET,
    FUTURE
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

private data class StreakPalette(
    val ochre: Color,
    val ochreContainer: Color,
    val forest: Color,
    val forestContainer: Color,
    val sage: Color,
    val sageContainer: Color,
    val terracotta: Color,
    val terracottaContainer: Color,
    val earth: Color
)

@Composable
private fun streakPalette(): StreakPalette {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        StreakPalette(
            ochre = Color(0xFFF2B84B),
            ochreContainer = Color(0xFFC67D0A).copy(alpha = 0.22f),
            forest = Color(0xFF74C69D),
            forestContainer = Color(0xFF2D6A4F).copy(alpha = 0.30f),
            sage = Color(0xFF8BAE93),
            sageContainer = Color(0xFF385A43).copy(alpha = 0.30f),
            terracotta = Color(0xFFE08E73),
            terracottaContainer = Color(0xFFC06346).copy(alpha = 0.25f),
            earth = Color(0xFFE57373)
        )
    } else {
        StreakPalette(
            ochre = Color(0xFFC67D0A),
            ochreContainer = Color(0xFFFDEFD9),
            forest = Color(0xFF2D6A4F),
            forestContainer = Color(0xFFD8F3DC),
            sage = Color(0xFF385A43),
            sageContainer = Color(0xFFE2EBE2),
            terracotta = Color(0xFFC06346),
            terracottaContainer = Color(0xFFF9EAE1),
            earth = Color(0xFFB93838)
        )
    }
}

@Composable
fun CelebrationConfettiCanvas(
    modifier: Modifier = Modifier,
    isCelebrating: Boolean = true
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFFFB703),
            Color(0xFFFB8500),
            Color(0xFF2A9D8F),
            Color(0xFFE76F51),
            Color(0xFFFFD166),
            Color(0xFF06D6A0),
            Color(0xFF118AB2)
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
    dailyTasks: List<DailyTask>,
    streakShieldsCount: Int = 1,
    onAddTask: (String) -> Unit = {},
    onRemoveTask: (String) -> Unit = {},
    onToggleTask: (String) -> Unit = {},
    onOpenShieldModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val dateNumFormat = remember { SimpleDateFormat("d", Locale.US) }

    val todayStr = remember { dateFormat.format(Date()) }

    val streakMap = remember(dailyStreaks) {
        dailyStreaks.associateBy { it.dateString }
    }

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
            val target = entry?.goalTargetCards ?: 20

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

    val (activeStreak, longestStreak, todayCompleted) = remember(streakMap, todayStr) {
        var current = 0
        var maxStreak = 0
        var tempStreak = 0

        val cal = Calendar.getInstance()

        val todayEntry = streakMap[todayStr]
        val isTodayActive = (todayEntry?.cardsReviewed ?: 0) > 0 || (todayEntry?.quizzesCompleted ?: 0) > 0

        cal.add(Calendar.DAY_OF_YEAR, -1)
        if (isTodayActive) current++

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

        Triple(current, maxOf(current, maxStreak), isTodayActive)
    }

    var selectedDayDetails by remember { mutableStateOf<CalendarDayStreak?>(null) }
    var triggersConfettiCelebration by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }

    val tasksCompleted = dailyTasks.count { it.isCompleted }
    val totalTasks = dailyTasks.size

    val palette = streakPalette()

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val flamePulse by rememberInfiniteTransition(label = "flame_pulse").animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
                            label = "flame_pulse_value"
                        )
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .scale(if (activeStreak > 0) 1f + 0.05f * flamePulse else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (activeStreak > 0) palette.ochreContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Flame",
                                tint = if (activeStreak > 0) palette.ochre else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
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
                            }

                            Text(
                                text = when {
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.ochreContainer)
                            .clickable { onOpenShieldModal() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("study_tracker_streak_shield_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = palette.ochre,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streakShieldsCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.ochre
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(2.dp)
                                    .background(palette.ochre.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Record",
                                tint = palette.forest,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$longestStreak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.forest
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                            DayStreakStatus.GOAL_MET -> Triple(palette.forestContainer, palette.forest, palette.forest)
                            DayStreakStatus.STUDIED -> Triple(palette.ochreContainer, palette.ochre, palette.ochre)
                            DayStreakStatus.PENDING_TODAY -> Triple(palette.sageContainer, palette.sage, palette.sage)
                            DayStreakStatus.MISSED_RESET -> Triple(palette.terracottaContainer, palette.terracotta, palette.earth)
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
                                        color = if (dayStreak.status == DayStreakStatus.MISSED_RESET) palette.earth else MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    when (dayStreak.status) {
                                        DayStreakStatus.GOAL_MET -> Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Goal Met",
                                            tint = palette.forest,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        DayStreakStatus.STUDIED -> Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Studied",
                                            tint = palette.ochre,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        DayStreakStatus.PENDING_TODAY -> Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(palette.sage)
                                        )
                                        DayStreakStatus.MISSED_RESET -> Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Missed Day (Reset)",
                                            tint = palette.earth,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = palette.forest,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Today's Tasks",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "$tasksCompleted / $totalTasks done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tasksCompleted == totalTasks && totalTasks > 0) palette.forest else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (totalTasks > 0) {
                    AnimatedDailyGoalProgressBar(
                        currentCount = tasksCompleted,
                        targetGoal = totalTasks,
                        titleText = "Task Progress",
                        barHeight = 14.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (dailyTasks.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "No tasks yet — add your first study task below!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dailyTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (task.isCompleted) palette.forestContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = {
                                        onToggleTask(task.id)
                                        if (!task.isCompleted) {
                                            triggersConfettiCelebration = true
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = palette.forest,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )

                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (task.isCompleted) FontWeight.Medium else FontWeight.SemiBold,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { onRemoveTask(task.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove task",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        placeholder = { Text("Add a task, e.g. Review 5 cards", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                onAddTask(newTaskText)
                                newTaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (triggersConfettiCelebration) {
            CelebrationConfettiCanvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp)),
                isCelebrating = true
            )
        }
    }

    // Reset the celebration flag once the burst finishes, so completing the
    // NEXT task fires confetti again (instead of only the first one per session).
    LaunchedEffect(triggersConfettiCelebration) {
        if (triggersConfettiCelebration) {
            delay(4000)
            triggersConfettiCelebration = false
        }
    }

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
                            DayStreakStatus.GOAL_MET -> palette.forest
                            DayStreakStatus.STUDIED -> palette.ochre
                            DayStreakStatus.MISSED_RESET -> palette.earth
                            else -> palette.sage
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
                            DayStreakStatus.PENDING_TODAY -> "No study activity recorded yet today. Complete tasks to increment your streak!"
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
}
