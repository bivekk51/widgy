package com.widgy.lyrics.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.widgy.lyrics.data.api.LrcLibClient
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongEntity
import com.widgy.lyrics.data.model.LrcTrackDto
import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult
import com.widgy.lyrics.domain.model.playlist.PlaylistImportState
import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import com.widgy.lyrics.domain.model.playlist.TrackImportQuery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LyricsRepositoryPlaylistTest {

    private lateinit var db: AppDatabase
    private lateinit var mockClient: LrcLibClient
    private lateinit var repository: LyricsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mockClient = mockk()
        repository = LyricsRepository(db, mockClient)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun importPlaylist_fetchesLyricsAndInsertsAsRandomPool() = runBlocking {
        val playlist = PlaylistExtractResult(
            playlistTitle = "Chill Beats",
            platform = PlaylistPlatform.SPOTIFY,
            tracks = listOf(
                TrackImportQuery("Die With A Smile", "Lady Gaga"),
                TrackImportQuery("Daydreaming", "Radiohead")
            )
        )

        coEvery { mockClient.getLyrics("Die With A Smile", "Lady Gaga") } returns Result.success(
            LrcTrackDto(
                id = 101L,
                trackName = "Die With A Smile",
                artistName = "Lady Gaga",
                plainLyrics = "If the world was ending, I'd wanna be next to you"
            )
        )

        coEvery { mockClient.getLyrics("Daydreaming", "Radiohead") } returns Result.success(
            LrcTrackDto(
                id = 102L,
                trackName = "Daydreaming",
                artistName = "Radiohead",
                plainLyrics = "Dreamers, they never learn"
            )
        )

        val states = repository.importPlaylist(playlist).toList()

        val lastState = states.last()
        assertTrue(lastState is PlaylistImportState.Success)
        val success = lastState as PlaylistImportState.Success
        assertEquals(2, success.totalTracks)
        assertEquals(2, success.importedCount)
        assertEquals(0, success.skippedCount)

        val randomPool = db.songDao().getRandomPoolSongs()
        assertEquals(2, randomPool.size)
        assertTrue(randomPool.any { it.title == "Die With A Smile" && it.isRandomPool })
        assertTrue(randomPool.any { it.title == "Daydreaming" && it.isRandomPool })
    }

    @Test
    fun importPlaylist_fallsBackToSearchWhenGetLyricsFails() = runBlocking {
        val playlist = PlaylistExtractResult(
            playlistTitle = "Hits",
            platform = PlaylistPlatform.YOUTUBE,
            tracks = listOf(
                TrackImportQuery("APT", "ROSE & Bruno Mars")
            )
        )

        // getLyrics fails (e.g. 404 or name mismatch)
        coEvery { mockClient.getLyrics("APT", "ROSE & Bruno Mars") } returns Result.failure(Exception("404 Not Found"))

        // searchTracks succeeds
        coEvery { mockClient.searchTracks("ROSE & Bruno Mars APT") } returns Result.success(
            listOf(
                LrcTrackDto(
                    id = 103L,
                    trackName = "APT.",
                    artistName = "ROSÉ, Bruno Mars",
                    plainLyrics = "Apateu, apateu"
                )
            )
        )

        val states = repository.importPlaylist(playlist).toList()
        val lastState = states.last() as PlaylistImportState.Success
        assertEquals(1, lastState.importedCount)

        val found = db.songDao().findByLrclibId(103L)
        assertNotNull(found)
        assertEquals("APT.", found?.title)
        assertTrue(found?.isRandomPool == true)
    }

    @Test
    fun importPlaylist_skipsExistingCuratedSongWithoutModifyingSnippets() = runBlocking {
        // Pre-insert an existing song with curated snippets
        val songId = db.songDao().insertSong(
            SongEntity(
                title = "Creep",
                artist = "Radiohead",
                plainLyrics = "I wish I was special",
                lrclibId = 999L,
                isRandomPool = false
            )
        )
        db.snippetDao().insertSnippet(
            SnippetEntity(
                songId = songId,
                content = "I'm a weirdo",
                isActive = true
            )
        )

        val playlist = PlaylistExtractResult(
            playlistTitle = "Radiohead Collection",
            platform = PlaylistPlatform.DEEZER,
            tracks = listOf(
                TrackImportQuery("Creep", "Radiohead")
            )
        )

        val states = repository.importPlaylist(playlist).toList()
        val success = states.last() as PlaylistImportState.Success
        assertEquals(0, success.importedCount)
        assertEquals(1, success.skippedCount)

        // Verify existing snippets were untouched
        val snippets = db.snippetDao().getSnippetsForSong(songId)
        assertEquals(1, snippets.size)
        assertEquals("I'm a weirdo", snippets[0].content)
        val song = db.songDao().getSongById(songId)
        assertEquals(false, song?.isRandomPool)
    }
}
