package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DailyStreakEntity
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.ui.DailyTask
import com.example.ui.ReviseViewModel
import com.example.ui.components.AiFlashcardGeneratorDialog
import com.example.ui.components.GlobalSearchContent
import com.example.ui.components.StreakShieldBottomSheet
import com.example.ui.components.StudyStreakTracker
import com.example.ui.components.ThemeSwitcherChip
import com.example.ui.components.WeeklyGoalContent
import com.example.ui.components.WeeklyGoalSettingModal
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.EmeraldMastery
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseAlert

/** A deck with its due cards summarized for the dashboard's compact tiles. */
data class DueDeckSummary(
    val deck: DeckEntity,
    val dueCount: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val unratedCount: Int,
    val earliestDue: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ReviseViewModel,
    onNavigateToReview: (Long) -> Unit,
    onNavigateToQuiz: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToDecks: () -> Unit
) {
    val dueCards by viewModel.dueCards.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val allCards by viewModel.allCards.collectAsState()
    val dailyStreaks by viewModel.dailyStreaks.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val streakShieldsCount by viewModel.streakShieldsCount.collectAsState()
    val dailyTasks by viewModel.dailyTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val weeklyGoalHours by viewModel.weeklyStudyGoalHours.collectAsState()
    val currentWeeklyHours by viewModel.weeklyStudyHoursProgress.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var showStreakShieldSheet by remember { mutableStateOf(false) }
    var showWeeklyGoalModal by remember { mutableStateOf(false) }
    var showAiGeneratorDialog by remember { mutableStateOf(false) }

    DashboardContent(
        decks = decks,
        dueCards = dueCards,
        allCards = allCards,
        dailyStreaks = dailyStreaks,
        dailyTasks = dailyTasks,
        searchQuery = searchQuery,
        weeklyGoalHours = weeklyGoalHours,
        currentWeeklyHours = currentWeeklyHours,
        isDarkMode = isDarkMode,
        streakShieldsCount = streakShieldsCount,
        onSearchQueryChange = viewModel::setSearchQuery,
        onToggleTheme = viewModel::toggleDarkMode,
        onEditWeeklyGoal = { showWeeklyGoalModal = true },
        onQuickLogMinutes = viewModel::addQuickStudyMinutes,
        onNavigateToReview = onNavigateToReview,
        onNavigateToQuiz = onNavigateToQuiz,
        onNavigateToCalendar = onNavigateToCalendar,
        onNavigateToDecks = onNavigateToDecks,
        onOpenAiGenerator = { showAiGeneratorDialog = true },
        onAddTask = viewModel::addDailyTask,
        onRemoveTask = viewModel::removeDailyTask,
        onToggleTask = viewModel::toggleDailyTask,
        onOpenShieldModal = { showStreakShieldSheet = true }
    )

    if (showStreakShieldSheet) {
        StreakShieldBottomSheet(
            viewModel = viewModel,
            onDismiss = { showStreakShieldSheet = false }
        )
    }

    if (showWeeklyGoalModal) {
        WeeklyGoalSettingModal(
            viewModel = viewModel,
            onDismissRequest = { showWeeklyGoalModal = false }
        )
    }

    if (showAiGeneratorDialog) {
        AiFlashcardGeneratorDialog(
            viewModel = viewModel,
            decks = decks,
            folders = folders,
            aiState = aiState,
            onDismiss = { showAiGeneratorDialog = false }
        )
    }
}

