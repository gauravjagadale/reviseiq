package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "study_logs", indices = [Index(value = ["timestamp"])])
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: String, // "AGAIN", "HARD", "GOOD", "EASY"
    val reviewDurationSeconds: Int = 5,
    // --- sync fields (DB v5) ---
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)
