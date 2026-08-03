package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val colorHex: String = "#6366F1",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "folderId", defaultValue = "0")
    val folderId: Long = 0,
    // --- sync fields (DB v5) ---
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)
