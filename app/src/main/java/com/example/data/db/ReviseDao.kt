package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviseDao {
    // --- Decks ---
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    // --- Flashcards ---
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsForDeckList(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards ORDER BY id ASC")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :timestamp ORDER BY nextReviewDate ASC")
    fun getDueCards(timestamp: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :timestamp ORDER BY nextReviewDate ASC")
    suspend fun getDueCardsList(timestamp: Long): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    // --- Study Logs ---
    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun getAllStudyLogs(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE timestamp >= :fromTimestamp ORDER BY timestamp ASC")
    fun getStudyLogsSince(fromTimestamp: Long): Flow<List<StudyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLogEntity): Long

    // --- Daily Streaks ---
    @Query("SELECT * FROM daily_streaks ORDER BY dateString DESC")
    fun getAllDailyStreaks(): Flow<List<DailyStreakEntity>>

    @Query("SELECT * FROM daily_streaks WHERE dateString = :dateString")
    suspend fun getStreakForDate(dateString: String): DailyStreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: DailyStreakEntity)

    // --- Quiz Results ---
    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllQuizResults(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity): Long

    // --- Reset / Clear Data ---
    @Query("DELETE FROM decks")
    suspend fun deleteAllDecks()

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllCards()

    @Query("DELETE FROM study_logs")
    suspend fun deleteAllStudyLogs()

    @Query("DELETE FROM daily_streaks")
    suspend fun deleteAllDailyStreaks()

    @Query("DELETE FROM quiz_results")
    suspend fun deleteAllQuizResults()
}
