package com.example.musicplayer.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val searchResults: List<MusicFile> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    // TODO: Inject SearchRepository when implemented
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }

        if (query.length >= 2) {
            performSearch(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            try {
                // TODO: Implement actual search logic with Repository
                Timber.d("Searching for: $query")

                // Placeholder: Empty results for now
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Search failed")
                _uiState.update {
                    it.copy(
                        error = "검색 실패: ${e.message}",
                        isSearching = false
                    )
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            SearchUiState()
        }
    }
}
