package com.druboni.absplayer.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.druboni.absplayer.data.repository.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = "https://earmarked.the-phylactery.com",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AudiobookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun updateServerUrl(url: String) { _uiState.value = _uiState.value.copy(serverUrl = url) }
    fun updateUsername(v: String) { _uiState.value = _uiState.value.copy(username = v) }
    fun updatePassword(v: String) { _uiState.value = _uiState.value.copy(password = v) }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.serverUrl.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "All fields are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            repository.login(state.serverUrl.trimEnd('/'), state.username, state.password)
                .onSuccess { onSuccess() }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Login failed") }
        }
    }
}
