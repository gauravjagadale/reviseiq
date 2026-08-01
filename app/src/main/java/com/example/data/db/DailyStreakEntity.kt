package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_streaks")
data class DailyStreakEntity(
    @PrimaryKey val dateString: String, // e.g. "2026-08-01"
    val cardsReviewed: Int = 0,
    val quizzesCompleted: Int = 0,
    val studyDurationMinutes: Int = 0,
    val goalTargetCards: Int = 20,
    val targetMet: Boolean = false
)
