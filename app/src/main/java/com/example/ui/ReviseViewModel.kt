package com.example.ui

import android.content.Context
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiStudyService
import com.example.data.api.GeneratedCard
import com.example.data.db.AppDatabase
import com.example.data.db.DeckEntity
import com.example.data.db.DailyStreakEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.QuizResultEntity
import com.example.data.db.StudyLogEntity
import com.example.data.repository.ReviseRepository
import com.example.data.spacedrepetition.ReviewRating
import com.example.notification.StudyNotificationScheduler
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AiGenerationState {
    object Idle : AiGenerationState()
    object Loading : AiGenerationState()
    data class Success(val cards: List<GeneratedCard>) : AiGenerationState()
    data class Error(val message: String) : AiGenerationState()
}

sealed class PomodoroSummaryState {
    object Idle : PomodoroSummaryState()
    object Loading : PomodoroSummaryState()
    data class Success(
        val summaryText: String,
        val deckTitle: String,
        val focusTopic: String,
        val durationMinutes: Int
    ) : PomodoroSummaryState()
    data class Error(val message: String) : PomodoroSummaryState()
}

data class ScheduledSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val deckId: Long,
    val deckTitle: String,
    val dateInMillis: Long,
    val durationMinutes: Int,
    val focusTopic: String,
    val isCompleted: Boolean = false
)

data class StreakShieldMilestone(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val targetValue: Int,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val isClaimed: Boolean,
    val rewardShields: Int = 1
)

class ReviseViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("reviseiq_prefs", Context.MODE_PRIVATE)

    private val repository: ReviseRepository

    val decks: StateFlow<List<DeckEntity>>
    val dueCards: StateFlow<List<FlashcardEntity>>
    val allCards: StateFlow<List<FlashcardEntity>>
    val dailyStreaks: StateFlow<List<DailyStreakEntity>>
    val quizResults: StateFlow<List<QuizResultEntity>>
    val studyLogs: StateFlow<List<StudyLogEntity>>
    val weeklyStudyHoursProgress: StateFlow<Float>
    val shieldMilestones: StateFlow<List<StreakShieldMilestone>>

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("key_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val nextMode = !_isDarkMode.value
        _isDarkMode.value = nextMode
        prefs.edit().putBoolean("key_dark_mode", nextMode).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("key_dark_mode", enabled).apply()
    }

    private val _isRemindersEnabled = MutableStateFlow(prefs.getBoolean("key_reminders_enabled", true))
    val isRemindersEnabled: StateFlow<Boolean> = _isRemindersEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt("key_reminder_hour", 20)) // Default 8:00 PM
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("key_reminder_minute", 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    fun setRemindersEnabled(enabled: Boolean, context: Context) {
        _isRemindersEnabled.value = enabled
        prefs.edit().putBoolean("key_reminders_enabled", enabled).apply()
        if (enabled) {
            val title = "Daily Calendar Study Reminder 📚"
            val nextSession = _scheduledSessions.value.firstOrNull { !it.isCompleted }
            val message = if (nextSession != null) {
                "Scheduled today: ${nextSession.deckTitle} (${nextSession.durationMinutes} min) - ${nextSession.focusTopic}"
            } else {
                "Don't break your study streak! Review your active cards now."
            }
            StudyNotificationScheduler.scheduleDailyReminder(
                context, _reminderHour.value, _reminderMinute.value, title, message
            )
        } else {
            StudyNotificationScheduler.cancelReminder(context)
        }
    }

    fun setReminderTime(hour: Int, minute: Int, context: Context) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit().putInt("key_reminder_hour", hour).putInt("key_reminder_minute", minute).apply()
        if (_isRemindersEnabled.value) {
            setRemindersEnabled(true, context)
        }
    }

    fun triggerTestNotification(context: Context) {
        val nextSession = _scheduledSessions.value.firstOrNull { !it.isCompleted }
        val title = "Calendar Study Reminder 📚"
        val message = if (nextSession != null) {
            "Reminder for ${nextSession.deckTitle}: ${nextSession.focusTopic} (${nextSession.durationMinutes} min study plan)"
        } else {
            "Your daily study goal is waiting! Review cards now to keep your streak active 🔥"
        }
        StudyNotificationScheduler.sendImmediateNotification(context, title, message)
    }

    private val _currentStreakCount = MutableStateFlow(0)
    val currentStreakCount: StateFlow<Int> = _currentStreakCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _weeklyStudyGoalHours = MutableStateFlow(prefs.getFloat("key_weekly_study_goal_hours", 5.0f))
    val weeklyStudyGoalHours: StateFlow<Float> = _weeklyStudyGoalHours.asStateFlow()

    private val _extraStudyMinutes = MutableStateFlow(prefs.getInt("key_extra_study_minutes", 120)) // 2.0 hrs initial progress
    val extraStudyMinutes: StateFlow<Int> = _extraStudyMinutes.asStateFlow()

    private val _completedPomodorosCount = MutableStateFlow(prefs.getInt("key_completed_pomodoros_count", 3)) // Initial completed sessions count for richness
    val completedPomodorosCount: StateFlow<Int> = _completedPomodorosCount.asStateFlow()

    fun setWeeklyStudyGoalHours(hours: Float) {
        val rounded = (kotlin.math.round(hours * 10f) / 10f).coerceAtLeast(0.5f)
        _weeklyStudyGoalHours.value = rounded
        prefs.edit().putFloat("key_weekly_study_goal_hours", rounded).apply()
    }

    fun addQuickStudyMinutes(minutes: Int) {
        val nextVal = _extraStudyMinutes.value + minutes
        _extraStudyMinutes.value = nextVal
        prefs.edit().putInt("key_extra_study_minutes", nextVal).apply()
        logQuickStudyActivity(cardsCount = (minutes / 2).coerceAtLeast(1))
    }

    fun recordPomodoroSession(deckTitle: String, focusTopic: String, durationMinutes: Int) {
        val completedSession = ScheduledSession(
            deckId = 0L,
            deckTitle = deckTitle.ifBlank { "Pomodoro Deep Focus" },
            dateInMillis = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            focusTopic = focusTopic.ifBlank { "Distraction-Free Study Session" },
            isCompleted = true
        )
        _scheduledSessions.value = _scheduledSessions.value + completedSession
        addQuickStudyMinutes(durationMinutes)
        val newCount = _completedPomodorosCount.value + 1
        _completedPomodorosCount.value = newCount
        prefs.edit().putInt("key_completed_pomodoros_count", newCount).apply()
    }

    fun generateExportJson(deckId: Long? = null): String {
        val rootJson = JSONObject()
        rootJson.put("appName", "ReviseIQ")
        rootJson.put("exportVersion", 1)
        rootJson.put("exportedAtMillis", System.currentTimeMillis())
        rootJson.put("exportedAtIso", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))

        // Study Statistics payload
        val statsJson = JSONObject()
        statsJson.put("weeklyGoalHours", weeklyStudyGoalHours.value)
        statsJson.put("weeklyProgressHours", weeklyStudyHoursProgress.value)
        statsJson.put("completedPomodoros", completedPomodorosCount.value)
        statsJson.put("totalDecksCount", decks.value.size)
        statsJson.put("totalFlashcardsCount", allCards.value.size)

        val streakArray = JSONArray()
        dailyStreaks.value.forEach { streak ->
            val s = JSONObject()
            s.put("date", streak.dateString)
            s.put("cardsReviewed", streak.cardsReviewed)
            s.put("studyDurationMinutes", streak.studyDurationMinutes)
            streakArray.put(s)
        }
        statsJson.put("dailyStreaks", streakArray)

        val quizArray = JSONArray()
        quizResults.value.forEach { q ->
            val qj = JSONObject()
            qj.put("deckId", q.deckId)
            qj.put("deckTitle", q.deckTitle)
            qj.put("scorePercentage", q.scorePercentage)
            qj.put("totalQuestions", q.totalQuestions)
            qj.put("correctAnswers", q.correctAnswers)
            qj.put("durationSeconds", q.durationSeconds)
            qj.put("timestamp", q.timestamp)
            quizArray.put(qj)
        }
        statsJson.put("quizHistory", quizArray)

        rootJson.put("studyStatistics", statsJson)

        // Decks & Flashcards payload
        val decksArray = JSONArray()
        val targetDecks = if (deckId != null) decks.value.filter { it.id == deckId } else decks.value
        targetDecks.forEach { deck ->
            val dJson = JSONObject()
            dJson.put("deckId", deck.id)
            dJson.put("title", deck.title)
            dJson.put("category", deck.category)
            dJson.put("createdAt", deck.createdAt)

            val cardsArray = JSONArray()
            allCards.value.filter { it.deckId == deck.id }.forEach { card ->
                val cJson = JSONObject()
                cJson.put("cardId", card.id)
                cJson.put("front", card.front)
                cJson.put("back", card.back)
                cJson.put("hint", card.hint)
                cJson.put("boxLevel", card.boxLevel)
                cJson.put("repetitions", card.repetitions)
                cJson.put("intervalDays", card.intervalDays)
                cJson.put("easeFactor", card.easeFactor)
                cJson.put("nextReviewDate", card.nextReviewDate)
                card.lastReviewed?.let { cJson.put("lastReviewed", it) }
                cardsArray.put(cJson)
            }
            dJson.put("flashcards", cardsArray)
            decksArray.put(dJson)
        }
        rootJson.put("decks", decksArray)

        return rootJson.toString(2)
    }

    fun exportDataToFile(context: Context, deckId: Long? = null): File? {
        return try {
            val jsonContent = generateExportJson(deckId)
            val fileName = if (deckId != null) {
                val titleSanitized = decks.value.find { it.id == deckId }?.title?.lowercase()?.replace(Regex("[^a-z0-9]"), "_") ?: "deck"
                "reviseiq_deck_${titleSanitized}_${System.currentTimeMillis()}.json"
            } else {
                "reviseiq_full_backup_${System.currentTimeMillis()}.json"
            }

            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(jsonContent)

            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val downloadFile = File(downloadsDir, fileName)
                    downloadFile.writeText(jsonContent)
                }
            } catch (_: Exception) {}

            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val _aiState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiState: StateFlow<AiGenerationState> = _aiState.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()

    private val _streakShieldsCount = MutableStateFlow(prefs.getInt("key_streak_shields_count", 1))
    val streakShieldsCount: StateFlow<Int> = _streakShieldsCount.asStateFlow()

    private val _claimedMilestoneIds = MutableStateFlow(
        prefs.getStringSet("key_claimed_milestone_ids", emptySet()) ?: emptySet()
    )
    val claimedMilestoneIds: StateFlow<Set<String>> = _claimedMilestoneIds.asStateFlow()

    private val _pomodoroSummaryState = MutableStateFlow<PomodoroSummaryState>(PomodoroSummaryState.Idle)
    val pomodoroSummaryState: StateFlow<PomodoroSummaryState> = _pomodoroSummaryState.asStateFlow()

    private val _scheduledSessions = MutableStateFlow<List<ScheduledSession>>(
        listOf(
            ScheduledSession(
                deckId = 1L,
                deckTitle = "Computer Science Fundamentals",
                dateInMillis = System.currentTimeMillis(),
                durationMinutes = 30,
                focusTopic = "Data Structures & Time Complexities",
                isCompleted = false
            ),
            ScheduledSession(
                deckId = 2L,
                deckTitle = "Spanish Vocabulary",
                dateInMillis = System.currentTimeMillis() + 86400000L * 2,
                durationMinutes = 20,
                focusTopic = "Leitner Box 1 Memory Reinforcement",
                isCompleted = false
            ),
            ScheduledSession(
                deckId = 3L,
                deckTitle = "Biology & Human Anatomy",
                dateInMillis = System.currentTimeMillis() + 86400000L * 4,
                durationMinutes = 45,
                focusTopic = "Cardiovascular & Respiratory Systems",
                isCompleted = false
            )
        )
    )
    val scheduledSessions: StateFlow<List<ScheduledSession>> = _scheduledSessions.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).reviseDao()
        repository = ReviseRepository(dao)

        decks = repository.allDecks.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        dueCards = repository.getDueCards().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allCards = repository.allCards.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        dailyStreaks = repository.allDailyStreaks.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        quizResults = repository.allQuizResults.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        studyLogs = repository.allStudyLogs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        weeklyStudyHoursProgress = kotlinx.coroutines.flow.combine(dailyStreaks, extraStudyMinutes) { streaks, extraMins ->
            val streakMins = streaks.sumOf { it.studyDurationMinutes }
            val totalMins = streakMins + extraMins
            (kotlin.math.round((totalMins / 60.0f) * 10f) / 10f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)

        shieldMilestones = kotlinx.coroutines.flow.combine(
            listOf(_currentStreakCount, dailyStreaks, _completedPomodorosCount, quizResults, decks, _claimedMilestoneIds)
        ) { array: Array<Any> ->
            val streak = array[0] as Int
            @Suppress("UNCHECKED_CAST")
            val streaksList = array[1] as List<DailyStreakEntity>
            val pomodoros = array[2] as Int
            @Suppress("UNCHECKED_CAST")
            val quizzes = array[3] as List<QuizResultEntity>
            @Suppress("UNCHECKED_CAST")
            val deckList = array[4] as List<DeckEntity>
            @Suppress("UNCHECKED_CAST")
            val claimed = array[5] as Set<String>

            val totalCards = streaksList.sumOf { it.cardsReviewed }
            listOf(
                StreakShieldMilestone(
                    id = "ms_streak_3",
                    title = "3-Day Streak Hero",
                    description = "Maintain a 3-day consecutive study streak.",
                    category = "Consistency",
                    targetValue = 3,
                    currentValue = streak,
                    isUnlocked = streak >= 3,
                    isClaimed = claimed.contains("ms_streak_3"),
                    rewardShields = 1
                ),
                StreakShieldMilestone(
                    id = "ms_cards_25",
                    title = "Card Master",
                    description = "Review 25 flashcards across any decks.",
                    category = "Practice",
                    targetValue = 25,
                    currentValue = totalCards,
                    isUnlocked = totalCards >= 25,
                    isClaimed = claimed.contains("ms_cards_25"),
                    rewardShields = 1
                ),
                StreakShieldMilestone(
                    id = "ms_pomodoro_3",
                    title = "Focus Champion",
                    description = "Complete 3 full Pomodoro focus sessions.",
                    category = "Focus",
                    targetValue = 3,
                    currentValue = pomodoros,
                    isUnlocked = pomodoros >= 3,
                    isClaimed = claimed.contains("ms_pomodoro_3"),
                    rewardShields = 1
                ),
                StreakShieldMilestone(
                    id = "ms_quiz_2",
                    title = "Quiz Mastermind",
                    description = "Complete 2 knowledge check quizzes.",
                    category = "Testing",
                    targetValue = 2,
                    currentValue = quizzes.size,
                    isUnlocked = quizzes.size >= 2,
                    isClaimed = claimed.contains("ms_quiz_2"),
                    rewardShields = 1
                ),
                StreakShieldMilestone(
                    id = "ms_deck_2",
                    title = "Deck Architect",
                    description = "Build and maintain at least 2 custom decks.",
                    category = "Creation",
                    targetValue = 2,
                    currentValue = deckList.size,
                    isUnlocked = deckList.size >= 2,
                    isClaimed = claimed.contains("ms_deck_2"),
                    rewardShields = 1
                )
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            val hasCleared = prefs.getBoolean("key_has_cleared_initial_sample_data_v3", false)
            if (!hasCleared) {
                repository.clearAllData()
                prefs.edit()
                    .putBoolean("key_has_cleared_initial_sample_data_v3", true)
                    .putInt("key_extra_study_minutes", 0)
                    .putInt("key_completed_pomodoros_count", 0)
                    .apply()
                _extraStudyMinutes.value = 0
                _completedPomodorosCount.value = 0
            }
            refreshStreakCount()
        }
    }

    fun refreshStreakCount() {
        viewModelScope.launch {
            _currentStreakCount.value = repository.calculateCurrentStreakCount(_streakShieldsCount.value)
        }
    }

    fun claimStreakShieldMilestone(milestoneId: String) {
        val updatedSet = _claimedMilestoneIds.value + milestoneId
        _claimedMilestoneIds.value = updatedSet

        val milestone = shieldMilestones.value.find { it.id == milestoneId }
        val rewardAmount = milestone?.rewardShields ?: 1
        val newShieldCount = _streakShieldsCount.value + rewardAmount
        _streakShieldsCount.value = newShieldCount

        prefs.edit()
            .putStringSet("key_claimed_milestone_ids", updatedSet)
            .putInt("key_streak_shields_count", newShieldCount)
            .apply()

        refreshStreakCount()
    }

    fun createDeck(title: String, description: String, category: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertDeck(
                DeckEntity(
                    title = title,
                    description = description,
                    category = category,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
    }

    fun deleteDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    fun addFlashcard(deckId: Long, front: String, back: String, hint: String) {
        viewModelScope.launch {
            repository.insertCard(
                FlashcardEntity(
                    deckId = deckId,
                    front = front,
                    back = back,
                    hint = hint
                )
            )
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun reviewCard(card: FlashcardEntity, rating: ReviewRating, durationSeconds: Int = 5) {
        viewModelScope.launch {
            repository.reviewCard(card, rating, durationSeconds)
            refreshStreakCount()
        }
    }

    fun recordQuizCompletion(
        deckId: Long,
        deckTitle: String,
        totalQuestions: Int,
        correctAnswers: Int,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            repository.recordQuizCompletion(deckId, deckTitle, totalQuestions, correctAnswers, durationSeconds)
            refreshStreakCount()
        }
    }

    fun logQuickStudyActivity(cardsCount: Int = 5) {
        viewModelScope.launch {
            repository.logQuickStudyActivity(cardsCount)
            refreshStreakCount()
        }
    }

    fun generateAiFlashcards(topicOrNotes: String, deckId: Long, count: Int = 5) {
        viewModelScope.launch {
            _aiState.value = AiGenerationState.Loading
            val result = GeminiStudyService.generateFlashcards(topicOrNotes, count)
            result.onSuccess { generatedList ->
                // Automatically save generated cards to deck
                val entities = generatedList.map {
                    FlashcardEntity(
                        deckId = deckId,
                        front = it.front,
                        back = it.back,
                        hint = it.hint
                    )
                }
                repository.insertCards(entities)
                _aiState.value = AiGenerationState.Success(generatedList)
            }.onFailure { err ->
                _aiState.value = AiGenerationState.Error(err.message ?: "Failed to generate AI flashcards.")
            }
        }
    }

    fun resetAiState() {
        _aiState.value = AiGenerationState.Idle
    }

    fun explainConceptWithAi(front: String, back: String) {
        viewModelScope.launch {
            _aiExplanation.value = "Asking Gemini AI to break down this concept..."
            val result = GeminiStudyService.generateAIExplanation(front, back)
            result.onSuccess { explanation ->
                _aiExplanation.value = explanation
            }.onFailure { err ->
                _aiExplanation.value = "Error: ${err.message}"
            }
        }
    }

    fun clearAiExplanation() {
        _aiExplanation.value = null
    }

    fun generatePomodoroSummary(deckTitle: String, focusTopic: String, durationMinutes: Int) {
        viewModelScope.launch {
            _pomodoroSummaryState.value = PomodoroSummaryState.Loading

            val matchedDeck = decks.value.find { it.title.equals(deckTitle, ignoreCase = true) }
            val sampleCards = if (matchedDeck != null) {
                allCards.value
                    .filter { it.deckId == matchedDeck.id }
                    .take(6)
                    .map { Pair(it.front, it.back) }
            } else emptyList()

            val result = GeminiStudyService.generatePomodoroSummary(
                deckTitle = deckTitle,
                focusTopic = focusTopic,
                durationMinutes = durationMinutes,
                sampleCardTexts = sampleCards
            )

            result.onSuccess { summary ->
                _pomodoroSummaryState.value = PomodoroSummaryState.Success(
                    summaryText = summary,
                    deckTitle = deckTitle,
                    focusTopic = focusTopic,
                    durationMinutes = durationMinutes
                )
            }.onFailure { err ->
                _pomodoroSummaryState.value = PomodoroSummaryState.Error(
                    err.message ?: "Failed to generate Gemini AI Pomodoro summary."
                )
            }
        }
    }

    fun clearPomodoroSummary() {
        _pomodoroSummaryState.value = PomodoroSummaryState.Idle
    }

    fun addScheduledSession(deckId: Long, deckTitle: String, dateInMillis: Long, durationMinutes: Int, focusTopic: String) {
        val newSession = ScheduledSession(
            deckId = deckId,
            deckTitle = deckTitle,
            dateInMillis = dateInMillis,
            durationMinutes = durationMinutes,
            focusTopic = focusTopic
        )
        _scheduledSessions.value = _scheduledSessions.value + newSession
    }

    fun toggleSessionCompleted(sessionId: String) {
        _scheduledSessions.value = _scheduledSessions.value.map {
            if (it.id == sessionId) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun deleteScheduledSession(sessionId: String) {
        _scheduledSessions.value = _scheduledSessions.value.filter { it.id != sessionId }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _extraStudyMinutes.value = 0
            _completedPomodorosCount.value = 0
            _currentStreakCount.value = 0
            _scheduledSessions.value = emptyList()
            prefs.edit()
                .putInt("key_extra_study_minutes", 0)
                .putInt("key_completed_pomodoros_count", 0)
                .remove("key_scheduled_sessions")
                .apply()
        }
    }
}
