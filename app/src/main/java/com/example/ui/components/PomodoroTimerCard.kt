package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DeckEntity
import com.example.ui.PomodoroRuntimeState
import com.example.ui.PomodoroSummaryState
import com.example.ui.ReviseViewModel
import com.example.ui.audio.SoundEffectManager
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.delay

enum class PomodoroMode(val title: String, val defaultMinutes: Int, val themeColor: Color) {
    FOCUS("Deep Focus", 25, IndigoPrimary),
    SHORT_BREAK("Short Break", 5, EmeraldMastery),
    LONG_BREAK("Long Break", 15, VioletSecondary)
}

@Composable
fun PomodoroTimerCard(
    viewModel: ReviseViewModel,
    modifier: Modifier = Modifier
) {
    val decks by viewModel.decks.collectAsState()
    val completedPomodoros by viewModel.completedPomodorosCount.collectAsState()
    val pomodoroSummaryState by viewModel.pomodoroSummaryState.collectAsState()
    val runtimeState by viewModel.pomodoroRuntimeState.collectAsState()
    val pendingCompletionMinutes by viewModel.pendingPomodoroCompletionMinutes.collectAsState()

    PomodoroTimerContent(
        decks = decks,
        completedPomodoros = completedPomodoros,
        pomodoroSummaryState = pomodoroSummaryState,
        pomodoroRuntimeState = runtimeState,
        pendingCompletionMinutes = pendingCompletionMinutes,
        onToggleRunning = { viewModel.togglePomodoroRunning() },
        onReset = { viewModel.resetPomodoro() },
        onSetMode = { mode, minutes -> viewModel.setPomodoroMode(mode, minutes) },
        onSetDuration = { minutes -> viewModel.setPomodoroDuration(minutes) },
        onCountdownFinished = { viewModel.completePomodoroNow() },
        onRetrySummary = { deckTitle, focusTopic, durationMinutes ->
            viewModel.generatePomodoroSummary(
                deckTitle = deckTitle,
                focusTopic = focusTopic,
                durationMinutes = durationMinutes
            )
        },
        onScheduleRecommendedReview = { deckTitle, focusTopic ->
            // Attribute the follow-up to a real deck (the one whose title was
            // used, or the first deck), never a hardcoded id.
            val targetDeck = decks.find { it.title.equals(deckTitle, ignoreCase = true) }
                ?: decks.firstOrNull()
            viewModel.addScheduledSession(
                deckId = targetDeck?.id ?: 0L,
                deckTitle = targetDeck?.title ?: deckTitle,
                dateInMillis = System.currentTimeMillis() + 86400000L,
                durationMinutes = 20,
                focusTopic = "Review: $focusTopic"
            )
        },
        onDismissCompletion = { viewModel.consumePomodoroCompletion() },
        modifier = modifier
    )
}

