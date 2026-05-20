package com.druboni.absplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.druboni.absplayer.data.api.model.Library
import com.druboni.absplayer.data.api.model.LibraryItem
import com.druboni.absplayer.data.repository.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val libraries: List<Library> = emptyList(),
    val selectedLibraryId: String? = null,
    val books: List<LibraryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val serverUrl: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: AudiobookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadLibraries()
    }

    private fun loadLibraries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val serverUrl = repository.getServerUrl()
            repository.getLibraries()
                .onSuccess { libs ->
                    _uiState.value = _uiState.value.copy(libraries = libs, serverUrl = serverUrl)
                    libs.firstOrNull()?.let { selectLibrary(it.id) }
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun selectLibrary(libraryId: String) {
        _uiState.value = _uiState.value.copy(selectedLibraryId = libraryId, isLoading = true)
        viewModelScope.launch {
            repository.getLibraryItems(libraryId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(books = response.results, isLoading = false)
                }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onComplete()
        }
    }

    fun coverUrl(itemId: String): String? {
        val serverUrl = _uiState.value.serverUrl ?: return null
        return "$serverUrl/api/items/$itemId/cover"
    }
}
