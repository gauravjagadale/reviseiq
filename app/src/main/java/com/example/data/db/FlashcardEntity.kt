package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deckId"]), Index(value = ["nextReviewDate"])]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val hint: String = "",
    val boxLevel: Int = 1,
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val lastReviewed: Long? = null,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastRating: String = "",
    // --- sync fields (DB v5) ---
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)