@Composable
fun PomodoroTimerContent(
    decks: List<DeckEntity>,
    completedPomodoros: Int,
    pomodoroSummaryState: PomodoroSummaryState,
    pomodoroRuntimeState: PomodoroRuntimeState,
    pendingCompletionMinutes: Int?,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onSetMode: (PomodoroMode, Int) -> Unit,
    onSetDuration: (Int) -> Unit,
    onCountdownFinished: () -> Unit,
    onRetrySummary: (deckTitle: String, focusTopic: String, durationMinutes: Int) -> Unit,
    onScheduleRecommendedReview: (deckTitle: String, focusTopic: String) -> Unit,
    onDismissCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val defaultDeckTitle = remember(decks) {
        decks.firstOrNull()?.title ?: "General Study Focus"
    }

    val activeMode = runCatching {
        PomodoroMode.valueOf(pomodoroRuntimeState.activeModeName)
    }.getOrDefault(PomodoroMode.FOCUS)
    val totalSessionMinutes = pomodoroRuntimeState.totalMinutes
    val isRunning = pomodoroRuntimeState.isRunning
    val endTimeMillis = pomodoroRuntimeState.endTimeMillis

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // Wall-clock display ticker: only refreshes the UI. The actual countdown
    // lives in the ViewModel (endTimeMillis), so backgrounded time still counts.
    LaunchedEffect(isRunning, endTimeMillis) {
        if (isRunning) {
            nowMillis = System.currentTimeMillis()
            while (true) {
                delay(1000L)
                nowMillis = System.currentTimeMillis()
                if (nowMillis >= endTimeMillis) {
                    onCountdownFinished()
                    break
                }
            }
        }
    }

    // A session that finished in the background is finalized by the ViewModel;
    // when it lands here, surface the completion dialog.
    LaunchedEffect(pendingCompletionMinutes) {
        if (pendingCompletionMinutes != null) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (_: Throwable) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            soundManager.playCompletionArpeggio()
            showCompletionDialog = true
        }
    }

    val completedSessionMinutes = pendingCompletionMinutes ?: totalSessionMinutes

    val timeLeftSeconds = if (isRunning) {
        maxOf(0L, (endTimeMillis - nowMillis) / 1000L).toInt()
    } else {
        (pomodoroRuntimeState.remainingMillis / 1000L).toInt()
    }

    val progressFraction = remember(timeLeftSeconds, totalSessionMinutes) {
        if (totalSessionMinutes > 0) {
            (1f - (timeLeftSeconds.toFloat() / (totalSessionMinutes * 60f))).coerceIn(0f, 1f)
        } else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "pomodoro_ring_progress"
    )

    val currentThemeColor by animateColorAsState(
        targetValue = activeMode.themeColor,
        animationSpec = tween(400),
        label = "pomodoro_theme_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_timer_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(currentThemeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pomodoro Timer",
                        tint = currentThemeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Pomodoro Focus Timer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Boost focus with timed study sprints",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PomodoroMode.values().forEach { mode ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) mode.themeColor else Color.Transparent)
                            .clickable {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                } catch (_: Throwable) {}
                                onSetMode(mode, mode.defaultMinutes)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .testTag("pomodoro_circular_countdown")
            ) {
                val trackColor = currentThemeColor.copy(alpha = 0.15f)
                val strokeWidth = 12.dp

                Canvas(modifier = Modifier.size(180.dp)) {
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth.toPx())
                    )
                    drawArc(
                        color = currentThemeColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mins = timeLeftSeconds / 60
                    val secs = timeLeftSeconds % 60
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", mins, secs),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (isRunning) "FOCUSING" else "PAUSED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentThemeColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = currentThemeColor.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = currentThemeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$completedPomodoros sessions completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentThemeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        onReset()
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("pomodoro_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } catch (_: Throwable) {}
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleRunning()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentThemeColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .width(130.dp)
                        .testTag("pomodoro_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start Focus",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "PAUSE" else "START",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        val next = when (activeMode) {
                            PomodoroMode.FOCUS -> PomodoroMode.SHORT_BREAK
                            PomodoroMode.SHORT_BREAK -> PomodoroMode.FOCUS
                            PomodoroMode.LONG_BREAK -> PomodoroMode.FOCUS
                        }
                        onSetMode(next, next.defaultMinutes)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("pomodoro_skip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(15, 25, 45, 50).forEach { mins ->
                    val isSelectedDuration = totalSessionMinutes == mins
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSetDuration(mins)
                            }
                            .testTag("pomodoro_preset_$mins"),
                        color = if (isSelectedDuration) currentThemeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelectedDuration) androidx.compose.foundation.BorderStroke(1.dp, currentThemeColor) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedDuration) currentThemeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCompletionDialog) {
        AlertDialog(
            onDismissRequest = {
                showCompletionDialog = false
                onDismissCompletion()
            },
            modifier = Modifier.testTag("pomodoro_completion_dialog"),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎉 FOCUS SESSION COMPLETED! 🎉", fontSize = 16.sp, fontWeight = FontWeight.Black, color = EmeraldMastery)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Awesome job! You completed a $completedSessionMinutes-minute study sprint for '$defaultDeckTitle'.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CyanAI.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAI.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanAI,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini AI Review & Spaced Interval",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAI
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            when (val state = pomodoroSummaryState) {
                                is PomodoroSummaryState.Loading -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = CyanAI,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Analyzing concepts reviewed & calculating next study interval...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                is PomodoroSummaryState.Success -> {
                                    Text(
                                        text = state.summaryText,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            onScheduleRecommendedReview(
                                                defaultDeckTitle,
                                                "Core Concepts & Active Recall"
                                            )
                                            Toast.makeText(context, "Added recommended review session to Calendar! 📅", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAI)
                                    ) {
                                        Text(
                                            text = "Schedule Recommended Review 📅",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                is PomodoroSummaryState.Error -> {
                                    Column {
                                        Text(
                                            text = state.message,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = {
                                                onRetrySummary(defaultDeckTitle, "Core Concepts & Active Recall", completedSessionMinutes)
                                            }
                                        ) {
                                            Text("Retry AI Summary 🔄", fontSize = 12.sp)
                                        }
                                    }
                                }
                                PomodoroSummaryState.Idle -> {
                                    Text(
                                        text = "Generating AI insights...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = EmeraldMastery.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅ Added +$completedSessionMinutes mins to Weekly Study Goal Progress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldMastery)
                            Text("📅 Session logged to Calendar & Daily Streak History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        onDismissCompletion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMastery),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue Momentum 🔥", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