@Composable
fun DashboardContent(
    decks: List<DeckEntity>,
    dueCards: List<FlashcardEntity>,
    allCards: List<FlashcardEntity>,
    dailyStreaks: List<DailyStreakEntity>,
    dailyTasks: List<DailyTask>,
    searchQuery: String,
    weeklyGoalHours: Float,
    currentWeeklyHours: Float,
    isDarkMode: Boolean,
    streakShieldsCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onEditWeeklyGoal: () -> Unit,
    onQuickLogMinutes: (Int) -> Unit,
    onNavigateToReview: (Long) -> Unit,
    onNavigateToQuiz: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToDecks: () -> Unit,
    onOpenAiGenerator: () -> Unit,
    onAddTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onOpenShieldModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Compact per-deck summary: group due cards by deck, tag the last review
    // ratings (Again/Hard/Good/Easy/New) and sort by the earliest due card.
    val dueDecks = remember(decks, dueCards) {
        dueCards.groupBy { it.deckId }
            .mapNotNull { (deckId, cards) ->
                val deck = decks.firstOrNull { it.id == deckId } ?: return@mapNotNull null
                DueDeckSummary(
                    deck = deck,
                    dueCount = cards.size,
                    againCount = cards.count { it.lastRating == "AGAIN" },
                    hardCount = cards.count { it.lastRating == "HARD" },
                    goodCount = cards.count { it.lastRating == "GOOD" },
                    easyCount = cards.count { it.lastRating == "EASY" },
                    unratedCount = cards.count { it.lastRating.isBlank() },
                    earliestDue = cards.minOf { it.nextReviewDate }
                )
            }
            .sortedBy { it.earliestDue }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
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
                        ThemeSwitcherChip(
                            isDarkMode = isDarkMode,
                            onToggleTheme = onToggleTheme
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

        item {
            GlobalSearchContent(
                searchQuery = searchQuery,
                allCards = allCards,
                decks = decks,
                onQueryChange = onSearchQueryChange,
                onClearQuery = { onSearchQueryChange("") },
                onNavigateToReview = onNavigateToReview
            )
        }

        item {
            WeeklyGoalContent(
                weeklyGoalHours = weeklyGoalHours,
                currentWeeklyHours = currentWeeklyHours,
                onEditGoal = onEditWeeklyGoal,
                onQuickLogMinutes = onQuickLogMinutes
            )
        }

        item {
            StudyStreakTracker(
                dailyStreaks = dailyStreaks,
                dailyTasks = dailyTasks,
                streakShieldsCount = streakShieldsCount,
                onAddTask = onAddTask,
                onRemoveTask = onRemoveTask,
                onToggleTask = onToggleTask,
                onOpenShieldModal = onOpenShieldModal
            )
        }

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
            items(dueDecks) { summary ->
                val deckDotColor = runCatching {
                    Color(android.graphics.Color.parseColor(summary.deck.colorHex))
                }.getOrDefault(IndigoPrimary)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReview(summary.deck.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(deckDotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = summary.deck.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (summary.againCount > 0) {
                                    RatingChip(label = "Again", count = summary.againCount, color = RoseAlert)
                                }
                                if (summary.hardCount > 0) {
                                    RatingChip(label = "Hard", count = summary.hardCount, color = AmberStreak)
                                }
                                if (summary.goodCount > 0) {
                                    RatingChip(label = "Good", count = summary.goodCount, color = IndigoPrimary)
                                }
                                if (summary.easyCount > 0) {
                                    RatingChip(label = "Easy", count = summary.easyCount, color = EmeraldMastery)
                                }
                                if (summary.unratedCount > 0) {
                                    RatingChip(
                                        label = "New",
                                        count = summary.unratedCount,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RoseAlert.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, RoseAlert.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${summary.dueCount} due",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseAlert,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Review ${summary.deck.title}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

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
                        title = "Review Cards",
                        subtitle = if (dueCards.isNotEmpty()) "${dueCards.size} due now" else "All caught up",
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
                        title = "AI Generate",
                        subtitle = "Flashcards from topic",
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
                        title = "Study Planner",
                        subtitle = "Calendar & sessions",
                        icon = Icons.Default.CalendarMonth,
                        gradientColors = listOf(MaterialTheme.colorScheme.tertiary, Color(0xFF8C5202)),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalendar
                    )
                }
            }
        }

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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tile_press_scale"
    )
    Card(
        modifier = modifier
            .scale(tileScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
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

@Composable
private fun RatingChip(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = "$label ×$count",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
