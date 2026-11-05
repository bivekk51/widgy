package com.widgy.lyrics.ui.search

import com.widgy.lyrics.data.api.LrcLibClient
import com.widgy.lyrics.data.model.LrcTrackDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val client: LrcLibClient = mockk()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { client.searchTracks("Kendrick") } returns Result.success(
            listOf(
                LrcTrackDto(id = 1L, trackName = "Like That", artistName = "Future, Metro Boomin, Kendrick Lamar")
            )
        )
        viewModel = SearchViewModel(client)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onQueryChanged_debouncesAndEmitsResults() = runTest(testDispatcher) {
        viewModel.onQueryChanged("Kendrick")
        advanceTimeBy(350)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.results.size)
        assertEquals("Like That", state.results[0].displayTitle)
    }
}
