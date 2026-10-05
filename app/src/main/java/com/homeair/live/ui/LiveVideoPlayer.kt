package com.homeair.live.ui

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.homeair.live.data.Channel
import com.homeair.live.playback.PlaybackFactory
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun LiveVideoPlayer(
    channel: Channel,
    tokenHeader: String,
    token: String,
    modifier: Modifier = Modifier,
    onPlaying: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(channel.id, tokenHeader, token) {
        PlaybackFactory(context.applicationContext).create(channel, tokenHeader, token)
    }

    var state by remember(channel.id) { mutableIntStateOf(Player.STATE_BUFFERING) }
    var errorText by remember(channel.id) { mutableStateOf<String?>(null) }
    var reconnectTrigger by remember(channel.id) { mutableIntStateOf(0) }
    var reconnectAttempt by remember(channel.id) { mutableIntStateOf(0) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state = playbackState
                if (playbackState == Player.STATE_READY) {
                    errorText = null
                    reconnectAttempt = 0
                    onPlaying?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorText = "Live stream interrupted"
                if (reconnectAttempt < 5) {
                    reconnectAttempt += 1
                    reconnectTrigger += 1
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, reconnectTrigger) {
        if (reconnectTrigger == 0) return@LaunchedEffect
        val backoff = min(8_000L, 1_500L * (1L shl (reconnectAttempt - 1).coerceAtLeast(0)))
        delay(backoff)
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(player) {
        while (true) {
            delay(1_000)
            if (state == Player.STATE_READY && !player.isPlaying && player.playWhenReady) {
                if (reconnectAttempt < 5) {
                    reconnectAttempt += 1
                    reconnectTrigger += 1
                }
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    useController = false
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view -> view.player = player }
        )

        val showReconnect = errorText != null || state == Player.STATE_BUFFERING
        AnimatedVisibility(
            visible = showReconnect,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = .72f),
                tonalElevation = 6.dp
            ) {
                Column(
                    Modifier.widthIn(min = 250.dp, max = 360.dp).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    HomeAirSpinner()
                    Text(
                        if (errorText != null) "Reconnecting to live stream…" else "Buffering…",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    if (reconnectAttempt > 0) {
                        Text(
                            "Attempt $reconnectAttempt of 5",
                            color = Color(0xFFFFD54A),
                            fontSize = 13.sp
                        )
                    }
                    if (reconnectAttempt >= 5) {
                        Button(onClick = {
                            reconnectAttempt = 0
                            errorText = null
                            player.prepare()
                            player.playWhenReady = true
                        }) {
                            Text("RETRY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAirSpinner() {
    androidx.compose.material3.CircularProgressIndicator(
        color = Color(0xFFFF7A00),
        strokeWidth = 3.dp
    )
}
