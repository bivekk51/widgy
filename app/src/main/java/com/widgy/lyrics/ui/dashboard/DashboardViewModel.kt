package com.widgy.lyrics.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.db.SongWithSnippets
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayLyric: DailyLyric? = null,
    val savedSongs: List<SongWithSnippets> = emptyList(),
    val isRefreshingLyric: Boolean = false,
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: LyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadTodayLyric()
        repository.observeAllSongsWithSnippets()
            .onEach { songs ->
                _uiState.update { it.copy(savedSongs = songs) }
            }
            .launchIn(viewModelScope)
    }

    fun loadTodayLyric() {
        viewModelScope.launch {
            val lyric = repository.getTodayDailyLyric()
            _uiState.update { it.copy(todayLyric = lyric) }
        }
    }

    fun refreshTodayLyric() {
        _uiState.update { it.copy(isRefreshingLyric = true) }
        viewModelScope.launch {
            val newLyric = repository.refreshTodayLyric()
            _uiState.update { it.copy(todayLyric = newLyric, isRefreshingLyric = false) }
        }
    }

    fun toggleSnippetActive(snippetId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.setSnippetActive(snippetId, isActive)
        }
    }

    fun deleteSong(songId: Long) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            loadTodayLyric()
        }
    }

    fun deleteSnippet(snippetId: Long) {
        viewModelScope.launch {
            repository.deleteSnippet(snippetId)
            loadTodayLyric()
        }
    }
}
