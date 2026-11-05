package com.widgy.lyrics.domain.model

import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongEntity

sealed interface Candidate {
    data class Curated(
        val snippet: SnippetEntity,
        val song: SongEntity
    ) : Candidate

    data class RandomSong(
        val song: SongEntity
    ) : Candidate
}
