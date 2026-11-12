package com.widgy.lyrics.ui.playlist

import com.widgy.lyrics.data.api.playlist.PlaylistExtractor
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult
import com.widgy.lyrics.domain.model.playlist.PlaylistImportState
import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import com.widgy.lyrics.domain.model.playlist.TrackImportQuery
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LyricsRepository
    private lateinit var extractor: PlaylistExtractor
    private lateinit var viewModel: PlaylistImportViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        extractor = mockk()
        viewModel = PlaylistImportViewModel(repository, extractor)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startImport_withUnsupportedUrl_setsErrorState() = runTest {
        every { extractor.canHandle(any()) } returns false

        viewModel.startImport("https://example.com/playlist")

        assertTrue(viewModel.importState.value is PlaylistImportState.Error)
    }

    @Test
    fun startImport_successfulFlow_emitsSuccess() = runTest {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        every { extractor.canHandle(url) } returns true

        val extractResult = PlaylistExtractResult(
            playlistTitle = "Chill",
            platform = PlaylistPlatform.SPOTIFY,
            tracks = listOf(TrackImportQuery("Song", "Artist"))
        )

        coEvery { extractor.extract(url) } returns Result.success(extractResult)
        coEvery { repository.importPlaylist(extractResult) } returns flowOf(
            PlaylistImportState.FetchingLyrics("Chill", 0, 1, "Song", 0),
            PlaylistImportState.Success("Chill", 1, 1, 0)
        )

        viewModel.startImport(url)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.importState.value
        assertTrue(state is PlaylistImportState.Success)
        assertEquals("Chill", (state as PlaylistImportState.Success).playlistTitle)
    }

    @Test
    fun cancelImport_cancelsJobAndSetsCancelledState() = runTest {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        every { extractor.canHandle(url) } returns true
        coEvery { extractor.extract(url) } returns Result.success(
            PlaylistExtractResult("Chill", PlaylistPlatform.SPOTIFY, listOf(TrackImportQuery("Song", "Artist")))
        )

        // Mock long running flow
        coEvery { repository.importPlaylist(any()) } returns kotlinx.coroutines.flow.flow {
            emit(PlaylistImportState.FetchingLyrics("Chill", 0, 10, "Song", 3))
            kotlinx.coroutines.delay(10000)
        }

        viewModel.startImport(url)
        testDispatcher.scheduler.runCurrent()

        viewModel.cancelImport()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.importState.value
        assertTrue(state is PlaylistImportState.Cancelled)
        assertEquals(3, (state as PlaylistImportState.Cancelled).importedCount)
    }
}
