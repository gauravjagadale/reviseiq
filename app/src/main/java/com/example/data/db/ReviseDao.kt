package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviseDao {
    // --- Decks ---
    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId AND isDeleted = 0")
    suspend fun getDeckById(deckId: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE uuid = :uuid")
    suspend fun getDeckByUuid(uuid: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE updatedAtMillis > :timestamp")
    suspend fun getDecksChangedSince(timestamp: Long): List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Upsert
    suspend fun upsertDecks(decks: List<DeckEntity>)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("UPDATE decks SET isDeleted = 1, updatedAtMillis = :timestamp WHERE id = :deckId")
    suspend fun softDeleteDeck(deckId: Long, timestamp: Long)

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAtMillis = :timestamp WHERE deckId = :deckId")
    suspend fun softDeleteCardsForDeck(deckId: Long, timestamp: Long)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: Long)

    // --- Flashcards ---
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isDeleted = 0 ORDER BY id ASC")
    suspend fun getCardsForDeckList(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE uuid = :uuid")
    suspend fun getCardByUuid(uuid: String): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE updatedAtMillis > :timestamp")
    suspend fun getCardsChangedSince(timestamp: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :timestamp AND isDeleted = 0 ORDER BY nextReviewDate ASC")
    fun getDueCards(timestamp: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :timestamp AND isDeleted = 0 ORDER BY nextReviewDate ASC")
    suspend fun getDueCardsList(timestamp: Long): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Upsert
    suspend fun upsertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAtMillis = :timestamp WHERE id = :cardId")
    suspend fun softDeleteCardById(cardId: Long, timestamp: Long)

    @Query("DELETE FROM flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    // --- Study Logs ---
    @Query("SELECT * FROM study_logs WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllStudyLogs(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE timestamp >= :fromTimestamp AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getStudyLogsSince(fromTimestamp: Long): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs WHERE uuid = :uuid")
    suspend fun getLogByUuid(uuid: String): StudyLogEntity?

    @Query("SELECT * FROM study_logs WHERE updatedAtMillis > :timestamp")
    suspend fun getLogsChangedSince(timestamp: Long): List<StudyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLogEntity): Long

    @Upsert
    suspend fun upsertStudyLogs(logs: List<StudyLogEntity>)

    // --- Daily Streaks ---
    @Query("SELECT * FROM daily_streaks WHERE isDeleted = 0 ORDER BY dateString DESC")
    fun getAllDailyStreaks(): Flow<List<DailyStreakEntity>>

    @Query("SELECT * FROM daily_streaks WHERE dateString = :dateString AND isDeleted = 0")
    suspend fun getStreakForDate(dateString: String): DailyStreakEntity?

    @Query("SELECT * FROM daily_streaks WHERE uuid = :uuid")
    suspend fun getStreakByUuid(uuid: String): DailyStreakEntity?

    @Query("SELECT * FROM daily_streaks WHERE updatedAtMillis > :timestamp")
    suspend fun getStreaksChangedSince(timestamp: Long): List<DailyStreakEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: DailyStreakEntity)

    @Upsert
    suspend fun upsertStreaks(streaks: List<DailyStreakEntity>)

    // --- Quiz Results ---
    @Query("SELECT * FROM quiz_results WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllQuizResults(): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results WHERE uuid = :uuid")
    suspend fun getQuizByUuid(uuid: String): QuizResultEntity?

    @Query("SELECT * FROM quiz_results WHERE updatedAtMillis > :timestamp")
    suspend fun getQuizzesChangedSince(timestamp: Long): List<QuizResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity): Long

    @Upsert
    suspend fun upsertQuizResults(results: List<QuizResultEntity>)

    // --- Folders ---
    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE uuid = :uuid")
    suspend fun getFolderByUuid(uuid: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE updatedAtMillis > :timestamp")
    suspend fun getFoldersChangedSince(timestamp: Long): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Upsert
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, updatedAtMillis = :timestamp WHERE id = :folderId")
    suspend fun softDeleteFolder(folderId: Long, timestamp: Long)

    @Query("UPDATE decks SET isDeleted = 1, updatedAtMillis = :timestamp WHERE folderId = :folderId")
    suspend fun softDeleteDecksInFolder(folderId: Long, timestamp: Long)

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAtMillis = :timestamp WHERE deckId IN (SELECT id FROM decks WHERE folderId = :folderId)")
    suspend fun softDeleteCardsInFolder(folderId: Long, timestamp: Long)

    @Query("UPDATE decks SET folderId = 0, updatedAtMillis = :timestamp WHERE folderId = :folderId")
    suspend fun clearFolderFromDecks(folderId: Long, timestamp: Long)

    @Query("DELETE FROM decks WHERE folderId = :folderId")
    suspend fun deleteDecksInFolder(folderId: Long)

    @Query("DELETE FROM flashcards WHERE deckId IN (SELECT id FROM decks WHERE folderId = :folderId)")
    suspend fun deleteCardsInFolder(folderId: Long)

    // --- Export Snapshot (single-shot reads, fresh data) ---
    @Query("SELECT * FROM decks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllDecksList(): List<DeckEntity>

    @Query("SELECT * FROM flashcards WHERE deckId IN (:deckIds) AND isDeleted = 0 ORDER BY id ASC")
    suspend fun getCardsForDeckIds(deckIds: List<Long>): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards ORDER BY id ASC")
    suspend fun getAllCardsList(): List<FlashcardEntity>

    @Query("SELECT * FROM daily_streaks WHERE isDeleted = 0 ORDER BY dateString DESC")
    suspend fun getAllStreaksList(): List<DailyStreakEntity>

    @Query("SELECT * FROM quiz_results WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getQuizResultsList(): List<QuizResultEntity>

    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getAllFoldersList(): List<FolderEntity>

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

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    // Tombstone everything for a table so the deletion propagates via sync
    // (rather than hard-deleting, which would let other devices resurrect it).
    @Query("UPDATE decks SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllDecks(timestamp: Long)

    @Query("UPDATE flashcards SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllCards(timestamp: Long)

    @Query("UPDATE study_logs SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllStudyLogs(timestamp: Long)

    @Query("UPDATE daily_streaks SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllDailyStreaks(timestamp: Long)

    @Query("UPDATE quiz_results SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllQuizResults(timestamp: Long)

    @Query("UPDATE folders SET isDeleted = 1, updatedAtMillis = :timestamp")
    suspend fun softDeleteAllFolders(timestamp: Long)
}
