package com.druboni.absplayer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.druboni.absplayer.data.local.dao.DownloadedBookDao
import com.druboni.absplayer.data.local.dao.DownloadedTrackDao
import com.druboni.absplayer.data.local.entity.DownloadedBookEntity
import com.druboni.absplayer.data.local.entity.DownloadedTrackEntity
import com.druboni.absplayer.data.repository.AudiobookRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AudiobookRepository,
    private val downloadedBookDao: DownloadedBookDao,
    private val downloadedTrackDao: DownloadedTrackDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_TITLE = "title"
        const val KEY_AUTHOR = "author"
        const val KEY_COVER_URL = "cover_url"
        const val KEY_DURATION = "duration"

        fun buildRequest(itemId: String, title: String, author: String?, coverUrl: String?, duration: Double): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_ITEM_ID to itemId,
                KEY_TITLE to title,
                KEY_AUTHOR to author,
                KEY_COVER_URL to coverUrl,
                KEY_DURATION to duration
            )
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val author = inputData.getString(KEY_AUTHOR)
        val coverUrl = inputData.getString(KEY_COVER_URL)
        val duration = inputData.getDouble(KEY_DURATION, 0.0)

        val item = repository.getItem(itemId).getOrNull() ?: return Result.failure()
        val serverUrl = repository.getServerUrl() ?: return Result.failure()
        val token = repository.getToken() ?: return Result.failure()

        val bookDir = File(context.filesDir, "books/$itemId").also { it.mkdirs() }

        val localCoverPath = if (coverUrl != null) {
            downloadFile(coverUrl, File(bookDir, "cover.jpg"), token)
        } else null

        val tracks = item.media.tracks ?: item.media.audioFiles?.mapIndexed { i, af ->
            com.druboni.absplayer.data.api.model.AudioTrack(
                index = i,
                startOffset = 0.0,
                duration = af.duration,
                title = af.metadata.filename,
                contentUrl = "$serverUrl/api/items/$itemId/file/${af.metadata.relPath}"
            )
        } ?: return Result.failure()

        tracks.forEachIndexed { idx, track ->
            val trackUrl = track.contentUrl?.let {
                if (it.startsWith("http")) it else "$serverUrl$it"
            } ?: return@forEachIndexed

            val ext = trackUrl.substringAfterLast('.').substringBefore('?').take(5).ifBlank { "mp3" }
            val localFile = File(bookDir, "track_${track.index}.$ext")
            downloadFile("$trackUrl?token=$token", localFile, token)

            downloadedTrackDao.insertTrack(
                DownloadedTrackEntity(
                    id = "${itemId}_${track.index}",
                    itemId = itemId,
                    index = track.index,
                    title = track.title,
                    localPath = localFile.absolutePath,
                    duration = track.duration,
                    startOffset = track.startOffset
                )
            )
            setProgress(workDataOf("progress" to (idx + 1) * 100 / tracks.size))
        }

        downloadedBookDao.insertBook(
            DownloadedBookEntity(
                itemId = itemId,
                title = title,
                author = author,
                coverPath = coverUrl,
                localCoverPath = localCoverPath,
                duration = duration
            )
        )

        return Result.success()
    }

    private fun downloadFile(url: String, dest: File, token: String): String? {
        if (dest.exists()) return dest.absolutePath
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.byteStream()?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
            }
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
