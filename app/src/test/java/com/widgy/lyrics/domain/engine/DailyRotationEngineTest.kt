package com.widgy.lyrics.domain.engine

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.widgy.lyrics.data.db.AppDatabase
import com.widgy.lyrics.data.db.SnippetEntity
import com.widgy.lyrics.data.db.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DailyRotationEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var engine: DailyRotationEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        engine = DailyRotationEngine(db.songDao(), db.snippetDao(), db.dailyHistoryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun resolveDailyLyric_emptyPool_returnsNullWithoutCrashing() = runBlocking {
        val result = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5))
        assertNull(result)
    }

    @Test
    fun resolveDailyLyric_deterministicAcrossMultipleCallsOnSameDate() = runBlocking {
        val songId = db.songDao().insertSong(
            SongEntity(title = "Sing About Me", artist = "Kendrick Lamar", plainLyrics = "Sample lyrics")
        )
        db.snippetDao().insertSnippet(
            SnippetEntity(songId = songId, content = "Promise that you will sing about me", isActive = true)
        )

        val lyric1 = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = false)
        val lyric2 = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = false)

        assertNotNull(lyric1)
        assertEquals(lyric1, lyric2)
        assertEquals("Promise that you will sing about me", lyric1?.text)
        assertEquals("Sing About Me", lyric1?.songTitle)
    }

    @Test
    fun resolveDailyLyric_balancedCandidates_randomPoolDoesNotOverwhelmSnippets() = runBlocking {
        // Song A: 1 curated snippet
        val songAId = db.songDao().insertSong(
            SongEntity(title = "Curated Song", artist = "Artist A", plainLyrics = "Line 1")
        )
        db.snippetDao().insertSnippet(
            SnippetEntity(songId = songAId, content = "Curated Line", isActive = true)
        )

        // Song B: marked as random pool, containing 50 lines
        val fiftyLines = (1..50).joinToString("\n") { "Random Line $it" }
        db.songDao().insertSong(
            SongEntity(title = "Random Song", artist = "Artist B", plainLyrics = fiftyLines, isRandomPool = true)
        )

        // There should be exactly 2 candidates in candidate pool: 1 curated snippet and 1 random-pool song
        val candidates = engine.buildCandidatePool()
        assertEquals(2, candidates.size)
    }

    @Test
    fun resolveDailyLyric_forceRefresh_singleSongWithLyrics_updatesStoredHistory() = runBlocking {
        val multilineLyrics = """
            Line 1: Promise that you will sing about me
            Line 2: When I am gone
            Line 3: Promise you will remember me
            Line 4: Dying of thirst
        """.trimIndent()
        db.songDao().insertSong(
            SongEntity(title = "Sing About Me", artist = "Kendrick Lamar", plainLyrics = multilineLyrics, isRandomPool = true)
        )

        val firstLyric = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = false)
        assertNotNull(firstLyric)

        val refreshed = engine.resolveDailyLyric(LocalDate.of(2026, 11, 5), forceRefresh = true)
        assertNotNull(refreshed)

        val stored = db.dailyHistoryDao().getDailyLyric("2026-11-05")
        assertEquals(refreshed?.text, stored?.lyricText)
    }
}
