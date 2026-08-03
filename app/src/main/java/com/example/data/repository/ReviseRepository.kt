package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.DeckEntity
import com.example.data.db.DailyStreakEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.FolderEntity
import com.example.data.db.QuizResultEntity
import com.example.data.db.ReviseDao
import com.example.data.db.StudyLogEntity
import com.example.data.spacedrepetition.ReviewRating
import com.example.data.spacedrepetition.SpacedRepetitionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Result of restoring a ReviseIQ JSON backup.
 */
data class ImportSummary(
    val decksImported: Int,
    val cardsImported: Int,
    val streakDaysMerged: Int,
    val quizzesImported: Int
)

class ReviseRepository(private val db: AppDatabase, private val dao: ReviseDao) {

    private fun now(): Long = System.currentTimeMillis()

    private fun ensureUuid(uuid: String?): String =
        if (uuid.isNullOrBlank()) UUID.randomUUID().toString() else uuid

    val allDecks: Flow<List<DeckEntity>> = dao.getAllDecks()
    val allCards: Flow<List<FlashcardEntity>> = dao.getAllCards()
    val allStudyLogs: Flow<List<StudyLogEntity>> = dao.getAllStudyLogs()
    val allDailyStreaks: Flow<List<DailyStreakEntity>> = dao.getAllDailyStreaks()
    val allQuizResults: Flow<List<QuizResultEntity>> = dao.getAllQuizResults()
    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()

    /**
     * A consistent, fresh snapshot for JSON export — reads the DB directly
     * instead of relying on in-memory StateFlow caches that may be stale or
     * empty on a cold start.
     */
    data class ExportSnapshot(
        val decks: List<DeckEntity>,
        val cards: List<FlashcardEntity>,
        val streaks: List<DailyStreakEntity>,
        val quizResults: List<QuizResultEntity>
    )

    /**
     * Restores data from a ReviseIQ JSON backup (same format produced by
     * generateExportJson). Decks/cards/quizzes are always imported as NEW rows
     * with fresh uuids so nothing is overwritten; daily streaks are merged
     * day-by-day keeping the max of each metric. Returns what was imported.
     */
    suspend fun importFromJson(json: String): ImportSummary = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val now = now()
        var decksImported = 0
        var cardsImported = 0
        var streakDaysMerged = 0
        var quizzesImported = 0

