package com.widgy.lyrics.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SongDaoDeduplicationTest {

    private lateinit var db: AppDatabase
    private lateinit var songDao: SongDao
    private lateinit var snippetDao: SnippetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        songDao = db.songDao()
        snippetDao = db.snippetDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun findByLrclibId_returnsMatchingSong() = runBlocking {
        val song = SongEntity(
            title = "Daydreaming",
            artist = "Radiohead",
            plainLyrics = "Dreamers, they never learn",
            lrclibId = 987654L,
            isRandomPool = true
        )
        songDao.insertSong(song)

        val found = songDao.findByLrclibId(987654L)
        assertNotNull(found)
        assertEquals("Daydreaming", found?.title)
        assertEquals("Radiohead", found?.artist)

        val notFound = songDao.findByLrclibId(111111L)
        assertNull(notFound)
    }

    @Test
    fun findByTitleAndArtist_caseInsensitiveMatch() = runBlocking {
        val song = SongEntity(
            title = "Die With A Smile",
            artist = "Lady Gaga, Bruno Mars",
            plainLyrics = "If the world was ending...",
            isRandomPool = true
        )
        songDao.insertSong(song)

        val found = songDao.findByTitleAndArtist("die with a smile", "lady gaga, bruno mars")
        assertNotNull(found)
        assertEquals("Die With A Smile", found?.title)

        val notFound = songDao.findByTitleAndArtist("Apt", "Rose")
        assertNull(notFound)
    }

    @Test
    fun existingCuratedSnippets_arePreservedWhenCheckingDeduplication() = runBlocking {
        val song = SongEntity(
            title = "Creep",
            artist = "Radiohead",
            plainLyrics = "I wish I was special",
            isRandomPool = false
        )
        val songId = songDao.insertSong(song)
        snippetDao.insertSnippet(
            SnippetEntity(
                songId = songId,
                content = "I'm a creep, I'm a weirdo",
                isActive = true
            )
        )

        // Query deduplication before deciding whether to insert
        val existing = songDao.findByTitleAndArtist("Creep", "Radiohead")
        assertNotNull(existing)
        assertEquals(false, existing?.isRandomPool)

        // Snippets are intact
        val snippets = snippetDao.getSnippetsForSong(songId)
        assertEquals(1, snippets.size)
        assertEquals("I'm a creep, I'm a weirdo", snippets[0].content)
    }
}
