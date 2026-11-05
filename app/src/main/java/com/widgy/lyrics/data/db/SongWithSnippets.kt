package com.widgy.lyrics.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class SongWithSnippets(
    @Embedded val song: SongEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "songId"
    )
    val snippets: List<SnippetEntity>
)
