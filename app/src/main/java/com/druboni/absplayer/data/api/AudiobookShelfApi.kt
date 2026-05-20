package com.druboni.absplayer.data.api

import com.druboni.absplayer.data.api.model.*
import retrofit2.http.*

interface AudiobookShelfApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/libraries")
    suspend fun getLibraries(): LibrariesResponse

    @GET("api/libraries/{libraryId}/items")
    suspend fun getLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 0,
        @Query("sort") sort: String = "media.metadata.title"
    ): LibraryItemsResponse

    @GET("api/items/{itemId}")
    suspend fun getItem(@Path("itemId") itemId: String): LibraryItem

    @POST("api/items/{itemId}/play")
    suspend fun startPlaybackSession(
        @Path("itemId") itemId: String,
        @Body request: PlayItemRequest = PlayItemRequest()
    ): PlaybackSessionResponse

    @PATCH("api/session/{sessionId}/sync")
    suspend fun syncProgress(
        @Path("sessionId") sessionId: String,
        @Body request: ProgressUpdateRequest
    )

    @GET("api/me")
    suspend fun getMe(): MeResponse

    @PATCH("api/me/progress/{itemId}")
    suspend fun updateProgress(
        @Path("itemId") itemId: String,
        @Body request: ProgressUpdateRequest
    )

    @GET("api/me/progress/{itemId}")
    suspend fun getProgress(@Path("itemId") itemId: String): MediaProgressResponse
}
