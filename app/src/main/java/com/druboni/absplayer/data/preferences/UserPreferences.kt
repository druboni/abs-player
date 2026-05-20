package com.druboni.absplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[serverUrlKey] }
    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val userId: Flow<String?> = context.dataStore.data.map { it[userIdKey] }
    val username: Flow<String?> = context.dataStore.data.map { it[usernameKey] }

    suspend fun saveCredentials(serverUrl: String, token: String, userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[serverUrlKey] = serverUrl
            prefs[tokenKey] = token
            prefs[userIdKey] = userId
            prefs[usernameKey] = username
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.remove(serverUrlKey)
            prefs.remove(tokenKey)
            prefs.remove(userIdKey)
            prefs.remove(usernameKey)
        }
    }
}
