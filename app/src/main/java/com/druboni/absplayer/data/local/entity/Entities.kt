package com.druboni.absplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_books")
data class DownloadedBookEntity(
    @PrimaryKey val itemId: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val localCoverPath: String?,
    val duration: Double,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val index: Int,
    val title: String?,
    val localPath: String,
    val duration: Double,
    val startOffset: Double
)

@Entity(tableName = "local_progress")
data class LocalProgressEntity(
    @PrimaryKey val itemId: String,
    val currentTime: Double,
    val duration: Double,
    val progress: Double,
    val isFinished: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
