package com.druboni.absplayer.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.druboni.absplayer.data.api.model.AudioTrack
import com.druboni.absplayer.data.api.model.Chapter
import com.druboni.absplayer.data.api.model.PlaybackSessionResponse
import com.druboni.absplayer.data.local.entity.DownloadedTrackEntity
import com.druboni.absplayer.data.repository.AudiobookRepository
import com.druboni.absplayer.service.AudioPlaybackService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val title: String = "",
    val author: String? = null,
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int = -1,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AudiobookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var playbackSession: PlaybackSessionResponse? = null
    private var progressJob: Job? = null
    private var currentItemId: String? = null

    fun initPlayer(itemId: String) {
        currentItemId = itemId
        val sessionToken = SessionToken(context, ComponentName(context, AudioPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(playerListener)
            startPlayback(itemId)
        }, MoreExecutors.directExecutor())
    }

    private fun startPlayback(itemId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val serverUrl = repository.getServerUrl() ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "No server URL")
                return@launch
            }
            val token = repository.getToken() ?: run {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not authenticated")
                return@launch
            }

            val localTracks = repository.getDownloadedTracks(itemId)
            if (localTracks.isNotEmpty()) {
                loadLocalTracks(itemId, localTracks, serverUrl)
            } else {
                loadRemoteSession(itemId, serverUrl, token)
            }
        }
    }

    private suspend fun loadRemoteSession(itemId: String, serverUrl: String, token: String) {
        repository.startPlaybackSession(itemId)
            .onSuccess { session ->
                playbackSession = session
                val item = repository.getItem(itemId).getOrNull()
                val meta = item?.media?.metadata
                _uiState.value = _uiState.value.copy(
                    title = meta?.title ?: "",
                    author = meta?.authorName,
                    coverUrl = "$serverUrl/api/items/$itemId/cover",
                    chapters = session.chapters,
                    isLoading = false
                )

                val mediaItems = session.audioTracks.map { track ->
                    val rawUrl = track.contentUrl ?: return@map null
                    val url = if (rawUrl.startsWith("http")) "$rawUrl?token=$token"
                    else "$serverUrl$rawUrl?token=$token"
                    MediaItem.fromUri(url)
                }.filterNotNull()

                mediaController?.apply {
                    setMediaItems(mediaItems)
                    prepare()
                    val savedProgress = repository.getProgress(itemId).getOrNull()
                    if (savedProgress != null && savedProgress.currentTime > 0) {
                        seekTo((savedProgress.currentTime * 1000).toLong())
                    } else if (session.currentTime > 0) {
                        seekTo((session.currentTime * 1000).toLong())
                    }
                    play()
                }
                startProgressTracking()
            }
            .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
    }

    private suspend fun loadLocalTracks(itemId: String, tracks: List<DownloadedTrackEntity>, serverUrl: String) {
        val item = repository.getItem(itemId).getOrNull()
        val meta = item?.media?.metadata
        _uiState.value = _uiState.value.copy(
            title = meta?.title ?: "",
            author = meta?.authorName,
            coverUrl = "$serverUrl/api/items/$itemId/cover",
            chapters = item?.media?.chapters ?: emptyList(),
            isLoading = false
        )

        val mediaItems = tracks.map { track -> MediaItem.fromUri("file://${track.localPath}") }
        mediaController?.apply {
            setMediaItems(mediaItems)
            prepare()
            val savedProgress = repository.getProgress(itemId).getOrNull()
            if (savedProgress != null && savedProgress.currentTime > 0) {
                seekTo((savedProgress.currentTime * 1000).toLong())
            }
            play()
        }
        startProgressTracking()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _uiState.value = _uiState.value.copy(
                    duration = mediaController?.duration ?: 0L
                )
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController ?: break
                val pos = controller.currentPosition
                val dur = controller.duration.takeIf { it > 0 } ?: 0L
                _uiState.value = _uiState.value.copy(
                    currentPosition = pos,
                    duration = dur,
                    currentChapterIndex = currentChapterIndex(pos)
                )
                val itemId = currentItemId
                if (itemId != null && dur > 0 && pos > 0) {
                    repository.updateProgress(itemId, pos / 1000.0, dur / 1000.0)
                }
                delay(5000)
            }
        }
    }

    private fun currentChapterIndex(posMs: Long): Int {
        val posSec = posMs / 1000.0
        return _uiState.value.chapters.indexOfLast { posSec >= it.start }
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun seekForward(ms: Long = 30_000) {
        val ctrl = mediaController ?: return
        ctrl.seekTo((ctrl.currentPosition + ms).coerceAtMost(ctrl.duration))
    }

    fun seekBack(ms: Long = 30_000) {
        val ctrl = mediaController ?: return
        ctrl.seekTo((ctrl.currentPosition - ms).coerceAtLeast(0))
    }

    fun setSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun seekToChapter(chapter: Chapter) {
        mediaController?.seekTo((chapter.start * 1000).toLong())
    }

    override fun onCleared() {
        progressJob?.cancel()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}
