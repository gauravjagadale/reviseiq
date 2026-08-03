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
import com.example.data.db.FolderEntity
import com.example.data.db.QuizResultEntity
import com.example.data.db.StudyLogEntity
import com.example.data.repository.ImportSummary
import com.example.data.repository.ReviseRepository
import com.example.data.spacedrepetition.ReviewRating
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncResult
import com.example.notification.StudyNotificationScheduler
import com.example.ui.components.PomodoroMode
import android.os.Environment
import kotlinx.coroutines.Job
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

data class DailyTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
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

/**
 * Wall-clock pomodoro runtime state. Persisted to prefs on every change so the
 * timer keeps counting while the app is backgrounded or the process is killed.
 */
data class PomodoroRuntimeState(
    val activeModeName: String = "FOCUS",
    val totalMinutes: Int = 25,
    val endTimeMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false
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
    val folders: StateFlow<List<FolderEntity>>
    val weeklyStudyHoursProgress: StateFlow<Float>
    val shieldMilestones: StateFlow<List<StreakShieldMilestone>>

    private lateinit var syncManager: SyncManager
    private var pendingSyncJob: Job? = null
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("key_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("key_user_name", "Learner") ?: "Learner")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _userName.value = trimmed
        prefs.edit().putString("key_user_name", trimmed).apply()
        scheduleSync()
    }

    fun toggleDarkMode() {
        val nextMode = !_isDarkMode.value
        _isDarkMode.value = nextMode
        prefs.edit().putBoolean("key_dark_mode", nextMode).apply()
        scheduleSync()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("key_dark_mode", enabled).apply()
        scheduleSync()
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
            val dayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 24L * 60L * 60L * 1000L
            val nextSession = _scheduledSessions.value.firstOrNull {
                !it.isCompleted && it.dateInMillis in dayStart until dayEnd
            }
            val message = if (nextSession != null) {
                "Scheduled today: ${nextSession.deckTitle} (${nextSession.durationMinutes} min) - ${nextSession.focusTopic}"
            } else {
                "Don't break your streak! Review your active cards now."
            }
            StudyNotificationScheduler.scheduleDailyReminder(
                context, _reminderHour.value, _reminderMinute.value, title, message
            )
        } else {
            StudyNotificationScheduler.cancelReminder(context)
        }
        scheduleSync()
    }

    fun setReminderTime(hour: Int, minute: Int, context: Context) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit().putInt("key_reminder_hour", hour).putInt("key_reminder_minute", minute).apply()
        if (_isRemindersEnabled.value) {
            setRemindersEnabled(true, context)
        }
        scheduleSync()
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

    private val _completedPomodorosCount = MutableStateFlow(prefs.getInt("key_completed_pomodoros_count", 3)) // Initial completed sessions count for richness
    val completedPomodorosCount: StateFlow<Int> = _completedPomodorosCount.asStateFlow()

    private val _pomodoroRuntimeState = MutableStateFlow(
        PomodoroRuntimeState(
            activeModeName = prefs.getString("key_pomodoro_mode", "FOCUS") ?: "FOCUS",
            totalMinutes = prefs.getInt("key_pomodoro_total_minutes", 25),
            endTimeMillis = prefs.getLong("key_pomodoro_end_time", 0L),
            remainingMillis = prefs.getLong("key_pomodoro_remaining_millis", 0L),
            isRunning = prefs.getBoolean("key_pomodoro_is_running", false)
        )
    )
    val pomodoroRuntimeState: StateFlow<PomodoroRuntimeState> = _pomodoroRuntimeState.asStateFlow()

    private val _pendingPomodoroCompletionMinutes = MutableStateFlow<Int?>(null)
    val pendingPomodoroCompletionMinutes: StateFlow<Int?> = _pendingPomodoroCompletionMinutes.asStateFlow()

    fun setWeeklyStudyGoalHours(hours: Float) {
        val rounded = (kotlin.math.round(hours * 10f) / 10f).coerceAtLeast(0.5f)
        _weeklyStudyGoalHours.value = rounded
        prefs.edit().putFloat("key_weekly_study_goal_hours", rounded).apply()
    }

    fun addQuickStudyMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.logQuickStudyActivity(minutes)
            refreshStreakCount()
        }
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
        persistSessions()
        addQuickStudyMinutes(durationMinutes)
        val newCount = _completedPomodorosCount.value + 1
        _completedPomodorosCount.value = newCount
        prefs.edit().putInt("key_completed_pomodoros_count", newCount).apply()
    }

    // --- Background-safe pomodoro runtime (wall-clock based) ---

    private fun persistPomodoroRuntime() {
        val s = _pomodoroRuntimeState.value
        prefs.edit()
            .putString("key_pomodoro_mode", s.activeModeName)
            .putInt("key_pomodoro_total_minutes", s.totalMinutes)
            .putLong("key_pomodoro_end_time", s.endTimeMillis)
            .putLong("key_pomodoro_remaining_millis", s.remainingMillis)
            .putBoolean("key_pomodoro_is_running", s.isRunning)
            .apply()
    }

    /** App went to the background: arm the exact alarm so the user is notified
     *  if the focus session ends while the app is closed. */
    fun onAppBackgrounded(context: Context) {
        val s = _pomodoroRuntimeState.value
        if (s.isRunning && s.endTimeMillis > System.currentTimeMillis()) {
            StudyNotificationScheduler.schedulePomodoroCompletionAlarm(context, s.endTimeMillis)
        }
    }

    /** App is visible again: drop any stale alarm and finalize sessions that
     *  finished while we were away. */
    fun onAppForegrounded(context: Context) {
        StudyNotificationScheduler.cancelPomodoroCompletionAlarm(context)
        checkPomodoroCompletion()
        // Pull anything the other devices did while this one was away.
        scheduleSync(delayMillis = 1500)
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                when (val result = syncManager.sync()) {
                    SyncResult.Success -> {
                        _lastSyncTime.value = System.currentTimeMillis()
                        _syncMessage.value = "Synced"
                    }
                    is SyncResult.Failure -> {
                        _syncMessage.value = "Sync failed: ${result.message ?: "network error"}"
                    }
                    SyncResult.NotLoggedIn -> {
                        _syncMessage.value = null
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun scheduleSync(delayMillis: Long = 3000) {
        if (pendingSyncJob?.isActive == true) return
        pendingSyncJob = viewModelScope.launch {
            delay(delayMillis)
            syncNow()
        }
    }

    fun togglePomodoroRunning() {
        val s = _pomodoroRuntimeState.value
        val now = System.currentTimeMillis()
        if (s.isRunning) {
            val remaining = maxOf(0L, s.endTimeMillis - now)
            _pomodoroRuntimeState.value = s.copy(
                isRunning = false,
                endTimeMillis = 0L,
                remainingMillis = remaining
            )
        } else {
            val remaining = if (s.remainingMillis > 0L) s.remainingMillis else s.totalMinutes * 60_000L
            _pomodoroRuntimeState.value = s.copy(
                isRunning = true,
                endTimeMillis = now + remaining,
                remainingMillis = remaining
            )
        }
        persistPomodoroRuntime()
    }

    fun resetPomodoro() {
        val s = _pomodoroRuntimeState.value
        _pomodoroRuntimeState.value = s.copy(
            isRunning = false,
            endTimeMillis = 0L,
            remainingMillis = s.totalMinutes * 60_000L
        )
        persistPomodoroRuntime()
    }

    fun setPomodoroMode(mode: PomodoroMode, minutes: Int) {
        _pomodoroRuntimeState.value = PomodoroRuntimeState(
            activeModeName = mode.name,
            totalMinutes = minutes,
            remainingMillis = minutes * 60_000L,
            isRunning = false
        )
        persistPomodoroRuntime()
    }

    fun setPomodoroDuration(minutes: Int) {
        val s = _pomodoroRuntimeState.value
        _pomodoroRuntimeState.value = s.copy(
            totalMinutes = minutes,
            remainingMillis = minutes * 60_000L,
            isRunning = false,
            endTimeMillis = 0L
        )
        persistPomodoroRuntime()
    }

    /** Dismiss the completion dialog + its AI summary panel. */
    fun consumePomodoroCompletion() {
        _pendingPomodoroCompletionMinutes.value = null
        clearPomodoroSummary()
    }

    /** Finalize a running focus session whose end time has passed. Records the
     *  session + AI summary once, then advances to the next mode. Safe to call
     *  repeatedly (guarded by isRunning). */
    fun checkPomodoroCompletion() {
        val s = _pomodoroRuntimeState.value
        if (!s.isRunning) return
        if (s.endTimeMillis > 0L && System.currentTimeMillis() < s.endTimeMillis) return
        finishPomodoroSession()
    }

    fun completePomodoroNow() = checkPomodoroCompletion()

    private fun finishPomodoroSession() {
        val s = _pomodoroRuntimeState.value
        val isFocus = s.activeModeName == PomodoroMode.FOCUS.name
        if (isFocus) {
            val deckTitle = decks.value.firstOrNull()?.title ?: "General Study Focus"
            val focusTopic = "Core Concepts & Active Recall"
            recordPomodoroSession(deckTitle, focusTopic, s.totalMinutes)
            generatePomodoroSummary(deckTitle, focusTopic, s.totalMinutes)
            _pendingPomodoroCompletionMinutes.value = s.totalMinutes
        }
        val nextMode = if (isFocus) {
            if (_completedPomodorosCount.value % 4 == 0) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
        } else {
            PomodoroMode.FOCUS
        }
        _pomodoroRuntimeState.value = PomodoroRuntimeState(
            activeModeName = nextMode.name,
            totalMinutes = nextMode.defaultMinutes,
            remainingMillis = nextMode.defaultMinutes * 60_000L,
            isRunning = false
        )
        persistPomodoroRuntime()
    }

    suspend fun generateExportJson(deckId: Long? = null): String {
        // Collect a FRESH snapshot from the database — never stale caches.
        val snapshot = repository.buildExportSnapshot(deckId)

        val rootJson = JSONObject()
        rootJson.put("appName", "ReviseIQ")
        rootJson.put("exportVersion", 1)
        rootJson.put("exportedAtMillis", System.currentTimeMillis())
        rootJson.put(
            "exportedAtIso",
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date())
        )

        // Study Statistics payload
        val statsJson = JSONObject()
        statsJson.put("weeklyGoalHours", weeklyStudyGoalHours.value)
        statsJson.put("weeklyProgressHours", weeklyStudyHoursProgress.value)
        statsJson.put("completedPomodoros", completedPomodorosCount.value)
        statsJson.put("totalDecksCount", snapshot.decks.size)
        statsJson.put("totalFlashcardsCount", snapshot.cards.size)

        val streakArray = JSONArray()
        snapshot.streaks.forEach { streak ->
            val s = JSONObject()
            s.put("date", streak.dateString)
            s.put("cardsReviewed", streak.cardsReviewed)
            s.put("quizzesCompleted", streak.quizzesCompleted)
            s.put("studyDurationMinutes", streak.studyDurationMinutes)
            s.put("goalTargetCards", streak.goalTargetCards)
            s.put("targetMet", streak.targetMet)
            streakArray.put(s)
        }
        statsJson.put("dailyStreaks", streakArray)

        val quizArray = JSONArray()
        snapshot.quizResults.forEach { q ->
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
        val cardsByDeck = snapshot.cards.groupBy { it.deckId }
        snapshot.decks.forEach { deck ->
            val dJson = JSONObject()
            dJson.put("deckId", deck.id)
            dJson.put("title", deck.title)
            dJson.put("description", deck.description)
            dJson.put("category", deck.category)
            dJson.put("colorHex", deck.colorHex)
            dJson.put("createdAt", deck.createdAt)

            val cardsArray = JSONArray()
            cardsByDeck[deck.id].orEmpty().forEach { card ->
                val cJson = JSONObject()
                cJson.put("cardId", card.id)
                cJson.put("front", card.front)
                cJson.put("back", card.back)
                cJson.put("hint", card.hint)
                cJson.put("boxLevel", card.boxLevel)
                cJson.put("repetitions", card.repetitions)
                cJson.put("intervalDays", card.intervalDays)
                cJson.put("easeFactor", card.easeFactor)
                cJson.put("lastRating", card.lastRating)
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

    /**
     * Restore a ReviseIQ JSON backup. Decks/cards/quizzes are imported as new
     * rows; daily streaks merge by date keeping the max of each metric.
     */
    suspend fun importFromJson(json: String): ImportSummary {
        val summary = repository.importFromJson(json)
        refreshStreakCount()
        scheduleSync()
        return summary
    }

    suspend fun exportDataToFile(context: Context, deckId: Long? = null): File? {
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

    private val _streakShieldsCount = MutableStateFlow(prefs.getInt("key_streak_shields_count", 0))
    val streakShieldsCount: StateFlow<Int> = _streakShieldsCount.asStateFlow()

    private val _shieldsConsumed = MutableStateFlow(prefs.getInt("key_streak_shields_consumed", 0))

    private val _claimedMilestoneIds = MutableStateFlow(
        prefs.getStringSet("key_claimed_milestone_ids", emptySet()) ?: emptySet()
    )
    val claimedMilestoneIds: StateFlow<Set<String>> = _claimedMilestoneIds.asStateFlow()

    private val _pomodoroSummaryState = MutableStateFlow<PomodoroSummaryState>(PomodoroSummaryState.Idle)
    val pomodoroSummaryState: StateFlow<PomodoroSummaryState> = _pomodoroSummaryState.asStateFlow()

    private val _scheduledSessions = MutableStateFlow<List<ScheduledSession>>(emptyList())
    val scheduledSessions: StateFlow<List<ScheduledSession>> = _scheduledSessions.asStateFlow()

    private val _dailyTasks = MutableStateFlow<List<DailyTask>>(emptyList())
    val dailyTasks: StateFlow<List<DailyTask>> = _dailyTasks.asStateFlow()

    private fun todayKey(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun sessionsToJson(sessions: List<ScheduledSession>): String {
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("deckId", s.deckId)
                    put("deckTitle", s.deckTitle)
                    put("dateInMillis", s.dateInMillis)
                    put("durationMinutes", s.durationMinutes)
                    put("focusTopic", s.focusTopic)
                    put("isCompleted", s.isCompleted)
                }
            )
        }
        return array.toString()
    }

    private fun sessionsFromJson(json: String): List<ScheduledSession> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                ScheduledSession(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    deckId = obj.optLong("deckId", 0L),
                    deckTitle = obj.optString("deckTitle", "General Study Focus"),
                    dateInMillis = obj.optLong("dateInMillis", System.currentTimeMillis()),
                    durationMinutes = obj.optInt("durationMinutes", 30),
                    focusTopic = obj.optString("focusTopic", ""),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistSessions() {
        prefs.edit().putString("key_scheduled_sessions", sessionsToJson(_scheduledSessions.value)).apply()
    }

    private fun loadDailyTasks() {
        val key = "key_daily_tasks_${todayKey()}"
        val raw = prefs.getString(key, null)
        _dailyTasks.value = if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { i ->
                    val obj = array.optJSONObject(i) ?: return@mapNotNull null
                    DailyTask(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", "Task"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun persistDailyTasks() {
        val key = "key_daily_tasks_${todayKey()}"
        val array = JSONArray()
        _dailyTasks.value.forEach { t ->
            array.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("isCompleted", t.isCompleted)
                }
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.reviseDao()
        repository = ReviseRepository(database, dao)

        syncManager = SyncManager(
            dao = dao,
            context = application,
            readSettings = {
                // Only true cross-device preferences belong in the cloud blob.
                // Per-device counters (pomodoros, streak shields) stay local so
                // one device's progress can never clobber another's.
                buildJsonObject {
                    put("user_name", _userName.value)
                    put("dark_mode", _isDarkMode.value)
                    put("reminders_enabled", _isRemindersEnabled.value)
                    put("reminder_hour", _reminderHour.value)
                    put("reminder_minute", _reminderMinute.value)
                }
            },
            applySettings = { payload ->
                val remindersChanged =
                    (payload["reminders_enabled"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
                        ?.let { it != _isRemindersEnabled.value }
                        ?: false
                val timeChanged =
                    (payload["reminder_hour"] as? JsonPrimitive)?.content?.toIntOrNull()
                        ?.let { it != _reminderHour.value }
                        ?: false
                val minutesChanged =
                    (payload["reminder_minute"] as? JsonPrimitive)?.content?.toIntOrNull()
                        ?.let { it != _reminderMinute.value }
                        ?: false
                prefs.edit()
                    .putString(
                        "key_user_name",
                        (payload["user_name"] as? JsonPrimitive)?.content ?: _userName.value
                    )
                    .putBoolean(
                        "key_dark_mode",
                        (payload["dark_mode"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
                            ?: _isDarkMode.value
                    )
                    .putBoolean(
                        "key_reminders_enabled",
                        (payload["reminders_enabled"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
                            ?: _isRemindersEnabled.value
                    )
                    .putInt(
                        "key_reminder_hour",
                        (payload["reminder_hour"] as? JsonPrimitive)?.content?.toIntOrNull()
                            ?: _reminderHour.value
                    )
                    .putInt(
                        "key_reminder_minute",
                        (payload["reminder_minute"] as? JsonPrimitive)?.content?.toIntOrNull()
                            ?: _reminderMinute.value
                    )
                    .apply()
                _userName.value = prefs.getString("key_user_name", "Learner") ?: "Learner"
                _isDarkMode.value = prefs.getBoolean("key_dark_mode", false)
                _isRemindersEnabled.value = prefs.getBoolean("key_reminders_enabled", true)
                _reminderHour.value = prefs.getInt("key_reminder_hour", 20)
                _reminderMinute.value = prefs.getInt("key_reminder_minute", 0)
                // Re-arm the notification so the device's alarm state matches
                // the newly synced settings.
                if (remindersChanged || timeChanged || minutesChanged) {
                    val context = getApplication<Application>()
                    if (_isRemindersEnabled.value) {
                        val title = "Daily Calendar Study Reminder 📚"
                        val message = "Don't break your streak! Review your active cards now."
                        StudyNotificationScheduler.scheduleDailyReminder(
                            context, _reminderHour.value, _reminderMinute.value, title, message
                        )
                    } else {
                        StudyNotificationScheduler.cancelReminder(context)
                    }
                }
            }
        )
        _lastSyncTime.value = syncManager.lastSyncTimeMillis()

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
        folders = repository.allFolders.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        weeklyStudyHoursProgress = dailyStreaks.map { streaks ->
            // Only count the current Mon-Sun week so targets reset every Monday
            calculateWeeklyStudyHours(streaks, System.currentTimeMillis())
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
                    .putInt("key_completed_pomodoros_count", 0)
                    .apply()
                _completedPomodorosCount.value = 0
            }

            // No fake seed sessions — the calendar starts from real data only.
            val savedSessions = prefs.getString("key_scheduled_sessions", null)
            _scheduledSessions.value = if (savedSessions.isNullOrBlank()) {
                emptyList()
            } else {
                sessionsFromJson(savedSessions)
            }

            loadDailyTasks()

            refreshStreakCount()
        }

        // Daily rollover: while the app stays open past midnight, reload the
        // new day's tasks and refresh the streak/stat views.
        viewModelScope.launch {
            while (true) {
                val nextMidnight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis
                delay(nextMidnight - System.currentTimeMillis() + 1000)
                loadDailyTasks()
                refreshStreakCount()
            }
        }

        // Restore pomodoro runtime: a session that was running when the process
        // died stays running (wall-clock), and its completion alarm is re-armed
        // so the user still gets notified even if the app is never reopened.
        val restored = _pomodoroRuntimeState.value
        if (restored.isRunning) {
            if (restored.endTimeMillis <= 0L) {
                // Corrupt partial state (crash between writes): stop cleanly.
                _pomodoroRuntimeState.value = restored.copy(
                    isRunning = false,
                    remainingMillis = restored.totalMinutes * 60_000L
                )
                persistPomodoroRuntime()
            } else if (restored.endTimeMillis > System.currentTimeMillis()) {
                StudyNotificationScheduler.schedulePomodoroCompletionAlarm(
                    getApplication(), restored.endTimeMillis
                )
            }
        }
    }

    fun refreshStreakCount() {
        viewModelScope.launch {
            val available = (_streakShieldsCount.value - _shieldsConsumed.value).coerceAtLeast(0)
            val (streak, consumed) = repository.calculateCurrentStreakCount(available)
            val totalConsumed = _shieldsConsumed.value + consumed
            _currentStreakCount.value = streak
            if (totalConsumed != _shieldsConsumed.value) {
                _shieldsConsumed.value = totalConsumed
                prefs.edit().putInt("key_streak_shields_consumed", totalConsumed).apply()
            }
        }
    }

    fun claimStreakShieldMilestone(milestoneId: String) {
        if (_claimedMilestoneIds.value.contains(milestoneId)) return
        val milestone = shieldMilestones.value.find { it.id == milestoneId }
        if (milestone == null || !milestone.isUnlocked) return

        val updatedSet = _claimedMilestoneIds.value + milestoneId
        _claimedMilestoneIds.value = updatedSet

        val newShieldCount = _streakShieldsCount.value + milestone.rewardShields
        _streakShieldsCount.value = newShieldCount

        prefs.edit()
            .putStringSet("key_claimed_milestone_ids", updatedSet)
            .putInt("key_streak_shields_count", newShieldCount)
            .apply()

        refreshStreakCount()
    }

    fun createDeck(title: String, description: String, category: String, colorHex: String, folderId: Long = 0): Long {
        val id = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            repository.insertDeck(
                DeckEntity(
                    title = title,
                    description = description,
                    category = category,
                    colorHex = colorHex,
                    folderId = folderId
                )
            )
        }
        scheduleSync()
        return id
    }

    fun updateDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
        scheduleSync()
    }

    fun moveDeckToFolder(deckId: Long, folderId: Long) {
        viewModelScope.launch {
            val deck = decks.value.find { it.id == deckId } ?: return@launch
            repository.updateDeck(deck.copy(folderId = folderId))
        }
        scheduleSync()
    }

    fun deleteDeck(deck: DeckEntity) {
        viewModelScope.launch {
            repository.deleteCardsForDeck(deck.id)
            repository.deleteDeck(deck)
        }
        scheduleSync()
    }

    // --- Folders ---
    fun createFolder(name: String, colorHex: String = "#6366F1"): Long {
        val folder = FolderEntity(name = name.trim(), colorHex = colorHex)
        val id = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            repository.insertFolder(folder)
        }
        scheduleSync()
        return id
    }

    fun renameFolder(folderId: Long, name: String) {
        viewModelScope.launch {
            val folder = folders.value.find { it.id == folderId } ?: return@launch
            repository.updateFolder(folder.copy(name = name.trim()))
        }
        scheduleSync()
    }

    fun deleteFolder(folderId: Long, deleteDecksInside: Boolean) {
        viewModelScope.launch {
            val folder = folders.value.find { it.id == folderId } ?: return@launch
            repository.deleteFolder(folder, deleteDecksInside)
        }
        scheduleSync()
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
        scheduleSync()
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
        scheduleSync()
    }

    fun reviewCard(card: FlashcardEntity, rating: ReviewRating, durationSeconds: Int = 5) {
        pendingReviewRatings.add(rating)
        viewModelScope.launch {
            repository.reviewCard(card, rating, durationSeconds)
            refreshStreakCount()
        }
        scheduleSync()
    }

    // --- Smart Review Scheduling ---
    private val pendingReviewRatings = mutableListOf<ReviewRating>()

    fun smartScheduleReviewSession(
        deckId: Long,
        deckTitle: String,
        sessionMinutes: Int
    ): ReviewSessionSummary? {
        // Consume the session ratings into a summary BEFORE clearing so the
        // review screen can show what happened this session.
        val followUp = followUpAfterRatings(pendingReviewRatings)
        val summary = reviewSessionSummaryOf(pendingReviewRatings)
        pendingReviewRatings.clear()

        // Log the real session duration (not per-card 5s defaults).
        viewModelScope.launch {
            repository.logReviewSessionMinutes(sessionMinutes)
            refreshStreakCount()
        }

        if (followUp != null) {
            val (days, focusTopic) = followUp
            val followUpDate = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
            addScheduledSession(
                deckId = deckId,
                deckTitle = deckTitle,
                dateInMillis = followUpDate,
                durationMinutes = 20,
                focusTopic = focusTopic
            )
        }
        return summary
    }

    fun smartScheduleAfterQuiz(deckId: Long, deckTitle: String, scorePct: Int) {
        val (days, focusTopic) = followUpAfterQuiz(scorePct)
        addScheduledSession(
            deckId = deckId,
            deckTitle = deckTitle,
            dateInMillis = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L,
            durationMinutes = 20,
            focusTopic = focusTopic
        )
    }

    fun recordQuizCompletion(
        deckId: Long,
        deckTitle: String,
        totalQuestions: Int,
        correctAnswers: Int,
        durationSeconds: Int
    ) {
        val scorePct = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        smartScheduleAfterQuiz(deckId, deckTitle, scorePct)
        viewModelScope.launch {
            repository.recordQuizCompletion(deckId, deckTitle, totalQuestions, correctAnswers, durationSeconds)
            refreshStreakCount()
        }
        scheduleSync()
    }

    fun logQuickStudyActivity(minutes: Int = 5) {
        viewModelScope.launch {
            repository.logQuickStudyActivity(minutes)
            refreshStreakCount()
        }
        scheduleSync()
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
                scheduleSync()
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
        if (deckId <= 0) return

        val existing = _scheduledSessions.value.firstOrNull {
            !it.isCompleted && it.deckId == deckId && it.focusTopic == focusTopic
        }
        val newSession = if (existing != null) {
            existing.copy(dateInMillis = dateInMillis, deckTitle = deckTitle, durationMinutes = durationMinutes)
        } else {
            ScheduledSession(
                deckId = deckId,
                deckTitle = deckTitle,
                dateInMillis = dateInMillis,
                durationMinutes = durationMinutes,
                focusTopic = focusTopic
            )
        }
        val updated = if (existing != null) {
            _scheduledSessions.value.map { if (it.id == existing.id) newSession else it }
        } else {
            _scheduledSessions.value + newSession
        }
        _scheduledSessions.value = updated
        persistSessions()
    }

    fun toggleSessionCompleted(sessionId: String) {
        _scheduledSessions.value = _scheduledSessions.value.map {
            if (it.id == sessionId) it.copy(isCompleted = !it.isCompleted) else it
        }
        persistSessions()
    }

    fun deleteScheduledSession(sessionId: String) {
        _scheduledSessions.value = _scheduledSessions.value.filter { it.id != sessionId }
        persistSessions()
    }

    fun addDailyTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        _dailyTasks.value = _dailyTasks.value + DailyTask(title = trimmed)
        persistDailyTasks()
    }

    fun removeDailyTask(taskId: String) {
        _dailyTasks.value = _dailyTasks.value.filter { it.id != taskId }
        persistDailyTasks()
    }

    fun toggleDailyTask(taskId: String) {
        val task = _dailyTasks.value.find { it.id == taskId } ?: return
        val nowCompleted = !task.isCompleted
        _dailyTasks.value = _dailyTasks.value.map {
            if (it.id == taskId) it.copy(isCompleted = nowCompleted) else it
        }
        persistDailyTasks()
        if (nowCompleted) {
            logQuickStudyActivity(1)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.softDeleteAllData()
            _completedPomodorosCount.value = 0
            _currentStreakCount.value = 0
            _scheduledSessions.value = emptyList()
            _streakShieldsCount.value = 0
            _shieldsConsumed.value = 0
            _claimedMilestoneIds.value = emptySet()
            prefs.edit()
                .putInt("key_completed_pomodoros_count", 0)
                .putString("key_scheduled_sessions", "[]")
                .putInt("key_streak_shields_count", 0)
                .putInt("key_streak_shields_consumed", 0)
                .putStringSet("key_claimed_milestone_ids", emptySet())
                .apply()
            _dailyTasks.value = emptyList()
            persistDailyTasks()
        }
        scheduleSync(500)
    }

    // --- Selective Resets ---
    fun resetStreakOnly() {
        viewModelScope.launch {
            repository.resetStreakOnly()
            _completedPomodorosCount.value = 0
            _currentStreakCount.value = 0
            _shieldsConsumed.value = 0
            // Allow streak-based milestones to be re-earned after a fresh streak.
            _claimedMilestoneIds.value = emptySet()
            prefs.edit()
                .putInt("key_completed_pomodoros_count", 0)
                .putInt("key_streak_shields_consumed", 0)
                .putStringSet("key_claimed_milestone_ids", emptySet())
                .apply()
            refreshStreakCount()
        }
        scheduleSync(500)
    }

    fun clearAllScheduledSessions() {
        _scheduledSessions.value = emptyList()
        persistSessions()
    }

    fun deleteAllDecksAndCards() {
        viewModelScope.launch {
            repository.deleteAllDecksAndCards()
            refreshStreakCount()
        }
        scheduleSync(500)
    }

    /** Wipe local content and pull the signed-in account's cloud data fresh. */
    fun wipeLocalDataForNewAccount(previousUserId: String?) {
        viewModelScope.launch {
            repository.wipeAllData()
            _scheduledSessions.value = emptyList()
            _dailyTasks.value = emptyList()
            persistSessions()
            persistDailyTasks()
            if (previousUserId != null) {
                syncManager.resetWatermark(previousUserId)
            }
            syncNow()
        }
    }
}
