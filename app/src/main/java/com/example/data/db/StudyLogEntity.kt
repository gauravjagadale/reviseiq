package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: String, // "AGAIN", "HARD", "GOOD", "EASY"
    val reviewDurationSeconds: Int = 5
)
