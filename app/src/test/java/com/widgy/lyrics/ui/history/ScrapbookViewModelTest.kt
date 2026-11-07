package com.widgy.lyrics.ui.history

import com.widgy.lyrics.data.db.DailyHistoryEntity
import com.widgy.lyrics.data.repository.LyricsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrapbookViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: LyricsRepository = mockk(relaxed = true)
    private lateinit var viewModel: ScrapbookViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val historyList = listOf(
            DailyHistoryEntity(
                date = "2026-11-06",
                songId = 1L,
                lyricText = "Yesterday's memorable lyric",
                songTitle = "Yesterday Song",
                artistName = "Great Artist"
            )
        )
        every { repository.observeDailyHistory() } returns flowOf(historyList)
        viewModel = ScrapbookViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsHistoryFromRepository() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.historyItems.size)
        assertEquals("Yesterday's memorable lyric", viewModel.uiState.value.historyItems[0].lyricText)
    }
}
