package com.homeair.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.homeair.live.data.AppPreferences
import com.homeair.live.data.Channel
import com.homeair.live.data.PlaylistRepository
import com.homeair.live.security.SecureTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeAirViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = AppPreferences(app)
    private val tokenStore = SecureTokenStore(app)
    private val repository = PlaylistRepository(preferences, tokenStore)

    private val _mode = MutableStateFlow<String?>(null)
    val mode: StateFlow<String?> = _mode.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _playlistError = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = _playlistError.asStateFlow()

    private val _token = MutableStateFlow(tokenStore.read())
    val token: StateFlow<String> = _token.asStateFlow()

    private val _tokenHeader = MutableStateFlow("Authorization: Bearer")
    val tokenHeader: StateFlow<String> = _tokenHeader.asStateFlow()

    init {
        viewModelScope.launch {
            _mode.value = preferences.mode.firstOrNull()
            _tokenHeader.value = preferences.getTokenHeader()
            loadPlaylist()
        }
    }

    fun chooseMode(value: String) {
        _mode.value = value
        viewModelScope.launch { preferences.setMode(value) }
    }

    fun savePlaylist(url: String, header: String, token: String) {
        tokenStore.save(token)
        _token.value = tokenStore.read()
        _tokenHeader.value = header.ifBlank { "Authorization: Bearer" }
        viewModelScope.launch {
            preferences.setPlaylist(url.trim(), _tokenHeader.value)
            loadPlaylist()
        }
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            val url = preferences.getPlaylistUrl()
            if (url.isBlank()) {
                _channels.value = emptyList()
                _playlistError.value = null
                return@launch
            }
            repository.load().fold(
                onSuccess = {
                    _channels.value = it
                    _playlistError.value = if (it.isEmpty()) "Playlist loaded but contains no valid channels." else null
                },
                onFailure = {
                    _playlistError.value = it.message ?: "Unable to load playlist."
                }
            )
        }
    }
}
