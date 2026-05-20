package com.druboni.absplayer.di

import android.content.Context
import androidx.room.Room
import com.druboni.absplayer.data.api.AudiobookShelfApi
import com.druboni.absplayer.data.local.AppDatabase
import com.druboni.absplayer.data.local.dao.DownloadedBookDao
import com.druboni.absplayer.data.local.dao.DownloadedTrackDao
import com.druboni.absplayer.data.local.dao.LocalProgressDao
import com.druboni.absplayer.data.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(userPreferences: UserPreferences): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { userPreferences.token.first() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        // Dynamically replaces the placeholder base URL with the stored server URL.
        // Retrofit requires a compile-time base URL, so we use a placeholder and swap it here.
        val dynamicUrlInterceptor = Interceptor { chain ->
            val serverUrl = runBlocking { userPreferences.serverUrl.first() }
            if (serverUrl == null) return@Interceptor chain.proceed(chain.request())

            val parsedServerUrl = serverUrl.trimEnd('/').toHttpUrlOrNull()
                ?: return@Interceptor chain.proceed(chain.request())

            val newUrl = chain.request().url.newBuilder()
                .scheme(parsedServerUrl.scheme)
                .host(parsedServerUrl.host)
                .port(parsedServerUrl.port)
                .build()

            chain.proceed(chain.request().newBuilder().url(newUrl).build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://placeholder.invalid/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): AudiobookShelfApi = retrofit.create(AudiobookShelfApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "absplayer.db").build()

    @Provides fun provideDownloadedBookDao(db: AppDatabase): DownloadedBookDao = db.downloadedBookDao()
    @Provides fun provideDownloadedTrackDao(db: AppDatabase): DownloadedTrackDao = db.downloadedTrackDao()
    @Provides fun provideLocalProgressDao(db: AppDatabase): LocalProgressDao = db.localProgressDao()
}
