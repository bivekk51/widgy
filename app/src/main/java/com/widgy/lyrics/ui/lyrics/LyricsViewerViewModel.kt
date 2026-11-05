package com.widgy.lyrics.ui.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.api.LyricLine
import com.widgy.lyrics.data.api.LyricsCleaner
import com.widgy.lyrics.data.model.LrcTrackDto
import com.widgy.lyrics.data.repository.LyricsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LyricsViewerUiState(
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val lrclibId: Long? = null,
    val lines: List<LyricLine> = emptyList(),
    val rawLyrics: String = "",
    val selectedLineIndices: Set<Int> = emptySet(),
    val isRandomPool: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class LyricsViewerViewModel(
    private val repository: LyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsViewerUiState())
    val uiState: StateFlow<LyricsViewerUiState> = _uiState.asStateFlow()

    fun loadTrack(track: LrcTrackDto) {
        val raw = track.plainLyrics ?: track.syncedLyrics ?: ""
        val cleaned = LyricsCleaner.cleanLyrics(raw)
        _uiState.update {
            it.copy(
                title = track.displayTitle,
                artist = track.artistName ?: "Unknown Artist",
                album = track.albumName,
                lrclibId = track.id,
                rawLyrics = raw,
                lines = cleaned,
                isLoading = false
            )
        }
        if (raw.isBlank() && track.trackName != null && track.artistName != null) {
            fetchLyrics(track.trackName, track.artistName)
        }
    }

    fun loadLocalSong(songId: Long, highlightSnippetId: Long? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val song = repository.getSongById(songId)
            if (song != null) {
                val cleaned = LyricsCleaner.cleanLyrics(song.plainLyrics)
                _uiState.update {
                    it.copy(
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        lrclibId = song.lrclibId,
                        rawLyrics = song.plainLyrics,
                        lines = cleaned,
                        isRandomPool = song.isRandomPool,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Song not found") }
            }
        }
    }

    private fun fetchLyrics(trackName: String, artistName: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.fetchLyricsOnline(trackName, artistName)
            result.onSuccess { track ->
                val raw = track.plainLyrics ?: track.syncedLyrics ?: ""
                val cleaned = LyricsCleaner.cleanLyrics(raw)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        rawLyrics = raw,
                        lines = cleaned,
                        errorMessage = if (cleaned.isEmpty()) "No lyrics available for this track" else null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to fetch lyrics")
                }
            }
        }
    }

    fun toggleLineSelection(index: Int) {
        _uiState.update { state ->
            val newSelection = if (state.selectedLineIndices.contains(index)) {
                state.selectedLineIndices - index
            } else {
                state.selectedLineIndices + index
            }
            state.copy(selectedLineIndices = newSelection)
        }
    }

    fun saveSelectedSnippets(onSuccess: () -> Unit) {
        val state = _uiState.value
        val selectedTexts = state.selectedLineIndices
            .sorted()
            .mapNotNull { idx -> state.lines.getOrNull(idx)?.text }

        if (selectedTexts.isEmpty()) return

        viewModelScope.launch {
            repository.saveSongWithSnippets(
                title = state.title,
                artist = state.artist,
                album = state.album,
                plainLyrics = state.rawLyrics,
                lrclibId = state.lrclibId,
                snippets = selectedTexts,
                isRandomPool = false
            )
            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }

    fun saveAsRandomPool(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveSongWithSnippets(
                title = state.title,
                artist = state.artist,
                album = state.album,
                plainLyrics = state.rawLyrics,
                lrclibId = state.lrclibId,
                snippets = emptyList(),
                isRandomPool = true
            )
            _uiState.update { it.copy(isSaved = true) }
            onSuccess()
        }
    }
}
