package com.homeair.live.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.homeair.live.data.Channel
import com.homeair.live.network.WorkerConfig

class PlaybackFactory(private val context: Context) {

    fun create(channel: Channel, tokenHeader: String = "", token: String = ""): ExoPlayer {
        val headers = buildMap {
            WorkerConfig.appSecret
                .takeIf { it.isNotBlank() }
                ?.let { put(WorkerConfig.APP_SECRET_HEADER, it) }

            if (token.isNotBlank() && tokenHeader.contains(":")) {
                val separator = tokenHeader.indexOf(':')
                val name = tokenHeader.substring(0, separator).trim()
                val prefix = tokenHeader.substring(separator + 1).trim()
                if (name.isNotBlank()) {
                    put(name, if (prefix.isBlank()) token else "$prefix $token")
                }
            }

            channel.userAgent?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
            channel.referrer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
        }

        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 45_000, 1_500, 3_000)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()
            .also { player ->
                player.setMediaItem(MediaItem.fromUri(channel.url))
                player.prepare()
                player.playWhenReady = true
            }
    }
}
