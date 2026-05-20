package com.druboni.absplayer.data.local.dao

import androidx.room.*
import com.druboni.absplayer.data.local.entity.DownloadedBookEntity
import com.druboni.absplayer.data.local.entity.DownloadedTrackEntity
import com.druboni.absplayer.data.local.entity.LocalProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedBookDao {
    @Query("SELECT * FROM downloaded_books ORDER BY title")
    fun getAllBooks(): Flow<List<DownloadedBookEntity>>

    @Query("SELECT * FROM downloaded_books WHERE itemId = :itemId")
    suspend fun getBook(itemId: String): DownloadedBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: DownloadedBookEntity)

    @Delete
    suspend fun deleteBook(book: DownloadedBookEntity)
}

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks WHERE itemId = :itemId ORDER BY `index`")
    suspend fun getTracksForBook(itemId: String): List<DownloadedTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE itemId = :itemId")
    suspend fun deleteTracksForBook(itemId: String)
}

@Dao
interface LocalProgressDao {
    @Query("SELECT * FROM local_progress WHERE itemId = :itemId")
    suspend fun getProgress(itemId: String): LocalProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: LocalProgressEntity)
}
