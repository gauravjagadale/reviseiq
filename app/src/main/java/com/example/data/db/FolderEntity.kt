package com.example.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6366F1",
    val createdAt: Long = System.currentTimeMillis(),
    // --- sync fields (DB v5) ---
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtMillis: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false
)
