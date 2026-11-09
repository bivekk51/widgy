package com.widgy.lyrics.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.db.SongWithSnippets
import com.widgy.lyrics.data.preferences.WidgetConfig
import com.widgy.lyrics.data.preferences.WidgetPreferences
import com.widgy.lyrics.data.repository.LyricsRepository
import com.widgy.lyrics.domain.model.DailyLyric
import com.widgy.lyrics.domain.theme.ThemeConfig
import com.widgy.lyrics.widget.WidgetUpdateManager
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
    val widgetConfig: WidgetConfig = WidgetConfig(),
    val themeConfig: ThemeConfig = ThemeConfig(),
    val isRefreshingLyric: Boolean = false,
    val isLoading: Boolean = false,
    val isLyricPinned: Boolean = false,
    val rotationIntervalHours: Int = 24
)

class DashboardViewModel(
    private val repository: LyricsRepository,
    context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            widgetConfig = context?.let { WidgetPreferences(it).getConfig() } ?: WidgetConfig(),
            themeConfig = context?.let { WidgetPreferences(it).getThemeConfig() } ?: ThemeConfig()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadTodayLyric()
        repository.observeAllSongsWithSnippets()
            .onEach { songs ->
                _uiState.update { it.copy(savedSongs = songs) }
            }
            .launchIn(viewModelScope)
    }

    fun loadTodayLyric(context: Context? = null) {
        viewModelScope.launch {
            val prefs = context?.let { WidgetPreferences(it) }
            val interval = prefs?.getRotationIntervalHours() ?: _uiState.value.rotationIntervalHours
            val pinned = prefs?.getPinnedLyric()
            val lyric = pinned ?: repository.getTodayDailyLyric(intervalHours = interval)
            _uiState.update {
                it.copy(
                    todayLyric = lyric,
                    isLyricPinned = prefs?.isLyricPinned() ?: false,
                    rotationIntervalHours = interval
                )
            }
        }
    }

    fun toggleLyricPin(context: Context) {
        val prefs = WidgetPreferences(context)
        val currentlyPinned = prefs.isLyricPinned()
        val newPinned = !currentlyPinned
        val lyricToPin = if (newPinned) _uiState.value.todayLyric else null
        prefs.setLyricPinned(newPinned, lyricToPin)
        _uiState.update { it.copy(isLyricPinned = newPinned) }
        viewModelScope.launch {
            WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    fun reloadConfig(context: Context) {
        val config = WidgetPreferences(context).getConfig()
        val themeConfig = WidgetPreferences(context).getThemeConfig()
        val interval = WidgetPreferences(context).getRotationIntervalHours()
        _uiState.update { it.copy(widgetConfig = config, themeConfig = themeConfig, rotationIntervalHours = interval) }
    }

    fun updateRotationInterval(hours: Int, context: Context) {
        val prefs = WidgetPreferences(context)
        prefs.setRotationIntervalHours(hours)
        _uiState.update { it.copy(rotationIntervalHours = hours) }
        WidgetUpdateManager.scheduleRotation(context, hours)
        refreshTodayLyric(context)
    }

    fun updateThemeConfig(newConfig: ThemeConfig, context: Context) {
        WidgetPreferences(context).saveThemeConfig(newConfig)
        val legacy = WidgetPreferences(context).getConfig()
        _uiState.update { it.copy(themeConfig = newConfig, widgetConfig = legacy) }
        viewModelScope.launch {
            WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    fun refreshTodayLyric(context: Context? = null) {
        _uiState.update { it.copy(isRefreshingLyric = true) }
        viewModelScope.launch {
            val interval = _uiState.value.rotationIntervalHours
            val newLyric = repository.refreshTodayLyric(intervalHours = interval)
            _uiState.update { it.copy(todayLyric = newLyric, isRefreshingLyric = false) }
            context?.let {
                if (newLyric != null) {
                    WidgetPreferences(it).saveCachedLyric(newLyric)
                }
                WidgetUpdateManager.updateAllWidgets(it)
            }
        }
    }

    fun updateWidgetConfig(newConfig: WidgetConfig, context: Context) {
        WidgetPreferences(context).saveConfig(newConfig)
        _uiState.update { it.copy(widgetConfig = newConfig) }
        viewModelScope.launch {
            WidgetUpdateManager.updateAllWidgets(context)
        }
    }

    fun toggleSnippetActive(snippetId: Long, isActive: Boolean, context: Context? = null) {
        viewModelScope.launch {
            repository.setSnippetActive(snippetId, isActive)
            context?.let { WidgetUpdateManager.updateAllWidgets(it) }
        }
    }

    fun deleteSong(songId: Long, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteSong(songId)
            loadTodayLyric()
            context?.let { WidgetUpdateManager.updateAllWidgets(it) }
        }
    }

    fun deleteSnippet(snippetId: Long, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteSnippet(snippetId)
            loadTodayLyric()
            context?.let { WidgetUpdateManager.updateAllWidgets(it) }
        }
    }
}
