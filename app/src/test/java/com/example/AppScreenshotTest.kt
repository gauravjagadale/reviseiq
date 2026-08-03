package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.db.DailyStreakEntity
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.ui.DailyTask
import com.example.ui.screens.CalendarContent
import com.example.ui.screens.DashboardContent
import com.example.ui.theme.ReviseIQTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class AppScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val now = System.currentTimeMillis()
  private val day = TimeUnit.DAYS.toMillis(1)

  private val decks = listOf(
    DeckEntity(id = 1, title = "Computer Science", description = "Operating systems, networking & data structures", category = "CS", colorHex = "#6366F1"),
    DeckEntity(id = 2, title = "Biology", description = "Cell biology and genetics review", category = "Science", colorHex = "#2D6A4F"),
    DeckEntity(id = 3, title = "World History", description = "Key events & figures", category = "Humanities", colorHex = "#C67D0A")
  )

  private val cards = listOf(
    FlashcardEntity(id = 1, deckId = 1, front = "What is virtual memory?", back = "A memory management technique that uses disk space as extension of RAM.", boxLevel = 3, nextReviewDate = now),
    FlashcardEntity(id = 2, deckId = 1, front = "Define a semaphore", back = "A synchronization primitive used to control access to shared resources.", boxLevel = 2, nextReviewDate = now + 2 * day),
    FlashcardEntity(id = 3, deckId = 1, front = "What does LRU stand for?", back = "Least Recently Used, a cache eviction policy.", boxLevel = 1, nextReviewDate = now - day),
    FlashcardEntity(id = 4, deckId = 2, front = "What is mitosis?", back = "Cell division producing two identical daughter cells.", boxLevel = 4, nextReviewDate = now),
    FlashcardEntity(id = 5, deckId = 2, front = "Function of ribosomes", back = "Protein synthesis.", boxLevel = 1, nextReviewDate = now + 1 * day)
  )

  private val dailyStreaks = listOf(
    DailyStreakEntity(dateString = "2026-07-31", cardsReviewed = 18, studyDurationMinutes = 45, targetMet = true),
    DailyStreakEntity(dateString = "2026-08-01", cardsReviewed = 22, studyDurationMinutes = 60, targetMet = true),
    DailyStreakEntity(dateString = "2026-08-02", cardsReviewed = 5, studyDurationMinutes = 15, targetMet = false)
  )

  private val dailyTasks = listOf(
    DailyTask(id = "1", title = "Review Box 1 cards", isCompleted = true),
    DailyTask(id = "2", title = "Pomodoro: OS chapter 5", isCompleted = true),
    DailyTask(id = "3", title = "Practice quiz: Biology", isCompleted = false)
  )

  @Test
  fun dashboard_light_screenshot() {
    composeTestRule.setContent {
      ReviseIQTheme(darkTheme = false) {
        DashboardContent(
          decks = decks,
          dueCards = cards.filter { it.nextReviewDate <= now },
          allCards = cards,
          dailyStreaks = dailyStreaks,
          dailyTasks = dailyTasks,
          searchQuery = "",
          weeklyGoalHours = 7f,
          currentWeeklyHours = 4.5f,
          isDarkMode = false,
          streakShieldsCount = 2,
          onSearchQueryChange = {},
          onToggleTheme = {},
          onEditWeeklyGoal = {},
          onQuickLogMinutes = {},
          onNavigateToReview = {},
          onNavigateToQuiz = {},
          onNavigateToCalendar = {},
          onNavigateToDecks = {},
          onOpenAiGenerator = {},
          onAddTask = {},
          onRemoveTask = {},
          onToggleTask = {},
          onOpenShieldModal = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_light.png")
  }

  @Test
  fun dashboard_dark_screenshot() {
    composeTestRule.setContent {
      ReviseIQTheme(darkTheme = true) {
        DashboardContent(
          decks = decks,
          dueCards = cards.filter { it.nextReviewDate <= now },
          allCards = cards,
          dailyStreaks = dailyStreaks,
          dailyTasks = dailyTasks,
          searchQuery = "",
          weeklyGoalHours = 7f,
          currentWeeklyHours = 4.5f,
          isDarkMode = true,
          streakShieldsCount = 2,
          onSearchQueryChange = {},
          onToggleTheme = {},
          onEditWeeklyGoal = {},
          onQuickLogMinutes = {},
          onNavigateToReview = {},
          onNavigateToQuiz = {},
          onNavigateToCalendar = {},
          onNavigateToDecks = {},
          onOpenAiGenerator = {},
          onAddTask = {},
          onRemoveTask = {},
          onToggleTask = {},
          onOpenShieldModal = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_dark.png")
  }

  @Test
  fun calendar_screenshot() {
    composeTestRule.setContent {
      ReviseIQTheme(darkTheme = false) {
        CalendarContent(
          allCards = cards,
          decks = decks,
          scheduledSessions = listOf(
            com.example.ui.ScheduledSession(id = "s1", deckId = 1, deckTitle = "Computer Science", dateInMillis = now, durationMinutes = 30, focusTopic = "Spaced repetition practice"),
            com.example.ui.ScheduledSession(id = "s2", deckId = 2, deckTitle = "Biology", dateInMillis = now + 2 * day, durationMinutes = 45, focusTopic = "Review Box 1 & 2", isCompleted = true)
          ),
          onNavigateToReview = {},
          onAddSession = { _, _, _, _, _ -> },
          onToggleSession = {},
          onDeleteSession = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calendar.png")
  }
}
