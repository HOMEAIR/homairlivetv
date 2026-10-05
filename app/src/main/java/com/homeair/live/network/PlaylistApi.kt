package com.homeair.live.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlaylistApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    suspend fun fetchPlaylist(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(WorkerConfig.appSecret.isNotBlank()) {
                "Home Air Worker secret is not configured"
            }

            val request = Request.Builder()
                .url(WorkerConfig.proxyUrl)
                .header(WorkerConfig.APP_SECRET_HEADER, WorkerConfig.appSecret)
                .header(
                    "Accept",
                    "application/vnd.apple.mpegurl, application/x-mpegURL, text/plain, */*"
                )
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Worker returned HTTP ${response.code}")
                }
                response.body?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IOException("Worker returned an empty playlist")
            }
        }
    }
}
