package com.homeair.live.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "home_air_live")

class AppPreferences(private val context: Context) {
    private val modeKey = stringPreferencesKey("mode")
    private val playlistUrlKey = stringPreferencesKey("playlist_url")
    private val headerKey = stringPreferencesKey("token_header")
    private val lastChannelKey = stringPreferencesKey("last_channel_id")
    private val setupDoneKey = booleanPreferencesKey("setup_done")

    val mode: Flow<String?> = context.dataStore.data.map { it[modeKey] }
    val playlistUrl: Flow<String> = context.dataStore.data.map { it[playlistUrlKey] ?: "" }
    val tokenHeader: Flow<String> = context.dataStore.data.map { it[headerKey] ?: "Authorization: Bearer" }
    val lastChannelId: Flow<String?> = context.dataStore.data.map { it[lastChannelKey] }
    val setupDone: Flow<Boolean> = context.dataStore.data.map { it[setupDoneKey] ?: false }

    suspend fun setMode(value: String) = context.dataStore.edit { it[modeKey] = value }
    suspend fun setPlaylist(url: String, header: String) = context.dataStore.edit {
        it[playlistUrlKey] = url
        it[headerKey] = header
    }
    suspend fun setLastChannel(id: String) = context.dataStore.edit { it[lastChannelKey] = id }
    suspend fun markSetupDone() = context.dataStore.edit { it[setupDoneKey] = true }
    suspend fun getPlaylistUrl(): String = context.dataStore.data.first()[playlistUrlKey] ?: ""
    suspend fun getTokenHeader(): String = context.dataStore.data.first()[headerKey] ?: "Authorization: Bearer"
}
