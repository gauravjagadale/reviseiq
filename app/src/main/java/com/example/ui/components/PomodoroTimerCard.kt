package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.PomodoroSummaryState
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.TerracottaSecondary
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import com.example.ui.audio.SoundEffectManager

enum class PomodoroMode(val title: String, val defaultMinutes: Int, val themeColor: Color) {
    FOCUS("Deep Focus 🎯", 25, IndigoPrimary),
    SHORT_BREAK("Short Break ☕", 5, EmeraldMastery),
    LONG_BREAK("Long Break 🌴", 15, VioletSecondary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimerCard(
    viewModel: ReviseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember(context) { SoundEffectManager(context) }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val decks by viewModel.decks.collectAsState()
    val completedPomodoros by viewModel.completedPomodorosCount.collectAsState()

    var activeMode by remember { mutableStateOf(PomodoroMode.FOCUS) }
    var totalSessionMinutes by remember { mutableIntStateOf(activeMode.defaultMinutes) }
    var timeLeftSeconds by remember { mutableIntStateOf(activeMode.defaultMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }

    var selectedDeckTitle by remember { mutableStateOf("") }
    var focusTopicText by remember { mutableStateOf("Core Concepts & Active Recall") }
    var isExpandedSettings by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var isDeckDropdownExpanded by remember { mutableStateOf(false) }

    // Keep active deck title synced with available decks
    LaunchedEffect(decks) {
        if (selectedDeckTitle.isEmpty() && decks.isNotEmpty()) {
            selectedDeckTitle = decks.first().title
        }
    }

    // Countdown Timer Loop
    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds -= 1
        } else if (isRunning && timeLeftSeconds == 0) {
            isRunning = false
            try {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (_: Throwable) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            soundManager.playCompletionArpeggio()

            if (activeMode == PomodoroMode.FOCUS) {
                val targetTitle = selectedDeckTitle.ifEmpty { "General Study Focus" }
                viewModel.recordPomodoroSession(
                    deckTitle = targetTitle,
                    focusTopic = focusTopicText,
                    durationMinutes = totalSessionMinutes
                )
                viewModel.generatePomodoroSummary(
                    deckTitle = targetTitle,
                    focusTopic = focusTopicText,
                    durationMinutes = totalSessionMinutes
                )
                showCompletionDialog = true
            }

            // Auto toggle to next mode
            if (activeMode == PomodoroMode.FOCUS) {
                val nextMode = if ((completedPomodoros + 1) % 4 == 0) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
                activeMode = nextMode
                totalSessionMinutes = nextMode.defaultMinutes
                timeLeftSeconds = nextMode.defaultMinutes * 60
            } else {
                activeMode = PomodoroMode.FOCUS
                totalSessionMinutes = PomodoroMode.FOCUS.defaultMinutes
                timeLeftSeconds = PomodoroMode.FOCUS.defaultMinutes * 60
            }
        }
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
            // Header Row: Title & Session Count Badge
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
                            text = "Pomodoro Focus Timer ⏱️",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Calendar & Study Log Integrated",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentThemeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = currentThemeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$completedPomodoros Done 🏆",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentThemeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector Segmented Tabs
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
                                isRunning = false
                                activeMode = mode
                                totalSessionMinutes = mode.defaultMinutes
                                timeLeftSeconds = mode.defaultMinutes * 60
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

            // Main Circular Countdown Timer Display
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .testTag("pomodoro_circular_countdown")
            ) {
                // Background Track and Active Progress Ring
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

                // Digital Clock Inside Circle
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
                        text = if (isRunning) "FOCUSING ⚡" else "PAUSED ⏸️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentThemeColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Buttons Row: Play/Pause, Reset, Skip
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        isRunning = false
                        timeLeftSeconds = totalSessionMinutes * 60
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

                // Primary Play / Pause Button
                Button(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } catch (_: Throwable) {}
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isRunning = !isRunning
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

                // Skip / Next Mode Button
                IconButton(
                    onClick = {
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Throwable) {}
                        isRunning = false
                        val next = when (activeMode) {
                            PomodoroMode.FOCUS -> PomodoroMode.SHORT_BREAK
                            PomodoroMode.SHORT_BREAK -> PomodoroMode.FOCUS
                            PomodoroMode.LONG_BREAK -> PomodoroMode.FOCUS
                        }
                        activeMode = next
                        totalSessionMinutes = next.defaultMinutes
                        timeLeftSeconds = next.defaultMinutes * 60
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

            Spacer(modifier = Modifier.height(16.dp))

            // Duration Presets Chips
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
                                isRunning = false
                                totalSessionMinutes = mins
                                timeLeftSeconds = mins * 60
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

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Calendar & Target Deck Settings Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpandedSettings = !isExpandedSettings }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = currentThemeColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedDeckTitle.isNotEmpty()) "Target: $selectedDeckTitle" else "Configure Study Target",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpandedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expandable Options Panel
            AnimatedVisibility(
                visible = isExpandedSettings,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "LINK TO CALENDAR & STUDY DECK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = currentThemeColor,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Deck Picker Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isDeckDropdownExpanded,
                        onExpandedChange = { isDeckDropdownExpanded = !isDeckDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedDeckTitle.ifEmpty { "General Focus Session" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Study Deck", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeckDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isDeckDropdownExpanded,
                            onDismissRequest = { isDeckDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("General Study Focus 📚") },
                                onClick = {
                                    selectedDeckTitle = "General Study Focus"
                                    isDeckDropdownExpanded = false
                                }
                            )
                            decks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text("${deck.title} (${deck.category})") },
                                    onClick = {
                                        selectedDeckTitle = deck.title
                                        isDeckDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = focusTopicText,
                        onValueChange = { focusTopicText = it },
                        label = { Text("Focus Topic / Sub-Goal", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }
    }

    // Completion Celebration Modal
    if (showCompletionDialog) {
        val pomodoroSummaryState by viewModel.pomodoroSummaryState.collectAsState()

        AlertDialog(
            onDismissRequest = {
                showCompletionDialog = false
                viewModel.clearPomodoroSummary()
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
                        text = "Awesome job! You completed a $totalSessionMinutes-minute study sprint for '${selectedDeckTitle.ifEmpty { "General Study Focus" }}'.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gemini AI Summary Container
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
                                            viewModel.addScheduledSession(
                                                deckId = 1L,
                                                deckTitle = selectedDeckTitle.ifEmpty { "General Study Focus" },
                                                dateInMillis = System.currentTimeMillis() + 86400000L, // Tomorrow
                                                durationMinutes = 20,
                                                focusTopic = "Review: $focusTopicText"
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
                                                viewModel.generatePomodoroSummary(
                                                    deckTitle = selectedDeckTitle.ifEmpty { "General Study Focus" },
                                                    focusTopic = focusTopicText,
                                                    durationMinutes = totalSessionMinutes
                                                )
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
                            Text("✅ Added +$totalSessionMinutes mins to Weekly Study Goal Progress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldMastery)
                            Text("📅 Session logged to Calendar & Daily Streak History", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompletionDialog = false
                        viewModel.clearPomodoroSummary()
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
