package com.example.data.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// DTOs mirroring the Supabase cloud tables. References between cloud rows use
// UUIDs (folder_uuid / deck_uuid / card_uuid) instead of numeric ids because
// numeric ids are per-device autoincrements that collide across devices.
// user_id is echoed back by PostgREST and sent on upsert so the composite key
// (user_id, uuid) is honored.
@Serializable
data class RemoteFolder(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    val name: String,
    val colorHex: String = "#6366F1",
    val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteDeck(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    val title: String,
    val description: String = "",
    val category: String = "",
    val colorHex: String = "#6366F1",
    val createdAt: Long = 0,
    @SerialName("folder_uuid") val folderUuid: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteFlashcard(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    @SerialName("deck_uuid") val deckUuid: String = "",
    val front: String,
    val back: String,
    val hint: String = "",
    val boxLevel: Int = 1,
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val lastReviewed: Long? = null,
    val nextReviewDate: Long = 0,
    val lastRating: String = "",
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteStudyLog(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    @SerialName("card_uuid") val cardUuid: String = "",
    @SerialName("deck_uuid") val deckUuid: String = "",
    val timestamp: Long = 0,
    val rating: String = "",
    val reviewDurationSeconds: Int = 5,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteStreak(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    val dateString: String,
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val studyDurationMinutes: Int = 0,
    val goalTargetCards: Int = 20,
    val targetMet: Boolean = false,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteQuizResult(
    @SerialName("user_id") val userId: String = "",
    val uuid: String,
    @SerialName("deck_uuid") val deckUuid: String = "",
    val deckTitle: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val scorePercentage: Int = 0,
    val durationSeconds: Int = 0,
    val timestamp: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class RemoteSettings(
    @SerialName("user_id") val userId: String = "",
    val payload: JsonObject = JsonObject(emptyMap()),
    @SerialName("updated_at") val updatedAt: Long = 0
)
