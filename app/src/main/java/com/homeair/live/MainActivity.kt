package com.homeair.live

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.homeair.live.data.Channel
import com.homeair.live.playback.PlaybackFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HomeAirLiveApp() }
    }
}

@Composable
fun HomeAirLiveApp(vm: HomeAirViewModel = viewModel()) {
    val mode by vm.mode.collectAsState()
    val channels by vm.channels.collectAsState()
    val error by vm.playlistError.collectAsState()
    val token by vm.token.collectAsState()
    val tokenHeader by vm.tokenHeader.collectAsState()
    val playlistUrl by vm.playlistUrl.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) selected = selected.coerceIn(0, channels.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
    when {
        mode == null -> ModeChooser { vm.chooseMode(it) }
        mode == "TV" -> TvScreen(
            channels = channels,
            current = selected,
            token = token,
            tokenHeader = tokenHeader,
            error = error,
            onSelect = { selected = it },
            onMode = { vm.chooseMode(it) },
            onRefresh = vm::loadPlaylist,
            onSettings = { showSettings = true }
        )
        else -> MobileScreen(
            channels = channels,
            current = selected,
            token = token,
            tokenHeader = tokenHeader,
            error = error,
            onSelect = { selected = it },
            onMode = { vm.chooseMode(it) },
            onRefresh = vm::loadPlaylist,
            onSettings = { showSettings = true }
        )
    }
    if (showSettings) SettingsDialog(playlistUrl, tokenHeader, token, onSave = { url, header, newToken -> vm.savePlaylist(url, header, newToken); showSettings = false }, onCancel = { showSettings = false })
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home_air_logo")
    val triangleRotation by transition.animateFloat(0f, 360f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2200), androidx.compose.animation.core.RepeatMode.Restart), label = "triangle")
    val signalRotation by transition.animateFloat(0f, -360f, animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1500), androidx.compose.animation.core.RepeatMode.Restart), label = "signal")
    Box(
        modifier = modifier.background(Color(0xFFFF7A00), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * .30f
            drawContext.canvas.save()
            drawContext.canvas.rotate(triangleRotation, cx, cy)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx + radius, cy)
                lineTo(cx - radius * .7f, cy - radius * .85f)
                lineTo(cx - radius * .7f, cy + radius * .85f)
                close()
            }
            drawPath(path, color = Color.White)
            drawContext.canvas.restore()
            drawContext.canvas.save()
            drawContext.canvas.rotate(signalRotation, cx, cy)
            drawLine(Color(0xFFFFD54A), start = androidx.compose.ui.geometry.Offset(cx - radius * .95f, cy + radius * .65f), end = androidx.compose.ui.geometry.Offset(cx + radius * .95f, cy - radius * .65f), strokeWidth = size.minDimension * .055f)
            drawContext.canvas.restore()
        }
    }
}

@Composable
fun ModeChooser(onChoose: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFFF7A00), Color.White)))) {
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandLogo(Modifier.size(84.dp))
            Spacer(Modifier.height(18.dp))
            Text("Home Air Live", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17171A))
            Text("Choose your viewing mode", color = Color.DarkGray)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ModeCard("TV MODE", "Full-screen live TV", onChoose)
                ModeCard("MOBILE MODE", "Dashboard & touch", onChoose)
            }
        }
    }
}

@Composable
fun ModeCard(title: String, subtitle: String, onChoose: (String) -> Unit) {
    val key = if (title.startsWith("TV")) "TV" else "MOBILE"
    Card(
        Modifier.width(280.dp).height(160.dp).clickable { onChoose(key) }.focusable(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f))
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFF45100))
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color.DarkGray)
        }
    }
}

