package com.widgy.lyrics.domain.model

data class DailyLyric(
    val date: String,
    val songId: Long,
    val snippetId: Long?,
    val text: String,
    val songTitle: String,
    val artistName: String
)
