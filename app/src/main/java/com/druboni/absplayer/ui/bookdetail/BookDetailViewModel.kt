package com.druboni.absplayer.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.druboni.absplayer.data.api.model.Chapter
import com.druboni.absplayer.data.api.model.LibraryItem
import com.druboni.absplayer.data.api.model.MediaProgressResponse
import com.druboni.absplayer.data.local.entity.DownloadedBookEntity
import com.druboni.absplayer.data.repository.AudiobookRepository
import com.druboni.absplayer.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val item: LibraryItem? = null,
    val progress: MediaProgressResponse? = null,
    val downloadedBook: DownloadedBookEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val coverUrl: String? = null
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: AudiobookRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(itemId: String) {
        viewModelScope.launch {
            _uiState.value = BookDetailUiState(isLoading = true)
            val serverUrl = repository.getServerUrl()
            repository.getItem(itemId)
                .onSuccess { item ->
                    val progress = repository.getProgress(itemId).getOrNull()
                    _uiState.value = BookDetailUiState(
                        item = item,
                        progress = progress,
                        isLoading = false,
                        coverUrl = serverUrl?.let { "$it/api/items/$itemId/cover" }
                    )
                }
                .onFailure { _uiState.value = BookDetailUiState(isLoading = false, error = it.message) }
        }
    }

    fun downloadBook(itemId: String) {
        val item = _uiState.value.item ?: return
        val meta = item.media.metadata
        val coverUrl = _uiState.value.coverUrl
        val duration = item.media.duration ?: 0.0

        val request = DownloadWorker.buildRequest(itemId, meta.title, meta.authorName, coverUrl, duration)
        workManager.enqueue(request)
    }

    fun formatDuration(seconds: Double?): String {
        if (seconds == null || seconds <= 0) return ""
        val h = (seconds / 3600).toInt()
        val m = ((seconds % 3600) / 60).toInt()
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun progressPercent(): Float {
        val p = _uiState.value.progress ?: return 0f
        return (p.progress * 100).toFloat().coerceIn(0f, 100f)
    }
}