@Composable
fun TvScreen(
    channels: List<Channel>,
    current: Int,
    token: String,
    tokenHeader: String,
    error: String?,
    onSelect: (Int) -> Unit,
    onMode: (String) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    var sidebar by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent {
                if (it.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { sidebar = true; true }
                    KeyEvent.KEYCODE_BACK -> if (sidebar) { sidebar = false; true } else false
                    KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                        if (channels.isNotEmpty()) onSelect((current - 1 + channels.size) % channels.size); true
                    }
                    KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (channels.isNotEmpty()) onSelect((current + 1) % channels.size); true
                    }
                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        val digit = it.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0
                        if (number.length < 4) number += digit.toString()
                        true
                    }
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                        val idx = channels.indexOfFirst { it.number == number.toIntOrNull() }
                        if (idx >= 0) onSelect(idx)
                        number = ""
                        true
                    }
                    else -> false
                }
            }
            .focusRequester(focus)
            .focusable()
    ) {
        if (channels.isNotEmpty()) {
            LiveVideoPlayer(channels[current], tokenHeader, token, Modifier.fillMaxSize())
            Text("LIVE  " + channels[current].name, color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).padding(24.dp))
            Text(channels[current].name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(28.dp))
        } else {
            EmptyTvState(error, onRefresh)
        }
        if (number.isNotEmpty()) {
            Surface(Modifier.align(Alignment.Center), color = Color.Black.copy(alpha = .72f), shape = RoundedCornerShape(12.dp)) {
                Text(number, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp))
            }
        }
        if (sidebar) {
            ChannelSidebar(channels, current, onSelect = { onSelect(it); sidebar = false }, onRefresh = onRefresh, onMode = { onMode("MOBILE") }, onSettings = onSettings)
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
fun EmptyTvState(error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0E0E10)), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandLogo(Modifier.size(76.dp))
        Spacer(Modifier.height(16.dp))
        Text(error ?: "No live channels configured", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRefresh) { Text("REFRESH") }
        Text("Configure your playlist in the app settings/backend configuration.", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun ChannelSidebar(
    channels: List<Channel>,
    current: Int,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
    onMode: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(Modifier.fillMaxHeight().width(400.dp), color = Color(0xF218181A)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(Modifier.size(50.dp))
                Spacer(Modifier.width(12.dp))
                Text("Channels", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(channels, key = { it.id }) { c ->
                    val i = channels.indexOf(c)
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (i == current) Color(0xFFFF7A00) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { onSelect(i) }
                            .focusable()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.number.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Text(c.name, color = Color.White, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("REFRESH PLAYLIST") }
            Row {
                TextButton(onClick = onSettings) { Text("SETTINGS") }
                TextButton(onClick = onMode) { Text("MOBILE MODE") }
            }
        }
    }
}

@Composable
fun MobileScreen(
    channels: List<Channel>,
    current: Int,
    token: String,
    tokenHeader: String,
    error: String?,
    onSelect: (Int) -> Unit,
    onMode: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showPlayer by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    if (showPlayer && channels.isNotEmpty()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black)
                .pointerInput(channels.size, current) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragTotal < -120f -> onSelect((current + 1) % channels.size)
                                dragTotal > 120f -> onSelect((current - 1 + channels.size) % channels.size)
                            }
                            dragTotal = 0f
                        }
                    )
                }
        ) {
            LiveVideoPlayer(channels[current], tokenHeader, token, Modifier.fillMaxSize())
            Text("LIVE  " + channels[current].name, color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).padding(20.dp))
            Text("Swipe ↑ / ↓  •  Tap system back to return", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp))
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFF0E0E10)).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Text("Home Air Live", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text("Live TV", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (error != null) Text(error, color = Color(0xFFFFC107), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(channels, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { onSelect(channels.indexOf(c)); showPlayer = true }) {
                        Text(c.number.toString() + "  " + c.name, Modifier.padding(20.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("REFRESH") }
                OutlinedButton(onClick = onSettings) { Text("SETTINGS") }
                OutlinedButton(onClick = { onMode("TV") }) { Text("TV MODE") }
            }
        }
    }
}

@Composable
fun LiveVideoPlayer(channel: Channel, tokenHeader: String, token: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(channel.id, tokenHeader, token) {
        PlaybackFactory(context.applicationContext).create(channel, tokenHeader, token)
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { PlayerView(it).apply { useController = false } },
        update = { it.player = player }
    )
}


@Composable
fun SettingsDialog(playlistUrl: String, tokenHeader: String, token: String, onSave: (String, String, String) -> Unit, onCancel: () -> Unit) {
    var url by remember(playlistUrl) { mutableStateOf(playlistUrl) }
    var header by remember(tokenHeader) { mutableStateOf(tokenHeader) }
    var newToken by remember(token) { mutableStateOf(token) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Streaming Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(url, { url = it }, label = { Text("M3U playlist URL") }, singleLine = true)
                OutlinedTextField(header, { header = it }, label = { Text("Auth header format") }, singleLine = true)
                OutlinedTextField(newToken, { newToken = it }, label = { Text("Access token") }, singleLine = true)
                Text("Example header: Authorization: Bearer", fontSize = 12.sp, color = Color.Gray)
                Text("Token is stored using Android Keystore. Prefer short-lived tokens.", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url.trim(), header.trim(), newToken) }) { Text("SAVE & LOAD") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("CANCEL") } }
    )
}
