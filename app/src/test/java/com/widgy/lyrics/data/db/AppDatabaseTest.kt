package com.widgy.lyrics.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertingSongAndSnippets_cascadesOnDelete() = runBlocking {
        val song = SongEntity(
            title = "Sing About Me",
            artist = "Kendrick Lamar",
            plainLyrics = "Promise that you will sing about me forever",
            isRandomPool = false
        )
        val songId = db.songDao().insertSong(song)
        db.snippetDao().insertSnippet(
            SnippetEntity(
                songId = songId,
                content = "Promise that you will sing about me",
                isActive = true
            )
        )

        val beforeDelete = db.snippetDao().getSnippetsForSong(songId)
        assertEquals(1, beforeDelete.size)

        db.songDao().deleteSongById(songId)
        val afterDelete = db.snippetDao().getSnippetsForSong(songId)
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun dailyHistory_insertOrUpdate_persistsState() = runBlocking {
        val history = DailyHistoryEntity(
            date = "2026-11-05",
            songId = 1L,
            snippetId = 2L,
            lyricText = "It's just big me",
            songTitle = "Like That",
            artistName = "Future, Metro Boomin, Kendrick Lamar",
            updatedAt = System.currentTimeMillis()
        )
        db.dailyHistoryDao().insertOrUpdate(history)

        val retrieved = db.dailyHistoryDao().getDailyLyric("2026-11-05")
        assertNotNull(retrieved)
        assertEquals("It's just big me", retrieved?.lyricText)
    }
}
