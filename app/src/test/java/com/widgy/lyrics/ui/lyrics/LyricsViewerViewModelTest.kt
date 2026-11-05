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
    fun toggleLineSelection_limitsToMaximumTwoLines() = runTest(testDispatcher) {
        viewModel.toggleLineSelection(0)
        viewModel.toggleLineSelection(1)
        viewModel.toggleLineSelection(2)

        assertEquals(setOf(0, 1), viewModel.uiState.value.selectedLineIndices)
        assertEquals("Maximum 2 lines can be selected", viewModel.uiState.value.selectionWarning)
    }

    @Test
    fun saveSelectedSnippets_twoLinesSelected_savesAsSingleTwoLineSnippet() = runTest(testDispatcher) {
        val existingSong = SongEntity(
            id = 42L,
            title = "Sing About Me",
            artist = "Kendrick Lamar",
            plainLyrics = "Line 1\nLine 2\nLine 3"
        )
        coEvery { repository.getSongById(42L) } returns existingSong

        viewModel.loadLocalSong(42L)
        testScheduler.advanceUntilIdle()

        viewModel.toggleLineSelection(0)
        viewModel.toggleLineSelection(1)
        viewModel.saveSelectedSnippets {}
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.saveSongWithSnippets(
                title = "Sing About Me",
                artist = "Kendrick Lamar",
                album = any(),
                plainLyrics = any(),
                lrclibId = any(),
                snippets = listOf("Line 1\nLine 2"),
                isRandomPool = false,
                existingSongId = 42L
            )
        }
    }
}
