package com.druboni.absplayer.data.api.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val user: UserResponse)

data class UserResponse(
    val id: String,
    val username: String,
    val token: String
)

data class LibrariesResponse(val libraries: List<Library>)

data class Library(
    val id: String,
    val name: String,
    val icon: String? = null
)

data class LibraryItemsResponse(
    val results: List<LibraryItem>,
    val total: Int,
    val limit: Int,
    val page: Int
)

data class LibraryItem(
    val id: String,
    val ino: String? = null,
    val libraryId: String? = null,
    val media: Media
)

data class Media(
    val metadata: Metadata,
    val coverPath: String? = null,
    val duration: Double? = null,
    val audioFiles: List<AudioFile>? = null,
    val chapters: List<Chapter>? = null,
    val tracks: List<AudioTrack>? = null
)

data class Metadata(
    val title: String,
    val authorName: String? = null,
    val description: String? = null,
    val seriesName: String? = null,
    val explicit: Boolean? = null,
    val abridged: Boolean? = null
)

data class AudioFile(
    val index: Int,
    val ino: String,
    val metadata: FileMetadata,
    val duration: Double,
    val bitRate: Int? = null,
    val mimeType: String? = null
)

data class FileMetadata(
    val filename: String,
    val ext: String,
    val path: String,
    val relPath: String,
    val size: Long
)

data class AudioTrack(
    val index: Int,
    val startOffset: Double,
    val duration: Double,
    val title: String? = null,
    val contentUrl: String? = null
)

data class Chapter(
    val id: Int,
    val start: Double,
    val end: Double,
    val title: String
)

data class PlaybackSessionResponse(
    val id: String,
    val userId: String,
    val libraryItemId: String,
    val audioTracks: List<AudioTrack>,
    val chapters: List<Chapter>,
    val duration: Double,
    val currentTime: Double
)

data class PlayItemRequest(
    val deviceInfo: DeviceInfo? = DeviceInfo(),
    val forceDirectPlay: Boolean = true,
    val forceTranscode: Boolean = false
)

data class DeviceInfo(
    val clientName: String = "ABSPlayer",
    val clientVersion: String = "1.0"
)

data class MediaProgressResponse(
    val id: String? = null,
    val libraryItemId: String,
    val currentTime: Double,
    val duration: Double,
    val progress: Double,
    val isFinished: Boolean,
    val lastUpdate: Long? = null
)

data class ProgressUpdateRequest(
    val currentTime: Double,
    val duration: Double,
    val progress: Double,
    val isFinished: Boolean
)

data class MeResponse(
    val id: String,
    val username: String,
    val mediaProgress: List<MediaProgressResponse>? = null
)