        db.withTransaction {
            // Decks + cards (old deckId -> new local id map for quiz re-linking).
            val deckIdMap = mutableMapOf<Long, Long>()
            val decksArray = root.optJSONArray("decks") ?: JSONArray()
            for (i in 0 until decksArray.length()) {
                val deckJson = decksArray.optJSONObject(i) ?: continue
                val title = deckJson.optString("title", "Imported Deck").trim()
                if (title.isBlank()) continue
                val newDeckId = dao.insertDeck(
                    DeckEntity(
                        title = title,
                        description = deckJson.optString("description", ""),
                        category = deckJson.optString("category", ""),
                        colorHex = deckJson.optString("colorHex", "#6366F1"),
                        createdAt = deckJson.optLong("createdAt", now),
                        uuid = UUID.randomUUID().toString(),
                        updatedAtMillis = now
                    )
                )
                deckIdMap[deckJson.optLong("deckId")] = newDeckId
                decksImported++

                val cardsArray = deckJson.optJSONArray("flashcards") ?: JSONArray()
                val cards = mutableListOf<FlashcardEntity>()
                for (j in 0 until cardsArray.length()) {
                    val cardJson = cardsArray.optJSONObject(j) ?: continue
                    cards += FlashcardEntity(
                        deckId = newDeckId,
                        front = cardJson.optString("front", ""),
                        back = cardJson.optString("back", ""),
                        hint = cardJson.optString("hint", ""),
                        boxLevel = cardJson.optInt("boxLevel", 1),
                        intervalDays = cardJson.optInt("intervalDays", 1),
                        repetitions = cardJson.optInt("repetitions", 0),
                        easeFactor = cardJson.optDouble("easeFactor", 2.5).toFloat(),
                        lastReviewed = if (cardJson.has("lastReviewed")) cardJson.getLong("lastReviewed") else null,
                        nextReviewDate = cardJson.optLong("nextReviewDate", now),
                        lastRating = cardJson.optString("lastRating", ""),
                        uuid = UUID.randomUUID().toString(),
                        updatedAtMillis = now
                    )
                }
                if (cards.isNotEmpty()) {
                    dao.insertCards(cards)
                    cardsImported += cards.size
                }
            }

            // Daily streaks: merge by date keeping the max of each metric.
            val stats = root.optJSONObject("studyStatistics") ?: JSONObject()
            val streaksArray = stats.optJSONArray("dailyStreaks") ?: JSONArray()
            for (i in 0 until streaksArray.length()) {
                val s = streaksArray.optJSONObject(i) ?: continue
                val dateString = s.optString("date", "")
                if (dateString.isBlank()) continue
                val existing = dao.getStreakForDate(dateString)
                dao.insertOrUpdateStreak(
                    DailyStreakEntity(
                        dateString = dateString,
                        cardsReviewed = maxOf(existing?.cardsReviewed ?: 0, s.optInt("cardsReviewed", 0)),
                        quizzesCompleted = maxOf(existing?.quizzesCompleted ?: 0, s.optInt("quizzesCompleted", 0)),
                        studyDurationMinutes = maxOf(existing?.studyDurationMinutes ?: 0, s.optInt("studyDurationMinutes", 0)),
                        goalTargetCards = maxOf(existing?.goalTargetCards ?: 20, s.optInt("goalTargetCards", 20)),
                        targetMet = (existing?.targetMet ?: false) || s.optBoolean("targetMet", false),
                        uuid = "streak-$dateString",
                        updatedAtMillis = now
                    )
                )
                streakDaysMerged++
            }

            // Quiz results: fresh rows, deck references remapped to the new ids.
            val quizArray = stats.optJSONArray("quizHistory") ?: JSONArray()
            for (i in 0 until quizArray.length()) {
                val q = quizArray.optJSONObject(i) ?: continue
                val oldDeckId = q.optLong("deckId", 0L)
                dao.insertQuizResult(
                    QuizResultEntity(
                        deckId = deckIdMap[oldDeckId] ?: oldDeckId,
                        deckTitle = q.optString("deckTitle", ""),
                        totalQuestions = q.optInt("totalQuestions", 0),
                        correctAnswers = q.optInt("correctAnswers", 0),
                        scorePercentage = q.optInt("scorePercentage", 0),
                        durationSeconds = q.optInt("durationSeconds", 0),
                        timestamp = q.optLong("timestamp", now),
                        uuid = UUID.randomUUID().toString(),
                        updatedAtMillis = now
                    )
                )
                quizzesImported++
            }
        }

