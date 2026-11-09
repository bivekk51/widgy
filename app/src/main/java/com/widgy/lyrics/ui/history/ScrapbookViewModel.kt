package com.widgy.lyrics.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.db.DailyHistoryEntity
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.widget.WidgetUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScrapbookUiState(
    val historyItems: List<DailyHistoryEntity> = emptyList(),
    val pinnedLyricText: String? = null
)

class ScrapbookViewModel(
    private val repository: LyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScrapbookUiState())
    val uiState: StateFlow<ScrapbookUiState> = _uiState.asStateFlow()

    init {
        repository.observeDailyHistory()
            .onEach { items ->
                _uiState.update { it.copy(historyItems = items) }
            }
            .launchIn(viewModelScope)
    }

    fun syncPinnedState(context: Context) {
        val prefs = WidgetPreferences(context)
        val pinned = prefs.getPinnedLyric()
        _uiState.update { it.copy(pinnedLyricText = pinned?.text) }
    }

    fun pinHistoryLyric(context: Context, entity: DailyHistoryEntity) {
        val prefs = WidgetPreferences(context)
        val isCurrentlyPinned = prefs.isLyricPinned() && prefs.getPinnedLyric()?.text == entity.lyricText

        if (isCurrentlyPinned) {
            prefs.setLyricPinned(false)
            _uiState.update { it.copy(pinnedLyricText = null) }
        } else {
            val dailyLyric = DailyLyric(
                date = entity.date,
                songId = entity.songId,
                snippetId = entity.snippetId,
                text = entity.lyricText,
                songTitle = entity.songTitle,
                artistName = entity.artistName
            )
            prefs.setLyricPinned(true, dailyLyric)
            _uiState.update { it.copy(pinnedLyricText = entity.lyricText) }
        }

        WidgetUpdateManager.notifyWidgetUpdate(context)
    }
}
