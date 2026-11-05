package com.widgy.lyrics.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lrclibId: Long? = null,
    val title: String,
    val artist: String,
    val album: String? = null,
    val plainLyrics: String,
    val isRandomPool: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