        ImportSummary(decksImported, cardsImported, streakDaysMerged, quizzesImported)
    }

    suspend fun buildExportSnapshot(deckId: Long? = null): ExportSnapshot = withContext(Dispatchers.IO) {
        val decks = if (deckId == null) {
            dao.getAllDecksList()
        } else {
            dao.getDeckById(deckId)?.let { listOf(it) } ?: emptyList()
        }
        val deckIds = decks.map { it.id }
        ExportSnapshot(
            decks = decks,
            cards = if (deckIds.isEmpty()) emptyList() else dao.getCardsForDeckIds(deckIds),
            streaks = dao.getAllStreaksList(),
            quizResults = dao.getQuizResultsList()
        )
    }

    fun getDueCards(timestamp: Long = System.currentTimeMillis()): Flow<List<FlashcardEntity>> {
        return dao.getDueCards(timestamp)
    }

    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>> {
        return dao.getCardsForDeck(deckId)
    }

    suspend fun getCardsForDeckList(deckId: Long): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        dao.getCardsForDeckList(deckId)
    }

    suspend fun insertDeck(deck: DeckEntity): Long = withContext(Dispatchers.IO) {
        val stamped = deck.copy(
            uuid = ensureUuid(deck.uuid),
            updatedAtMillis = now()
        )
        dao.insertDeck(stamped)
    }

    suspend fun updateDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        dao.updateDeck(
            deck.copy(
                uuid = ensureUuid(deck.uuid),
                updatedAtMillis = now()
            )
        )
    }

    suspend fun deleteDeck(deck: DeckEntity) = db.withTransaction {
        val ts = now()
        // Soft delete keeps the row locally until the cloud confirms it, so the
        // tombstone can be pushed during sync.
        dao.softDeleteDeck(deck.id, ts)
        dao.softDeleteCardsForDeck(deck.id, ts)
    }

    suspend fun deleteCardsForDeck(deckId: Long) = withContext(Dispatchers.IO) {
        dao.softDeleteCardsForDeck(deckId, now())
    }

    // --- Folders ---
    suspend fun insertFolder(folder: FolderEntity): Long = withContext(Dispatchers.IO) {
        val stamped = folder.copy(
            uuid = ensureUuid(folder.uuid),
            updatedAtMillis = now()
        )
        dao.insertFolder(stamped)
    }

    suspend fun updateFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        dao.updateFolder(
            folder.copy(
                uuid = ensureUuid(folder.uuid),
                updatedAtMillis = now()
            )
        )
    }

    suspend fun deleteFolder(folder: FolderEntity, deleteDecksInside: Boolean) = db.withTransaction {
        val ts = now()
        if (deleteDecksInside) {
            dao.softDeleteCardsInFolder(folder.id, ts)
            dao.softDeleteDecksInFolder(folder.id, ts)
        } else {
            dao.clearFolderFromDecks(folder.id, ts)
        }
        dao.softDeleteFolder(folder.id, ts)
    }

    suspend fun insertCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCard(
            card.copy(
                uuid = ensureUuid(card.uuid),
                updatedAtMillis = now()
            )
        )
    }

    suspend fun insertCards(cards: List<FlashcardEntity>) = withContext(Dispatchers.IO) {
        dao.insertCards(
            cards.map {
                it.copy(
                    uuid = ensureUuid(it.uuid),
                    updatedAtMillis = now()
                )
            }
        )
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        dao.softDeleteCardById(cardId, now())
    }

    /**
     * Process a flashcard review rating, update SM-2 parameters, log review, and update streak.
     */
    suspend fun reviewCard(
        card: FlashcardEntity,
        rating: ReviewRating,
        reviewDurationSeconds: Int = 5
    ) = withContext(Dispatchers.IO) {
        val now = now()
        val result = SpacedRepetitionEngine.calculateNextReview(card, rating, now)
        dao.updateCard(
            result.updatedCard.copy(
                lastRating = rating.name,
                uuid = ensureUuid(result.updatedCard.uuid),
                updatedAtMillis = now
            )
        )

        // Log study session
        dao.insertStudyLog(
            StudyLogEntity(
                cardId = card.id,
                deckId = card.deckId,
                timestamp = now,
                rating = rating.name,
                reviewDurationSeconds = reviewDurationSeconds,
                updatedAtMillis = now
            )
        )

        // Record streak progress for today
        incrementTodayStreak(
            cardsReviewedDelta = 1,
            studyMinutesDelta = if (reviewDurationSeconds >= 30) (reviewDurationSeconds + 59) / 60 else 0
        )
    }

    suspend fun recordQuizCompletion(
        deckId: Long,
        deckTitle: String,
        totalQuestions: Int,
        correctAnswers: Int,
        durationSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val now = now()
        val scorePct = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        dao.insertQuizResult(
            QuizResultEntity(
                deckId = deckId,
                deckTitle = deckTitle,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                scorePercentage = scorePct,
                durationSeconds = durationSeconds,
                updatedAtMillis = now
            )
        )

        incrementTodayStreak(quizzesCompletedDelta = 1, studyMinutesDelta = (durationSeconds + 59) / 60)
    }

    suspend fun logQuickStudyActivity(minutes: Int) = withContext(Dispatchers.IO) {
        // Quick-logged minutes live in today's streak row (day-keyed) so the weekly
        // total counts them exactly once and daily views see them too.
        incrementTodayStreak(
            cardsReviewedDelta = (minutes / 2).coerceAtLeast(1),
            studyMinutesDelta = minutes
        )
    }

    suspend fun logReviewSessionMinutes(minutes: Int) = withContext(Dispatchers.IO) {
        if (minutes > 0) {
            incrementTodayStreak(studyMinutesDelta = minutes)
        }
    }

    suspend fun incrementTodayStreak(
        cardsReviewedDelta: Int = 0,
        quizzesCompletedDelta: Int = 0,
        studyMinutesDelta: Int = 0
    ) = db.withTransaction {
        val now = now()
        val todayStr = getCurrentDateString()
        val existing = dao.getStreakForDate(todayStr)
        val targetGoal = existing?.goalTargetCards ?: 20

        val newReviewed = (existing?.cardsReviewed ?: 0) + cardsReviewedDelta
        val newQuizzes = (existing?.quizzesCompleted ?: 0) + quizzesCompletedDelta
        val newDuration = (existing?.studyDurationMinutes ?: 0) + studyMinutesDelta

        val streakEntity = DailyStreakEntity(
            dateString = todayStr,
            cardsReviewed = newReviewed,
            quizzesCompleted = newQuizzes,
            studyDurationMinutes = newDuration,
            goalTargetCards = targetGoal,
            targetMet = newReviewed >= targetGoal,
            uuid = existing?.uuid ?: "streak-$todayStr",
            updatedAtMillis = now
        )
        dao.insertOrUpdateStreak(streakEntity)
    }

    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Calculates consecutive day study streak count, taking Streak Shields into account.
     * Returns (streakCount, shieldsConsumed) so the ViewModel can persist consumption.
     */
    suspend fun calculateCurrentStreakCount(availableShields: Int = 0): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val streaks = dao.getAllDailyStreaks().first()
        val streakMap = streaks.associateBy { it.dateString }

        var count = 0
        var shieldsRemaining = availableShields
        val cal = Calendar.getInstance()

        val todayStr = getCurrentDateString()
        if (streakMap.containsKey(todayStr) && isActiveDay(streakMap[todayStr])) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        while (true) {
            val dStr = sdf.format(cal.time)
            val entry = streakMap[dStr]
            if (entry != null && isActiveDay(entry)) {
                count++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else if (shieldsRemaining > 0 && count > 0) {
                // Streak Shield activates to absorb missed day!
                shieldsRemaining--
                count++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        count to (availableShields - shieldsRemaining)
    }

    private fun isActiveDay(entry: DailyStreakEntity?): Boolean =
        entry != null && (entry.cardsReviewed > 0 || entry.quizzesCompleted > 0)

    /**
     * Legacy seed method - disabled to keep app completely fresh and clean for new users.
     */
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // No-op: keep app completely fresh and empty for new users.
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.deleteAllCards()
        dao.deleteAllDecks()
        dao.deleteAllStudyLogs()
        dao.deleteAllDailyStreaks()
        dao.deleteAllQuizResults()
        dao.deleteAllFolders()
    }

    /** Hard-wipe every row. Used only for account switches where the device is
     *  about to restore the new account from the cloud. */
    suspend fun wipeAllData() = db.withTransaction {
        dao.deleteAllCards()
        dao.deleteAllDecks()
        dao.deleteAllStudyLogs()
        dao.deleteAllDailyStreaks()
        dao.deleteAllQuizResults()
        dao.deleteAllFolders()
    }

    /** Tombstone everything so the reset propagates to other devices. */
    suspend fun softDeleteAllData() = db.withTransaction {
        val ts = now()
        dao.softDeleteAllDecks(ts)
        dao.softDeleteAllCards(ts)
        dao.softDeleteAllFolders(ts)
        dao.softDeleteAllStudyLogs(ts)
        dao.softDeleteAllDailyStreaks(ts)
        dao.softDeleteAllQuizResults(ts)
    }

    suspend fun resetStreakOnly() = db.withTransaction {
        dao.softDeleteAllDailyStreaks(now())
        dao.softDeleteAllStudyLogs(now())
    }

    suspend fun deleteAllDecksAndCards() = db.withTransaction {
        val ts = now()
        dao.softDeleteAllCards(ts)
        dao.softDeleteAllDecks(ts)
        dao.softDeleteAllFolders(ts)
    }
}
