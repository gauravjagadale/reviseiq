package com.example.data.sync

import android.content.Context
import com.example.data.db.DailyStreakEntity
import com.example.data.db.DeckEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.FolderEntity
import com.example.data.db.QuizResultEntity
import com.example.data.db.ReviseDao
import com.example.data.db.StudyLogEntity
import com.example.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

sealed interface SyncResult {
    data object NotLoggedIn : SyncResult
    data object Success : SyncResult
    data class Failure(val message: String?) : SyncResult
}

/**
 * Offline-first sync engine. Pulls remote rows changed since the last pull,
 * merges them with last-write-wins semantics, then pushes local rows changed
 * since the last push. Soft-delete flags act as tombstones so deletions
 * propagate.
 *
 * All cross-row references use UUIDs: parent lookups go through the whole
 * local dataset (uuid -> local id maps), never through numeric remote ids,
 * so rows referencing parents that were not re-pulled in this window still
 * attach correctly and per-device id collisions cannot mis-assign them.
 */
class SyncManager(
    private val dao: ReviseDao,
    private val context: Context,
    private val readSettings: () -> JsonObject,
    private val applySettings: (JsonObject) -> Unit
) {

    private val syncPrefs = context.getSharedPreferences("reviseiq_sync", Context.MODE_PRIVATE)

    // Sync watermarks are keyed per user id so switching accounts on the same
    // device never skips pulling the other account's history.
    private fun lastPushAt(userId: String): Long = syncPrefs.getLong("last_push_at_$userId", 0L)
    private fun setLastPushAt(userId: String, value: Long) =
        syncPrefs.edit().putLong("last_push_at_$userId", value).apply()

    private fun lastPullAt(userId: String): Long = syncPrefs.getLong("last_pull_at_$userId", 0L)
    private fun setLastPullAt(userId: String, value: Long) =
        syncPrefs.edit().putLong("last_pull_at_$userId", value).apply()

    private fun lastPushedSettingsPayload(userId: String): String =
        syncPrefs.getString("last_pushed_settings_payload_$userId", "") ?: ""
    private fun setLastPushedSettingsPayload(userId: String, value: String) =
        syncPrefs.edit().putString("last_pushed_settings_payload_$userId", value).apply()

    private fun settingsAppliedAt(userId: String): Long =
        syncPrefs.getLong("settings_applied_at_$userId", 0L)
    private fun setSettingsAppliedAt(userId: String, value: Long) =
        syncPrefs.edit().putLong("settings_applied_at_$userId", value).apply()

    fun lastSyncTimeMillis(): Long = syncPrefs.getLong("last_sync_time", 0L)

    fun recordLocalSyncTime(timestamp: Long) {
        syncPrefs.edit().putLong("last_sync_time", timestamp).apply()
    }

    /** Forget all progress markers for a user so the next sync is a full one. */
    fun resetWatermark(userId: String) {
        syncPrefs.edit()
            .putLong("last_push_at_$userId", 0L)
            .putLong("last_pull_at_$userId", 0L)
            .putLong("settings_applied_at_$userId", 0L)
            .remove("last_pushed_settings_payload_$userId")
            .apply()
    }

    private val isLoggedIn: Boolean
        get() = try {
            SupabaseProvider.client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        if (!isLoggedIn) return@withContext SyncResult.NotLoggedIn
        try {
            val client = SupabaseProvider.client
            val session = client.auth.currentSessionOrNull() ?: return@withContext SyncResult.NotLoggedIn
            val userId = session.user?.id ?: return@withContext SyncResult.NotLoggedIn
            val syncStart = System.currentTimeMillis()

            val sincePull = lastPullAt(userId)
            // 1. Pull parent rows first so child references can be remapped.
            val remoteFolders = pullFolders(client, sincePull)
            val remoteDecks = pullDecks(client, sincePull)
            val remoteCards = pullCards(client, sincePull)
            val remoteLogs = pullLogs(client, sincePull)
            val remoteQuizzes = pullQuizzes(client, sincePull)
            val remoteStreaks = pullStreaks(client, sincePull)

            // 2. Merge locally (folders before decks, decks before cards).
            mergeFolders(remoteFolders)
            mergeDecks(remoteDecks)
            mergeCards(remoteCards)
            mergeLogs(remoteLogs)
            mergeQuizzes(remoteQuizzes)
            mergeStreaks(remoteStreaks)

            // 3. Settings blob (small enough to always exchange, LWW on updated_at).
            mergeSettings(client, userId)

            val sincePush = lastPushAt(userId)
            // 4. Push local changes (idempotent upserts, LWW on the cloud side).
            pushFolders(client, userId, dao.getFoldersChangedSince(sincePush))
            pushDecks(client, userId, dao.getDecksChangedSince(sincePush))
            pushCards(client, userId, dao.getCardsChangedSince(sincePush))
            pushLogs(client, userId, dao.getLogsChangedSince(sincePush))
            pushQuizzes(client, userId, dao.getQuizzesChangedSince(sincePush))
            pushStreaks(client, userId, dao.getStreaksChangedSince(sincePush))
            pushSettings(client, userId)

            // 5. Commit progress markers so nothing is re-pulled/re-pushed.
            setLastPullAt(userId, syncStart)
            setLastPushAt(userId, syncStart)
            recordLocalSyncTime(System.currentTimeMillis())
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Failure(e.message)
        }
    }

    // --- Pull (cloud -> local) ---

    private suspend fun pullDecks(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteDeck> =
        client.postgrest.from("decks").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteDeck>()

    private suspend fun pullFolders(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteFolder> =
        client.postgrest.from("folders").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteFolder>()

    private suspend fun pullCards(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteFlashcard> =
        client.postgrest.from("flashcards").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteFlashcard>()

    private suspend fun pullLogs(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteStudyLog> =
        client.postgrest.from("study_logs").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteStudyLog>()

    private suspend fun pullQuizzes(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteQuizResult> =
        client.postgrest.from("quiz_results").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteQuizResult>()

    private suspend fun pullStreaks(client: io.github.jan.supabase.SupabaseClient, since: Long): List<RemoteStreak> =
        client.postgrest.from("daily_streaks").select {
            filter { gte("updated_at", since) }
        }.decodeList<RemoteStreak>()

    // --- Merge (last-write-wins on the local side) ---

    /**
     * uuid -> local id for every row currently in the local DB (including
     * soft-deleted ones). Built once per sync so child rows referencing a
     * parent that was not re-pulled in this window still resolve correctly.
     */
    private suspend fun folderUuidToId(): Map<String, Long> =
        dao.getAllFoldersList().associate { it.uuid to it.id }

    private suspend fun deckUuidToId(): Map<String, Long> =
        dao.getAllDecksList().associate { it.uuid to it.id }

    private suspend fun cardUuidToId(): Map<String, Long> =
        dao.getAllCardsList().associate { it.uuid to it.id }

    private suspend fun mergeFolders(remote: List<RemoteFolder>) {
        for (r in remote) {
            val local = dao.getFolderByUuid(r.uuid)
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertFolder(
                    FolderEntity(
                        name = r.name,
                        colorHex = r.colorHex,
                        createdAt = r.createdAt,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.updateFolder(
                    local.copy(
                        name = r.name,
                        colorHex = r.colorHex,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeDecks(remote: List<RemoteDeck>) {
        val folderIds = folderUuidToId()
        for (r in remote) {
            val local = dao.getDeckByUuid(r.uuid)
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertDeck(
                    DeckEntity(
                        title = r.title,
                        description = r.description,
                        category = r.category,
                        colorHex = r.colorHex,
                        createdAt = r.createdAt,
                        folderId = folderIds[r.folderUuid] ?: 0,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.updateDeck(
                    local.copy(
                        title = r.title,
                        description = r.description,
                        category = r.category,
                        colorHex = r.colorHex,
                        folderId = folderIds[r.folderUuid] ?: local.folderId,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeCards(remote: List<RemoteFlashcard>) {
        val deckIds = deckUuidToId()
        for (r in remote) {
            val localDeckId = deckIds[r.deckUuid] ?: continue
            val local = dao.getCardByUuid(r.uuid)
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertCard(
                    FlashcardEntity(
                        deckId = localDeckId,
                        front = r.front,
                        back = r.back,
                        hint = r.hint,
                        boxLevel = r.boxLevel,
                        intervalDays = r.intervalDays,
                        repetitions = r.repetitions,
                        easeFactor = r.easeFactor.toFloat(),
                        lastReviewed = r.lastReviewed,
                        nextReviewDate = r.nextReviewDate,
                        lastRating = r.lastRating,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.updateCard(
                    local.copy(
                        deckId = localDeckId,
                        front = r.front,
                        back = r.back,
                        hint = r.hint,
                        boxLevel = r.boxLevel,
                        intervalDays = r.intervalDays,
                        repetitions = r.repetitions,
                        easeFactor = r.easeFactor.toFloat(),
                        lastReviewed = r.lastReviewed,
                        nextReviewDate = r.nextReviewDate,
                        lastRating = r.lastRating,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeLogs(remote: List<RemoteStudyLog>) {
        val deckIds = deckUuidToId()
        val cardIds = cardUuidToId()
        for (r in remote) {
            val local = dao.getLogByUuid(r.uuid)
            // Logs are analytics; unresolved refs degrade to 0 instead of
            // being dropped. Never overwrite good existing refs with 0.
            val resolvedDeckId = deckIds[r.deckUuid] ?: local?.deckId ?: 0
            val resolvedCardId = cardIds[r.cardUuid] ?: local?.cardId ?: 0
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertStudyLog(
                    StudyLogEntity(
                        cardId = resolvedCardId,
                        deckId = resolvedDeckId,
                        timestamp = r.timestamp,
                        rating = r.rating,
                        reviewDurationSeconds = r.reviewDurationSeconds,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.insertStudyLog(
                    local.copy(
                        cardId = resolvedCardId,
                        deckId = resolvedDeckId,
                        timestamp = r.timestamp,
                        rating = r.rating,
                        reviewDurationSeconds = r.reviewDurationSeconds,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeQuizzes(remote: List<RemoteQuizResult>) {
        val deckIds = deckUuidToId()
        for (r in remote) {
            val local = dao.getQuizByUuid(r.uuid)
            val resolvedDeckId = deckIds[r.deckUuid] ?: local?.deckId ?: 0
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertQuizResult(
                    QuizResultEntity(
                        deckId = resolvedDeckId,
                        deckTitle = r.deckTitle,
                        totalQuestions = r.totalQuestions,
                        correctAnswers = r.correctAnswers,
                        scorePercentage = r.scorePercentage,
                        durationSeconds = r.durationSeconds,
                        timestamp = r.timestamp,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.insertQuizResult(
                    local.copy(
                        deckId = resolvedDeckId,
                        deckTitle = r.deckTitle,
                        totalQuestions = r.totalQuestions,
                        correctAnswers = r.correctAnswers,
                        scorePercentage = r.scorePercentage,
                        durationSeconds = r.durationSeconds,
                        timestamp = r.timestamp,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeStreaks(remote: List<RemoteStreak>) {
        for (r in remote) {
            val local = dao.getStreakByUuid(r.uuid) ?: dao.getStreakForDate(r.dateString)
            if (local == null) {
                if (r.isDeleted) continue
                dao.insertOrUpdateStreak(
                    DailyStreakEntity(
                        dateString = r.dateString,
                        cardsReviewed = r.cardsReviewed,
                        quizzesCompleted = r.quizzesCompleted,
                        studyDurationMinutes = r.studyDurationMinutes,
                        goalTargetCards = r.goalTargetCards,
                        targetMet = r.targetMet,
                        uuid = r.uuid,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = false
                    )
                )
            } else if (r.updatedAt > local.updatedAtMillis) {
                dao.insertOrUpdateStreak(
                    local.copy(
                        cardsReviewed = r.cardsReviewed,
                        quizzesCompleted = r.quizzesCompleted,
                        studyDurationMinutes = r.studyDurationMinutes,
                        goalTargetCards = r.goalTargetCards,
                        targetMet = r.targetMet,
                        updatedAtMillis = r.updatedAt,
                        isDeleted = r.isDeleted
                    )
                )
            }
        }
    }

    private suspend fun mergeSettings(client: io.github.jan.supabase.SupabaseClient, userId: String) {
        val remote = client.postgrest.from("user_settings").select {
            filter { gte("updated_at", settingsAppliedAt(userId)) }
        }.decodeList<RemoteSettings>().firstOrNull() ?: return
        if (remote.payload.isEmpty()) return
        // LWW: only newer cloud blobs are applied, so a device's newer local
        // preference edit is never clobbered by an older remote payload.
        if (remote.updatedAt <= settingsAppliedAt(userId)) return
        applySettings(remote.payload)
        setSettingsAppliedAt(userId, remote.updatedAt)
    }

    // --- Push (local -> cloud) ---

    /** local id -> uuid for every row currently in the local DB. */
    private suspend fun folderIdToUuid(): Map<Long, String> =
        dao.getAllFoldersList().associate { it.id to it.uuid }

    private suspend fun deckIdToUuid(): Map<Long, String> =
        dao.getAllDecksList().associate { it.id to it.uuid }

    private suspend fun cardIdToUuid(): Map<Long, String> =
        dao.getAllCardsList().associate { it.id to it.uuid }

    private suspend fun pushDecks(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<DeckEntity>) {
        if (rows.isEmpty()) return
        val folderUuids = folderIdToUuid()
        client.postgrest.from("decks").upsert(
            rows.map {
                RemoteDeck(
                    userId = userId,
                    uuid = it.uuid,
                    title = it.title,
                    description = it.description,
                    category = it.category,
                    colorHex = it.colorHex,
                    createdAt = it.createdAt,
                    folderUuid = folderUuids[it.folderId] ?: "",
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushFolders(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<FolderEntity>) {
        if (rows.isEmpty()) return
        client.postgrest.from("folders").upsert(
            rows.map {
                RemoteFolder(
                    userId = userId,
                    uuid = it.uuid,
                    name = it.name,
                    colorHex = it.colorHex,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushCards(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<FlashcardEntity>) {
        if (rows.isEmpty()) return
        val deckUuids = deckIdToUuid()
        client.postgrest.from("flashcards").upsert(
            rows.map {
                RemoteFlashcard(
                    userId = userId,
                    uuid = it.uuid,
                    deckUuid = deckUuids[it.deckId] ?: "",
                    front = it.front,
                    back = it.back,
                    hint = it.hint,
                    boxLevel = it.boxLevel,
                    intervalDays = it.intervalDays,
                    repetitions = it.repetitions,
                    easeFactor = it.easeFactor.toDouble(),
                    lastReviewed = it.lastReviewed,
                    nextReviewDate = it.nextReviewDate,
                    lastRating = it.lastRating,
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushLogs(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<StudyLogEntity>) {
        if (rows.isEmpty()) return
        val deckUuids = deckIdToUuid()
        val cardUuids = cardIdToUuid()
        client.postgrest.from("study_logs").upsert(
            rows.map {
                RemoteStudyLog(
                    userId = userId,
                    uuid = it.uuid,
                    cardUuid = cardUuids[it.cardId] ?: "",
                    deckUuid = deckUuids[it.deckId] ?: "",
                    timestamp = it.timestamp,
                    rating = it.rating,
                    reviewDurationSeconds = it.reviewDurationSeconds,
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushQuizzes(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<QuizResultEntity>) {
        if (rows.isEmpty()) return
        val deckUuids = deckIdToUuid()
        client.postgrest.from("quiz_results").upsert(
            rows.map {
                RemoteQuizResult(
                    userId = userId,
                    uuid = it.uuid,
                    deckUuid = deckUuids[it.deckId] ?: "",
                    deckTitle = it.deckTitle,
                    totalQuestions = it.totalQuestions,
                    correctAnswers = it.correctAnswers,
                    scorePercentage = it.scorePercentage,
                    durationSeconds = it.durationSeconds,
                    timestamp = it.timestamp,
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushStreaks(client: io.github.jan.supabase.SupabaseClient, userId: String, rows: List<DailyStreakEntity>) {
        if (rows.isEmpty()) return
        client.postgrest.from("daily_streaks").upsert(
            rows.map {
                RemoteStreak(
                    userId = userId,
                    uuid = it.uuid,
                    dateString = it.dateString,
                    cardsReviewed = it.cardsReviewed,
                    quizzesCompleted = it.quizzesCompleted,
                    studyDurationMinutes = it.studyDurationMinutes,
                    goalTargetCards = it.goalTargetCards,
                    targetMet = it.targetMet,
                    updatedAt = it.updatedAtMillis,
                    isDeleted = it.isDeleted
                )
            }
        ) { onConflict = "user_id,uuid" }
    }

    private suspend fun pushSettings(client: io.github.jan.supabase.SupabaseClient, userId: String) {
        val payload = readSettings()
        val payloadString = payload.toString()
        if (payloadString == lastPushedSettingsPayload(userId) && payload.isNotEmpty()) return
        client.postgrest.from("user_settings").upsert(
            RemoteSettings(
                userId = userId,
                payload = payload,
                updatedAt = System.currentTimeMillis()
            )
        ) { onConflict = "user_id" }
        setLastPushedSettingsPayload(userId, payloadString)
    }
}
