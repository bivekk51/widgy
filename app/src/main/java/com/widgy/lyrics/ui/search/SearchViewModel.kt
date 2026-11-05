package com.widgy.lyrics.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.widgy.lyrics.data.api.LrcLibClient
import com.widgy.lyrics.data.model.LrcTrackDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<LrcTrackDto> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val client: LrcLibClient = LrcLibClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                if (q.isBlank()) {
                    searchJob?.cancel()
                    _uiState.update { it.copy(results = emptyList(), isLoading = false, errorMessage = null) }
                } else {
                    executeSearch(q)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryFlow.value = newQuery
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        searchJob = viewModelScope.launch {
            val result = client.searchTracks(query)
            result.onSuccess { tracks ->
                _uiState.update { it.copy(isLoading = false, results = tracks, errorMessage = null) }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load tracks. Check connection."
                        )
                    }
                }
            }
        }
    }
}
