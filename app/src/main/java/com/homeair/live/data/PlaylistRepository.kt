package com.homeair.live.data

import com.homeair.live.security.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

class PlaylistRepository(
    private val preferences: AppPreferences,
    private val tokenStore: SecureTokenStore
) {
    suspend fun load(): Result<List<Channel>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = preferences.getPlaylistUrl().trim()
            require(endpoint.startsWith("https://") || endpoint.startsWith("http://")) { "Invalid playlist URL" }
            val connection = URI(endpoint).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            val header = preferences.getTokenHeader().trim()
            val token = tokenStore.read()
            if (token.isNotBlank() && header.contains(":")) {
                val separator = header.indexOf(':')
                val name = header.substring(0, separator).trim()
                val prefix = header.substring(separator + 1).trim()
                connection.setRequestProperty(name, if (prefix.isBlank()) token else "$prefix $token")
            }
            connection.connect()
            if (connection.responseCode !in 200..299) error("Playlist HTTP ${connection.responseCode}")
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            M3uParser.parse(text)
        }
    }
}
