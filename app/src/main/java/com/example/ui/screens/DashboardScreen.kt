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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ReviseViewModel
import com.example.ui.components.DataExportCard
import com.example.ui.components.GlobalSearchBar
import com.example.ui.components.PomodoroTimerCard
import com.example.ui.components.StreakBanner
import com.example.ui.components.StudyReminderCard
import com.example.ui.components.StudyStreakTracker
import com.example.ui.components.WeeklyGoalProgressCard
import com.example.ui.components.SoundSwitcherChip
import com.example.ui.components.ThemeSwitcherChip
import com.example.ui.theme.CyanAI
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.components.StreakShieldBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ReviseViewModel,
    onNavigateToReview: (Long) -> Unit,
    onNavigateToQuiz: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToDecks: () -> Unit,
    onOpenAiGenerator: () -> Unit
) {
    val streakCount by viewModel.currentStreakCount.collectAsState()
    val dueCards by viewModel.dueCards.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val dailyStreaks by viewModel.dailyStreaks.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val streakShieldsCount by viewModel.streakShieldsCount.collectAsState()

    var showStreakShieldSheet by remember { mutableStateOf(false) }

    val todayStreak = dailyStreaks.find { it.dateString == viewModel.repositoryCurrentDateString() }
    val todayReviewed = todayStreak?.cardsReviewed ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Header Title
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ReviseIQ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Personal Spaced Revision Studio",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundSwitcherChip()

                        ThemeSwitcherChip(
                            isDarkMode = isDarkMode,
                            onToggleTheme = { viewModel.toggleDarkMode() }
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable { onOpenAiGenerator() }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Global Search Bar Component
        item {
            GlobalSearchBar(
                viewModel = viewModel,
                onNavigateToReview = onNavigateToReview
            )
        }

        // Weekly Study Hours Goal Progress Card
        item {
            WeeklyGoalProgressCard(viewModel = viewModel)
        }

        // Pomodoro Focus Timer Component
        item {
            PomodoroTimerCard(viewModel = viewModel)
        }

        // Data Export & Backup Component
        item {
            DataExportCard(viewModel = viewModel)
        }

        // Study Streak Tracker Component
        item {
            StudyStreakTracker(
                dailyStreaks = dailyStreaks,
                targetDailyGoal = 20,
                streakShieldsCount = streakShieldsCount,
                onLogStudyActivity = { cardsCount ->
                    viewModel.logQuickStudyActivity(cardsCount)
                },
                onOpenShieldModal = {
                    showStreakShieldSheet = true
                }
            )
        }

        // Push Notification Study Reminder Card
        item {
            StudyReminderCard(viewModel = viewModel)
        }

        // Quick Actions Grid
        item {
            Column {
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = "Review Due",
                        subtitle = "${dueCards.size} cards ready",
                        icon = Icons.Default.PlayArrow,
                        gradientColors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF233E2E)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (dueCards.isNotEmpty()) {
                                onNavigateToReview(dueCards.first().deckId)
                            } else if (decks.isNotEmpty()) {
                                onNavigateToReview(decks.first().id)
                            } else {
                                onNavigateToDecks()
                            }
                        }
                    )

                    QuickActionTile(
                        title = "AI Deck Maker",
                        subtitle = "Generate from topic",
                        icon = Icons.Default.AutoAwesome,
                        gradientColors = listOf(MaterialTheme.colorScheme.secondary, Color(0xFF883E28)),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAiGenerator
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionTile(
                        title = "Practice Quiz",
                        subtitle = "Test your recall",
                        icon = Icons.Default.Quiz,
                        gradientColors = listOf(EmeraldMastery, Color(0xFF1B4332)),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (decks.isNotEmpty()) {
                                onNavigateToQuiz(decks.first().id)
                            } else {
                                onNavigateToDecks()
                            }
                        }
                    )

                    QuickActionTile(
                        title = "Revision Calendar",
                        subtitle = "Schedule & dates",
                        icon = Icons.Default.CalendarMonth,
                        gradientColors = listOf(MaterialTheme.colorScheme.tertiary, Color(0xFF8C5202)),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalendar
                    )
                }
            }
        }

        // Due Today Queue Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Due for Revision Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (dueCards.isNotEmpty()) {
                    Text(
                        text = "${dueCards.size} Due",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                }
            }
        }

        if (dueCards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "All Caught Up",
                            tint = EmeraldMastery,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All Caught Up!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No cards are pending review right now. Great job!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(dueCards.take(4)) { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReview(card.deckId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.front,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Box Level ${card.boxLevel} • Interval ${card.intervalDays}d",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Review",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Decks Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Decks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "See All (${decks.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary,
                    modifier = Modifier.clickable { onNavigateToDecks() }
                )
            }
        }

        item {
            if (decks.isEmpty()) {
                Text(
                    text = "No decks created yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(decks) { deck ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onNavigateToReview(deck.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            try {
                                                Color(android.graphics.Color.parseColor(deck.colorHex))
                                            } catch (e: Exception) {
                                                IndigoPrimary
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Deck",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = deck.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = deck.category,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStreakShieldSheet) {
        StreakShieldBottomSheet(
            viewModel = viewModel,
            onDismiss = { showStreakShieldSheet = false }
        )
    }
}

// Helper extension to access date string cleanly
fun ReviseViewModel.repositoryCurrentDateString(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return sdf.format(java.util.Date())
}

@Composable
fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(16.dp)
        ) {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
