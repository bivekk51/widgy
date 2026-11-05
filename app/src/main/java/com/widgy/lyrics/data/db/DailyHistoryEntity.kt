package com.widgy.lyrics.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_history")
data class DailyHistoryEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val songId: Long,
    val snippetId: Long? = null,
    val lyricText: String,
    val songTitle: String,
    val artistName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
