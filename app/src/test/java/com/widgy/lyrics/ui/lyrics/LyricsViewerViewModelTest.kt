package com.widgy.lyrics.ui.lyrics

import com.widgy.lyrics.data.db.SongEntity
import com.widgy.lyrics.data.repository.LyricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: LyricsRepository = mockk(relaxed = true)
    private lateinit var viewModel: LyricsViewerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LyricsViewerViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveSelectedSnippets_onExistingSong_passesExistingSongId() = runTest(testDispatcher) {
        val existingSong = SongEntity(
            id = 42L,
            title = "Sing About Me",
            artist = "Kendrick Lamar",
            plainLyrics = "Promise that you will sing about me\nForever"
        )
        coEvery { repository.getSongById(42L) } returns existingSong

        viewModel.loadLocalSong(42L)
        testScheduler.advanceUntilIdle()

        viewModel.toggleLineSelection(0)
        viewModel.saveSelectedSnippets {}
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.saveSongWithSnippets(
                title = "Sing About Me",
                artist = "Kendrick Lamar",
                album = any(),
                plainLyrics = any(),
                lrclibId = any(),
                snippets = listOf("Promise that you will sing about me"),
                isRandomPool = false,
                existingSongId = 42L
            )
        }
    }
}
