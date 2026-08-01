package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import com.example.ui.components.PomodoroTimerCard
import com.example.ui.components.StudyReminderCard
import com.example.ui.components.WeeklyGoalProgressCard
import com.example.ui.theme.ForestMastery
import com.example.ui.theme.OchreStreak
import com.example.ui.theme.SagePrimary
import com.example.ui.theme.SagePrimaryContainer
import com.example.ui.theme.TerracottaSecondary
import com.example.ui.theme.TerracottaSecondaryContainer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ReviseViewModel,
    onNavigateToReview: (Long) -> Unit
) {
    val allCards by viewModel.allCards.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val scheduledSessions by viewModel.scheduledSessions.collectAsState()

    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayDateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US)
    val todayStr = dayFormat.format(Calendar.getInstance().time)

    // Calculate grid items for the current view month
    val displayDays = remember(currentCalendar.timeInMillis) {
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed for Sunday
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<Calendar?>()
        for (i in 0 until firstDayOfWeek) {
            list.add(null)
        }
        for (day in 1..maxDays) {
            val c = cal.clone() as Calendar
            c.set(Calendar.DAY_OF_MONTH, day)
            list.add(c)
        }
        list
    }

    val selectedDateStr = dayFormat.format(selectedDateCalendar.time)

    // Monthly stats
    val (monthlyCardCount, monthlySessionCount) = remember(allCards, scheduledSessions, currentCalendar) {
        val viewMonth = currentCalendar.get(Calendar.MONTH)
        val viewYear = currentCalendar.get(Calendar.YEAR)

        val cardCount = allCards.count { card ->
            val cCal = Calendar.getInstance().apply { timeInMillis = card.nextReviewDate }
            cCal.get(Calendar.MONTH) == viewMonth && cCal.get(Calendar.YEAR) == viewYear
        }

        val sessionCount = scheduledSessions.count { session ->
            val sCal = Calendar.getInstance().apply { timeInMillis = session.dateInMillis }
            sCal.get(Calendar.MONTH) == viewMonth && sCal.get(Calendar.YEAR) == viewYear
        }

        Pair(cardCount, sessionCount)
    }

    // Items for selected day
    val cardsForSelectedDate = remember(allCards, selectedDateStr) {
        allCards.filter { card ->
            val cardCal = Calendar.getInstance().apply { timeInMillis = card.nextReviewDate }
            dayFormat.format(cardCal.time) == selectedDateStr
        }
    }

    val sessionsForSelectedDate = remember(scheduledSessions, selectedDateStr) {
        scheduledSessions.filter { session ->
            val sCal = Calendar.getInstance().apply { timeInMillis = session.dateInMillis }
            dayFormat.format(sCal.time) == selectedDateStr
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header & Action Buttons
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study Plan & Calendar",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Spaced repetition plan & sessions",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showScheduleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Monthly Forecast Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${monthFormat.format(currentCalendar.time)} Overview",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Jump to today button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SagePrimaryContainer)
                                .clickable {
                                    val now = Calendar.getInstance()
                                    currentCalendar = now.clone() as Calendar
                                    selectedDateCalendar = now.clone() as Calendar
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Today",
                                    tint = SagePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Today",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SagePrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$monthlyCardCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Cards Due",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$monthlySessionCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TerracottaSecondary
                            )
                            Text(
                                text = "Scheduled Sessions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val estTime = (monthlyCardCount * 1.5 + monthlySessionCount * 30).toInt()
                            Text(
                                text = "${estTime}m",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OchreStreak
                            )
                            Text(
                                text = "Est. Study Time",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heatmap Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Workload: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SagePrimaryContainer))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Light", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SagePrimary))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Heavy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerracottaSecondary))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Session", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Push Notification Study Reminders Card
        item {
            StudyReminderCard(viewModel = viewModel)
        }

        // Pomodoro Focus Timer Card (Calendar & Study Log Integrated)
        item {
            PomodoroTimerCard(viewModel = viewModel)
        }

        // Weekly Goal Progress Card
        item {
            WeeklyGoalProgressCard(viewModel = viewModel)
        }

        // Calendar Grid Container Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val prev = currentCalendar.clone() as Calendar
                                prev.add(Calendar.MONTH, -1)
                                currentCalendar = prev
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Text(
                            text = monthFormat.format(currentCalendar.time),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                val next = currentCalendar.clone() as Calendar
                                next.add(Calendar.MONTH, 1)
                                currentCalendar = next
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        userScrollEnabled = false
                    ) {
                        items(displayDays) { calDay ->
                            if (calDay == null) {
                                Box(modifier = Modifier.size(38.dp))
                            } else {
                                val dateStr = dayFormat.format(calDay.time)
                                val isSelected = dateStr == selectedDateStr
                                val isToday = dateStr == todayStr

                                val cardCountForDay = allCards.count { card ->
                                    val cCal = Calendar.getInstance().apply { timeInMillis = card.nextReviewDate }
                                    dayFormat.format(cCal.time) == dateStr
                                }

                                val hasSessionForDay = scheduledSessions.any { session ->
                                    val sCal = Calendar.getInstance().apply { timeInMillis = session.dateInMillis }
                                    dayFormat.format(sCal.time) == dateStr
                                }

                                val cellBg = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    cardCountForDay >= 5 -> SagePrimary.copy(alpha = 0.85f)
                                    cardCountForDay >= 1 -> SagePrimaryContainer
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isSelected -> Color.White
                                    cardCountForDay >= 5 -> Color.White
                                    cardCountForDay >= 1 -> SagePrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellBg)
                                        .then(
                                            if (isToday && !isSelected) {
                                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                            } else Modifier
                                        )
                                        .clickable { selectedDateCalendar = calDay },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = calDay.get(Calendar.DAY_OF_MONTH).toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || cardCountForDay > 0 || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )

                                        if (hasSessionForDay) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected || cardCountForDay >= 5) Color.White else TerracottaSecondary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDateFormat.format(selectedDateCalendar.time),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${cardsForSelectedDate.size} Cards • ${sessionsForSelectedDate.size} Sessions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Scheduled Study Sessions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TerracottaSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Scheduled Study Sessions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (sessionsForSelectedDate.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "No custom study sessions scheduled for this date.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            TextButton(
                                onClick = { showScheduleDialog = true }
                            ) {
                                Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TerracottaSecondary)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sessionsForSelectedDate.forEach { session ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (session.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = session.isCompleted,
                                        onCheckedChange = { viewModel.toggleSessionCompleted(session.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ForestMastery,
                                            uncheckedColor = TerracottaSecondary
                                        )
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = session.deckTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (session.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                        if (session.focusTopic.isNotBlank()) {
                                            Text(
                                                text = session.focusTopic,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TerracottaSecondaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${session.durationMinutes} min",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerracottaSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteScheduledSession(session.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Spaced Repetition Flashcards Due
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Cards Scheduled for Review",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (cardsForSelectedDate.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val firstCard = cardsForSelectedDate.firstOrNull()
                                if (firstCard != null) onNavigateToReview(firstCard.deckId)
                            }
                        ) {
                            Text("Start Review →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (cardsForSelectedDate.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Done",
                                tint = ForestMastery,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No cards scheduled for review on this date.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cardsForSelectedDate.forEach { card ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToReview(card.deckId) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.front,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Answer: ${card.back}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SagePrimaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Box ${card.boxLevel}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SagePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Interactive Schedule Study Session Dialog
    if (showScheduleDialog) {
        var selectedDeckId by remember { mutableStateOf(decks.firstOrNull()?.id ?: 1L) }
        var selectedDeckTitle by remember { mutableStateOf(decks.firstOrNull()?.title ?: "General Review") }
        var selectedDuration by remember { mutableIntStateOf(30) }
        var focusTopicText by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Schedule Study Session",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Date: ${displayDateFormat.format(selectedDateCalendar.time)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Target Deck Selector Dropdown
                    Column {
                        Text(
                            text = "Target Deck",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDeckTitle,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                decks.forEach { deck ->
                                    DropdownMenuItem(
                                        text = { Text(deck.title) },
                                        onClick = {
                                            selectedDeckId = deck.id
                                            selectedDeckTitle = deck.title
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Duration Chips
                    Column {
                        Text(
                            text = "Target Duration",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                val isSelected = selectedDuration == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedDuration = mins }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Focus Topic Notes Input
                    OutlinedTextField(
                        value = focusTopicText,
                        onValueChange = { focusTopicText = it },
                        label = { Text("Focus Topic / Session Goal") },
                        placeholder = { Text("e.g. Master Box 1 cards & practice recall") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addScheduledSession(
                            deckId = selectedDeckId,
                            deckTitle = selectedDeckTitle,
                            dateInMillis = selectedDateCalendar.timeInMillis,
                            durationMinutes = selectedDuration,
                            focusTopic = focusTopicText.ifBlank { "Spaced repetition practice" }
                        )
                        showScheduleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Schedule Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
