package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_streaks")
data class DailyStreakEntity(
    @PrimaryKey val dateString: String, // e.g. "2026-08-01"
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val studyDurationMinutes: Int = 0,
    val goalTargetCards: Int = 20,
    val targetMet: Boolean = false,
    // --- sync fields (DB v5) ---
    // Deterministic uuid derived from the date so every device produces the
    // same identity for the same day (merge key is dateString, not uuid).
    @ColumnInfo(defaultValue = "''")
    val uuid: String = "streak-$dateString",
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)
