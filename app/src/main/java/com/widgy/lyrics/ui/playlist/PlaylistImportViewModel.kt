package com.widgy.lyrics.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.api.playlist.CompositePlaylistExtractor
import com.widgy.lyrics.data.api.playlist.PlaylistExtractor
import com.widgy.lyrics.data.api.playlist.SharedHttpClient
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.playlist.PlaylistImportState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistImportViewModel(
    private val repository: LyricsRepository,
    private val extractor: PlaylistExtractor = CompositePlaylistExtractor()
) : ViewModel() {

    private val _importState = MutableStateFlow<PlaylistImportState>(PlaylistImportState.Idle)
    val importState: StateFlow<PlaylistImportState> = _importState.asStateFlow()

    private var importJob: Job? = null

    fun startImport(rawInput: String) {
        val url = SharedHttpClient.extractUrlFromText(rawInput) ?: rawInput.trim()
        if (url.isBlank()) {
            _importState.value = PlaylistImportState.Error("Please enter a valid playlist link.")
            return
        }

        if (!extractor.canHandle(url)) {
            _importState.value = PlaylistImportState.Error("Unsupported link. Supported: Spotify, YouTube/YouTube Music, Deezer.")
            return
        }

        val platform = SharedHttpClient.detectPlatform(url)
        _importState.value = PlaylistImportState.Extracting(platform)

        importJob?.cancel()
        importJob = viewModelScope.launch {
            val extractResult = extractor.extract(url)
            extractResult.fold(
                onSuccess = { result ->
                    if (result.tracks.isEmpty()) {
                        _importState.value = PlaylistImportState.Error("No tracks found in playlist.")
                        return@fold
                    }
                    repository.importPlaylist(result).collect { state ->
                        _importState.value = state
                    }
                },
                onFailure = { error ->
                    _importState.value = PlaylistImportState.Error(error.message ?: "Failed to extract playlist tracks.")
                }
            )
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        val current = _importState.value
        if (current is PlaylistImportState.FetchingLyrics) {
            _importState.value = PlaylistImportState.Cancelled(
                playlistTitle = current.playlistTitle,
                importedCount = current.matchedCount
            )
        } else {
            _importState.value = PlaylistImportState.Idle
        }
    }

    fun clearError() {
        if (_importState.value is PlaylistImportState.Error) {
            _importState.value = PlaylistImportState.Idle
        }
    }

    fun reset() {
        importJob?.cancel()
        _importState.value = PlaylistImportState.Idle
    }

    class Factory(
        private val repository: LyricsRepository,
        private val extractor: PlaylistExtractor = CompositePlaylistExtractor()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlaylistImportViewModel(repository, extractor) as T
        }
    }
}
