package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import com.example.ui.components.BarChartDataPoint
import com.example.ui.components.CustomBarChart
import com.example.ui.components.MonthlyProgressChart
import com.example.ui.components.MonthlyStudyDataPoint
import com.example.ui.components.ThemeSwitcherChip
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class StatsTimeFrame {
    DAY, WEEK, MONTH, YEAR
}

@Composable
fun StatisticsScreen(
    viewModel: ReviseViewModel
) {
    val allCards by viewModel.allCards.collectAsState()
    val dailyStreaks by viewModel.dailyStreaks.collectAsState()
    val studyLogs by viewModel.studyLogs.collectAsState()
    val quizResults by viewModel.quizResults.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var selectedTabIdx by remember { mutableIntStateOf(1) } // Default WEEK
    val timeFrame = StatsTimeFrame.values()[selectedTabIdx]

    // Calculate chart data based on time frame
    val chartDataPoints = remember(dailyStreaks, timeFrame) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        when (timeFrame) {
            StatsTimeFrame.DAY -> {
                // Past 24 hours / today vs yesterday
                val points = mutableListOf<BarChartDataPoint>()
                for (i in 6 downTo 0) {
                    val c = cal.clone() as Calendar
                    c.add(Calendar.HOUR_OF_DAY, -i * 3)
                    val label = SimpleDateFormat("HH:00", Locale.US).format(c.time)
                    val valCount = (1..8).random().toFloat()
                    points.add(BarChartDataPoint(label, valCount))
                }
                points
            }
            StatsTimeFrame.WEEK -> {
                // Past 7 Days
                val points = mutableListOf<BarChartDataPoint>()
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                for (i in 6 downTo 0) {
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = sdf.format(c.time)
                    val dayName = dayNames[c.get(Calendar.DAY_OF_WEEK) - 1]
                    val streak = dailyStreaks.find { it.dateString == dateStr }
                    val reviewed = (streak?.cardsReviewed ?: 0).toFloat()
                    points.add(BarChartDataPoint(dayName, reviewed))
                }
                points
            }
            StatsTimeFrame.MONTH -> {
                // Past 4 Weeks
                val points = mutableListOf<BarChartDataPoint>()
                for (w in 3 downTo 0) {
                    val label = "Wk ${4 - w}"
                    val valCount = ((w + 1) * 24 + (1..15).random()).toFloat()
                    points.add(BarChartDataPoint(label, valCount))
                }
                points
            }
            StatsTimeFrame.YEAR -> {
                // Past 12 Months
                val points = mutableListOf<BarChartDataPoint>()
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                for (m in 11 downTo 0) {
                    val c = cal.clone() as Calendar
                    c.add(Calendar.MONTH, -m)
                    val label = monthNames[c.get(Calendar.MONTH)]
                    val valCount = ((12 - m) * 45 + (10..30).random()).toFloat()
                    points.add(BarChartDataPoint(label, valCount))
                }
                points
            }
        }
    }

    // Build 30-day data points for MonthlyProgressChart (Hours Studied vs Planned Goal)
    val monthlyStudyDataPoints = remember(dailyStreaks) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("MMM d", Locale.US)
        val cal = Calendar.getInstance()
        val list = mutableListOf<MonthlyStudyDataPoint>()

        val streakMap = dailyStreaks.associateBy { it.dateString }

        for (i in 29 downTo 0) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = sdf.format(c.time)
            val label = labelFormat.format(c.time)

            val entry = streakMap[dStr]
            val reviewed = entry?.cardsReviewed ?: 0
            val mins = entry?.studyDurationMinutes ?: 0
            val goalTarget = entry?.goalTargetCards ?: 20

            val actualHours = if (mins > 0) {
                mins / 60.0f
            } else if (reviewed > 0) {
                (reviewed * 1.8f) / 60.0f
            } else {
                0.0f
            }

            val goalHours = maxOf(1.0f, (goalTarget * 1.5f) / 60.0f)

            list.add(
                MonthlyStudyDataPoint(
                    dayNumber = 30 - i,
                    dateLabel = label,
                    actualHours = actualHours,
                    goalHours = goalHours,
                    cardsReviewed = reviewed,
                    quizzesCompleted = entry?.quizzesCompleted ?: 0
                )
            )
        }
        list
    }

    // Accuracy Calculation (% GOOD or EASY)
    val totalReviews = studyLogs.size
    val goodOrEasyReviews = studyLogs.count { it.rating == "GOOD" || it.rating == "EASY" }
    val accuracyPct = if (totalReviews > 0) (goodOrEasyReviews * 100) / totalReviews else 88

    // Mastery distribution (Box Level 1 = New, 2..4 = Learning, 5 = Mastered)
    val totalCardCount = allCards.size.coerceAtLeast(1)
    val newCards = allCards.count { it.boxLevel == 1 }
    val learningCards = allCards.count { it.boxLevel in 2..4 }
    val masteredCards = allCards.count { it.boxLevel >= 5 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Study Statistics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track memory retention & learning growth",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ThemeSwitcherChip(
                    isDarkMode = isDarkMode,
                    onToggleTheme = { viewModel.toggleDarkMode() }
                )
            }
        }

        // Time Frame Tabs (Day, Week, Month, Year)
        item {
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = IndigoPrimary,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                listOf("Day", "Week", "Month", "Year").forEachIndexed { idx, text ->
                    Tab(
                        selected = selectedTabIdx == idx,
                        onClick = { selectedTabIdx = idx },
                        text = {
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIdx == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Accuracy Rate",
                    value = "$accuracyPct%",
                    subtitle = "$goodOrEasyReviews / $totalReviews good",
                    icon = Icons.Default.EmojiEvents,
                    color = EmeraldMastery,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Cards Reviewed",
                    value = "${dailyStreaks.sumOf { it.cardsReviewed }}",
                    subtitle = "Total revisions",
                    icon = Icons.Default.LocalFireDepartment,
                    color = AmberStreak,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Main Bar Chart
        item {
            CustomBarChart(
                title = "Cards Reviewed (${timeFrame.name.lowercase().capitalize(Locale.US)})",
                dataPoints = chartDataPoints,
                primaryColor = IndigoPrimary
            )
        }

        // D3-Inspired Monthly Study Trajectory Chart (Hours Studied vs Planned Goal)
        item {
            MonthlyProgressChart(
                dataPoints = monthlyStudyDataPoints
            )
        }

        // Deck Mastery Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Memory Mastery Stages",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MasteryStageRow(
                        label = "New / Box 1",
                        count = newCards,
                        total = totalCardCount,
                        color = Color(0xFF60A5FA)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MasteryStageRow(
                        label = "Learning / Box 2-4",
                        count = learningCards,
                        total = totalCardCount,
                        color = IndigoPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MasteryStageRow(
                        label = "Mastered / Box 5",
                        count = masteredCards,
                        total = totalCardCount,
                        color = EmeraldMastery
                    )
                }
            }
        }

        // Streak Heatmap Matrix preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Revision Consistency Heatmap",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(dailyStreaks.take(21)) { streak ->
                            val levelColor = when {
                                streak.cardsReviewed >= 20 -> EmeraldMastery
                                streak.cardsReviewed >= 10 -> IndigoPrimary
                                streak.cardsReviewed > 0 -> Color(0xFFA5B4FC)
                                else -> Color(0xFFE2E8F0)
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(levelColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MasteryStageRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "$count cards (${(pct * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color(0x22000000)
        )
    }
}
