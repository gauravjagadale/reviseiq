package com.example.data.repository

import com.example.data.db.DeckEntity
import com.example.data.db.DailyStreakEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.QuizResultEntity
import com.example.data.db.ReviseDao
import com.example.data.db.StudyLogEntity
import com.example.data.spacedrepetition.ReviewRating
import com.example.data.spacedrepetition.SpacedRepetitionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReviseRepository(private val dao: ReviseDao) {

    val allDecks: Flow<List<DeckEntity>> = dao.getAllDecks()
    val allCards: Flow<List<FlashcardEntity>> = dao.getAllCards()
    val allStudyLogs: Flow<List<StudyLogEntity>> = dao.getAllStudyLogs()
    val allDailyStreaks: Flow<List<DailyStreakEntity>> = dao.getAllDailyStreaks()
    val allQuizResults: Flow<List<QuizResultEntity>> = dao.getAllQuizResults()

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
        dao.insertDeck(deck)
    }

    suspend fun updateDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        dao.updateDeck(deck)
    }

    suspend fun deleteDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        dao.deleteDeck(deck)
    }

    suspend fun insertCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCard(card)
    }

    suspend fun insertCards(cards: List<FlashcardEntity>) = withContext(Dispatchers.IO) {
        dao.insertCards(cards)
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        dao.deleteCardById(cardId)
    }

    /**
     * Process a flashcard review rating, update SM-2 parameters, log review, and update streak.
     */
    suspend fun reviewCard(
        card: FlashcardEntity,
        rating: ReviewRating,
        reviewDurationSeconds: Int = 5
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val result = SpacedRepetitionEngine.calculateNextReview(card, rating, now)
        dao.updateCard(result.updatedCard)

        // Log study session
        dao.insertStudyLog(
            StudyLogEntity(
                cardId = card.id,
                deckId = card.deckId,
                timestamp = now,
                rating = rating.name,
                reviewDurationSeconds = reviewDurationSeconds
            )
        )

        // Record streak progress for today
        incrementTodayStreak(cardsReviewedDelta = 1, studyMinutesDelta = (reviewDurationSeconds / 60).coerceAtLeast(1))
    }

    suspend fun recordQuizCompletion(
        deckId: Long,
        deckTitle: String,
        totalQuestions: Int,
        correctAnswers: Int,
        durationSeconds: Int
    ) = withContext(Dispatchers.IO) {
        val scorePct = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        dao.insertQuizResult(
            QuizResultEntity(
                deckId = deckId,
                deckTitle = deckTitle,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                scorePercentage = scorePct,
                durationSeconds = durationSeconds
            )
        )

        incrementTodayStreak(quizzesCompletedDelta = 1, studyMinutesDelta = (durationSeconds / 60).coerceAtLeast(1))
    }

    suspend fun logQuickStudyActivity(cardsCount: Int) = withContext(Dispatchers.IO) {
        incrementTodayStreak(cardsReviewedDelta = cardsCount, studyMinutesDelta = (cardsCount * 2))
    }

    suspend fun incrementTodayStreak(
        cardsReviewedDelta: Int = 0,
        quizzesCompletedDelta: Int = 0,
        studyMinutesDelta: Int = 0
    ) {
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
            targetMet = newReviewed >= targetGoal
        )
        dao.insertOrUpdateStreak(streakEntity)
    }

    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    /**
     * Calculates consecutive day study streak count, taking Streak Shields into account.
     */
    suspend fun calculateCurrentStreakCount(availableShields: Int = 0): Int = withContext(Dispatchers.IO) {
        val streaks = dao.getAllDailyStreaks().first()
        val streakMap = streaks.associateBy { it.dateString }

        var count = 0
        var shieldsRemaining = availableShields
        val cal = Calendar.getInstance()

        val todayStr = getCurrentDateString()
        if (streakMap.containsKey(todayStr) && (streakMap[todayStr]?.cardsReviewed ?: 0) > 0) {
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
            if (entry != null && entry.cardsReviewed > 0) {
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
        count
    }

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
    }
}
