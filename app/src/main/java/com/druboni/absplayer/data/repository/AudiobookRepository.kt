package com.druboni.absplayer.data.repository

import com.druboni.absplayer.data.api.AudiobookShelfApi
import com.druboni.absplayer.data.api.model.*
import com.druboni.absplayer.data.local.dao.DownloadedBookDao
import com.druboni.absplayer.data.local.dao.DownloadedTrackDao
import com.druboni.absplayer.data.local.dao.LocalProgressDao
import com.druboni.absplayer.data.local.entity.LocalProgressEntity
import com.druboni.absplayer.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepository @Inject constructor(
    private val api: AudiobookShelfApi,
    private val prefs: UserPreferences,
    private val downloadedBookDao: DownloadedBookDao,
    private val downloadedTrackDao: DownloadedTrackDao,
    private val localProgressDao: LocalProgressDao
) {
    suspend fun login(serverUrl: String, username: String, password: String): Result<UserResponse> {
        return runCatching {
            val response = api.login(LoginRequest(username, password))
            prefs.saveCredentials(serverUrl, response.user.token, response.user.id, response.user.username)
            response.user
        }
    }

    suspend fun logout() = prefs.clearCredentials()

    suspend fun getLibraries(): Result<List<Library>> = runCatching {
        api.getLibraries().libraries
    }

    suspend fun getLibraryItems(libraryId: String, page: Int = 0): Result<LibraryItemsResponse> = runCatching {
        api.getLibraryItems(libraryId, page = page)
    }

    suspend fun getItem(itemId: String): Result<LibraryItem> = runCatching {
        api.getItem(itemId)
    }

    suspend fun startPlaybackSession(itemId: String): Result<PlaybackSessionResponse> = runCatching {
        api.startPlaybackSession(itemId)
    }

    suspend fun syncProgress(sessionId: String, currentTime: Double, duration: Double): Result<Unit> = runCatching {
        val progress = if (duration > 0) currentTime / duration else 0.0
        api.syncProgress(sessionId, ProgressUpdateRequest(currentTime, duration, progress, progress >= 0.99))
    }

    suspend fun updateProgress(itemId: String, currentTime: Double, duration: Double): Result<Unit> = runCatching {
        val progress = if (duration > 0) currentTime / duration else 0.0
        val isFinished = progress >= 0.99
        api.updateProgress(itemId, ProgressUpdateRequest(currentTime, duration, progress, isFinished))
        localProgressDao.upsertProgress(LocalProgressEntity(itemId, currentTime, duration, progress, isFinished))
    }

    suspend fun getProgress(itemId: String): Result<MediaProgressResponse> = runCatching {
        api.getProgress(itemId)
    }

    fun getDownloadedBooks() = downloadedBookDao.getAllBooks()

    suspend fun getDownloadedTracks(itemId: String) = downloadedTrackDao.getTracksForBook(itemId)

    suspend fun getServerUrl() = prefs.serverUrl.first()
    suspend fun getToken() = prefs.token.first()
}
