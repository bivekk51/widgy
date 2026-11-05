package com.widgy.lyrics.ui.dashboard

import com.widgy.lyrics.data.repository.LyricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: LyricsRepository = mockk(relaxed = true)
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeAllSongsWithSnippets() } returns flowOf(emptyList())
        coEvery { repository.getTodayDailyLyric() } returns null
        viewModel = DashboardViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleSnippetActive_updatesRepository() = runTest(testDispatcher) {
        viewModel.toggleSnippetActive(snippetId = 1L, isActive = false)
        testScheduler.advanceUntilIdle()
        coVerify { repository.setSnippetActive(1L, false) }
    }
}
