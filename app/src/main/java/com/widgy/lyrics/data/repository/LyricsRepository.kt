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
import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult
import com.widgy.lyrics.domain.model.playlist.PlaylistImportState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

class LyricsRepository(
    private val db: AppDatabase,
    private val client: LrcLibClient = LrcLibClient(),
    pinnedLyricProvider: (() -> DailyLyric?)? = null,
    private val rotationEngine: DailyRotationEngine = DailyRotationEngine(
        db.songDao(),
        db.snippetDao(),
        db.dailyHistoryDao(),
        pinnedLyricProvider = pinnedLyricProvider
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

        // Always remove existing snippets for this song before inserting,
        // so re-saving never creates duplicate entries, and clearing snippets works correctly.
        db.snippetDao().deleteAllSnippetsForSong(songId)
        if (snippets.isNotEmpty()) {
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

    fun importPlaylist(
        playlist: PlaylistExtractResult,
        delayMs: Long = 150L
    ): Flow<PlaylistImportState> = flow {
        val total = playlist.tracks.size
        if (total == 0) {
            emit(PlaylistImportState.Success(playlist.playlistTitle, 0, 0, 0))
            return@flow
        }

        var current = 0
        var importedCount = 0
        var skippedCount = 0
        val semaphore = Semaphore(2)

        try {
            for (query in playlist.tracks) {
                coroutineContext.ensureActive()

                emit(
                    PlaylistImportState.FetchingLyrics(
                        playlistTitle = playlist.playlistTitle,
                        current = current,
                        total = total,
                        currentTrack = query.trackName,
                        matchedCount = importedCount
                    )
                )

                // 1. Check local deduplication before making network calls
                val existing = db.songDao().findByTitleAndArtist(query.trackName, query.artistName)
                if (existing != null) {
                    skippedCount++
                    current++
                    continue
                }

                // 2. Fetch lyrics with rate limiting and search fallback
                var matchedDto: LrcTrackDto? = null
                semaphore.withPermit {
                    if (delayMs > 0) delay(delayMs)

                    // Primary exact lookup
                    var result = client.getLyrics(query.trackName, query.artistName)

                    // If 429, retry once after backoff
                    if (result.isFailure && result.exceptionOrNull()?.message?.contains("429") == true) {
                        delay(3000L)
                        result = client.getLyrics(query.trackName, query.artistName)
                    }

                    val primaryDto = result.getOrNull()
                    if (primaryDto != null && (!primaryDto.plainLyrics.isNullOrBlank() || !primaryDto.syncedLyrics.isNullOrBlank())) {
                        matchedDto = primaryDto
                    } else {
                        // Secondary search fallback
                        val searchResult = client.searchTracks("${query.artistName} ${query.trackName}")
                        val candidates = searchResult.getOrNull().orEmpty()
                        matchedDto = candidates.firstOrNull {
                            !it.plainLyrics.isNullOrBlank() || !it.syncedLyrics.isNullOrBlank()
                        }
                    }
                }

                // 3. Insert into database if lyrics found and not already inserted
                if (matchedDto != null) {
                    val lyricsText = matchedDto?.plainLyrics?.ifBlank { matchedDto?.syncedLyrics.orEmpty() }
                        ?: matchedDto?.syncedLyrics.orEmpty()

                    if (lyricsText.isNotBlank()) {
                        val finalTrackTitle = matchedDto!!.displayTitle.ifBlank { query.trackName }
                        val finalArtistName = matchedDto!!.artistName?.ifBlank { query.artistName } ?: query.artistName
                        val lrclibId = matchedDto!!.id

                        val alreadyInDb = (lrclibId != null && db.songDao().findByLrclibId(lrclibId) != null) ||
                                db.songDao().findByTitleAndArtist(finalTrackTitle, finalArtistName) != null

                        if (!alreadyInDb) {
                            db.songDao().insertSong(
                                SongEntity(
                                    title = finalTrackTitle,
                                    artist = finalArtistName,
                                    album = matchedDto!!.albumName ?: query.albumName,
                                    plainLyrics = lyricsText,
                                    lrclibId = lrclibId,
                                    isRandomPool = true
                                )
                            )
                            importedCount++
                        } else {
                            skippedCount++
                        }
                    } else {
                        skippedCount++
                    }
                } else {
                    skippedCount++
                }

                current++
            }

            emit(
                PlaylistImportState.Success(
                    playlistTitle = playlist.playlistTitle,
                    totalTracks = total,
                    importedCount = importedCount,
                    skippedCount = skippedCount
                )
            )
        } catch (e: CancellationException) {
            emit(
                PlaylistImportState.Cancelled(
                    playlistTitle = playlist.playlistTitle,
                    importedCount = importedCount
                )
            )
            throw e
        } catch (e: Exception) {
            emit(PlaylistImportState.Error(e.message ?: "Failed to import playlist"))
        }
    }

    suspend fun getTodayDailyLyric(forceRefresh: Boolean = false, intervalHours: Int = 24): DailyLyric? {
        return rotationEngine.resolveDailyLyric(LocalDate.now(), forceRefresh, intervalHours)
    }

    fun observeTodayLyric(date: LocalDate = LocalDate.now()): Flow<DailyHistoryEntity?> {
        return db.dailyHistoryDao().observeDailyLyric(date.toString())
    }

    fun observeDailyHistory(): Flow<List<DailyHistoryEntity>> {
        return db.dailyHistoryDao().getAllHistory()
    }

    suspend fun refreshTodayLyric(intervalHours: Int = 24): DailyLyric? {
        return rotationEngine.resolveDailyLyric(LocalDate.now(), forceRefresh = true, intervalHours = intervalHours)
    }
}
