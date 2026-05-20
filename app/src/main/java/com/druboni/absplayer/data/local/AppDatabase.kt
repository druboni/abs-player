package com.druboni.absplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.druboni.absplayer.data.local.dao.DownloadedBookDao
import com.druboni.absplayer.data.local.dao.DownloadedTrackDao
import com.druboni.absplayer.data.local.dao.LocalProgressDao
import com.druboni.absplayer.data.local.entity.DownloadedBookEntity
import com.druboni.absplayer.data.local.entity.DownloadedTrackEntity
import com.druboni.absplayer.data.local.entity.LocalProgressEntity

@Database(
    entities = [DownloadedBookEntity::class, DownloadedTrackEntity::class, LocalProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedBookDao(): DownloadedBookDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao
    abstract fun localProgressDao(): LocalProgressDao
}
