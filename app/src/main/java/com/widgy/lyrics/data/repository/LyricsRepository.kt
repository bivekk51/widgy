package com.widgy.lyrics.data.repository

import com.widgy.lyrics.data.api.LrcLibClient
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.db.DailyHistoryEntity
import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongEntity
import com.widgy.lyrics.data.db.SongWithSnippets
import com.widgy.lyrics.data.model.LrcTrackDto
import com.widgy.lyrics.domain.engine.DailyRotationEngine
import com.widgy.lyrics.domain.model.DailyLyric
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class LyricsRepository(
    private val db: AppDatabase,
    private val client: LrcLibClient = LrcLibClient(),
    private val rotationEngine: DailyRotationEngine = DailyRotationEngine(
        db.songDao(),
        db.snippetDao(),
        db.dailyHistoryDao()
    )
) {
    suspend fun searchOnline(query: String): Result<List<LrcTrackDto>> {
        return client.searchTracks(query)
    }

    suspend fun fetchLyricsOnline(trackName: String, artistName: String): Result<LrcTrackDto> {
        return client.getLyrics(trackName, artistName)
    }

    fun observeAllSongsWithSnippets(): Flow<List<SongWithSnippets>> {
        return db.songDao().getAllSongsWithSnippets()
    }

    suspend fun getSongById(id: Long): SongEntity? {
        return db.songDao().getSongById(id)
    }

    suspend fun saveSongWithSnippets(
        title: String,
        artist: String,
        album: String?,
        plainLyrics: String,
        lrclibId: Long?,
        snippets: List<String>,
        isRandomPool: Boolean,
        existingSongId: Long? = null
    ): Long {
        val songId = if (existingSongId != null && existingSongId > 0) {
            val existing = db.songDao().getSongById(existingSongId)
            if (existing != null) {
                db.songDao().updateSong(
                    existing.copy(
                        title = title,
                        artist = artist,
                        album = album,
                        plainLyrics = plainLyrics,
                        isRandomPool = isRandomPool
                    )
                )
                existingSongId
            } else {
                insertNewSong(title, artist, album, plainLyrics, lrclibId, isRandomPool)
            }
        } else {
            insertNewSong(title, artist, album, plainLyrics, lrclibId, isRandomPool)
        }

        if (snippets.isNotEmpty()) {
            // Delete existing snippets for this song before inserting new ones
            // so re-saving doesn't create duplicate entries.
            db.snippetDao().deleteAllSnippetsForSong(songId)
            val entities = snippets.map {
                SnippetEntity(
                    songId = songId,
                    content = it,
                    isActive = true
                )
            }
            db.snippetDao().insertSnippets(entities)
        }
        return songId
    }

    private suspend fun insertNewSong(
        title: String,
        artist: String,
        album: String?,
        plainLyrics: String,
        lrclibId: Long?,
        isRandomPool: Boolean
    ): Long {
        val song = SongEntity(
            title = title,
            artist = artist,
            album = album,
            plainLyrics = plainLyrics,
            lrclibId = lrclibId,
            isRandomPool = isRandomPool
        )
        return db.songDao().insertSong(song)
    }

    suspend fun setSnippetActive(snippetId: Long, isActive: Boolean) {
        db.snippetDao().updateSnippetActive(snippetId, isActive)
    }

    suspend fun deleteSong(songId: Long) {
        db.songDao().deleteSongById(songId)
    }

    suspend fun deleteSnippet(snippetId: Long) {
        db.snippetDao().deleteSnippetById(snippetId)
    }

    suspend fun getTodayDailyLyric(forceRefresh: Boolean = false): DailyLyric? {
        return rotationEngine.resolveDailyLyric(LocalDate.now(), forceRefresh)
    }

    fun observeTodayLyric(date: LocalDate = LocalDate.now()): Flow<DailyHistoryEntity?> {
        return db.dailyHistoryDao().observeDailyLyric(date.toString())
    }

    suspend fun refreshTodayLyric(): DailyLyric? {
        return rotationEngine.resolveDailyLyric(LocalDate.now(), forceRefresh = true)
    }
}
