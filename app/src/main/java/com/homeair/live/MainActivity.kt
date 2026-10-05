package com.homeair.live

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.media3.ui.PlayerView
import com.homeair.live.data.Channel
import com.homeair.live.data.ChannelFilters
import com.homeair.live.data.EpgSchedule
import com.homeair.live.network.WorkerConfig
import com.homeair.live.ui.ChannelLogo
import com.homeair.live.ui.EpgOverlay
import com.homeair.live.ui.LiveVideoPlayer
import com.homeair.live.ui.TvChannelSidebar
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private lateinit var homeAirVm: HomeAirViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        homeAirVm = androidx.lifecycle.ViewModelProvider(this)[HomeAirViewModel::class.java]
        setContent { HomeAirLiveApp(homeAirVm) }
    }
}

@Composable
fun HomeAirLiveApp(vm: HomeAirViewModel) {
    val mode by vm.mode.collectAsState()
    val channels by vm.channels.collectAsState()
    val error by vm.playlistError.collectAsState()
    val epg by vm.epg.collectAsState()
    val epgError by vm.epgError.collectAsState()
    val token by vm.token.collectAsState()
    val tokenHeader by vm.tokenHeader.collectAsState()
    val favorites by vm.favorites.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(channels, vm.lastChannelId) {
        if (channels.isNotEmpty()) {
            val lastId = vm.lastChannelId.value
            val restored = channels.indexOfFirst { it.id == lastId }
            selected = if (restored >= 0) restored else selected.coerceIn(0, channels.lastIndex)
        }
    }

    when {
        mode == null -> ModeChooser(vm::chooseMode)
        mode == "TV" -> TvScreen(
            channels = channels,
            current = selected,
            favorites = favorites,
            token = token,
            tokenHeader = tokenHeader,
            epg = epg,
            error = error,
            onSelect = {
                selected = it
                channels.getOrNull(it)?.let { channel -> vm.recordRecent(channel.id) }
            },
            onRefresh = vm::loadPlaylist,
            onSettings = { showSettings = true },
            onFavorite = vm::toggleFavorite,
            onModeChange = vm::chooseMode
        )
        else -> MobileScreen(
            channels = channels,
            current = selected,
            favorites = favorites,
            token = token,
            tokenHeader = tokenHeader,
            epg = epg,
            epgError = epgError,
            error = error,
            onSelect = {
                selected = it
                channels.getOrNull(it)?.let { channel -> vm.recordRecent(channel.id) }
            },
            onRefresh = vm::loadPlaylist,
            onSettings = { showSettings = true },
            onFavorite = vm::toggleFavorite,
            onModeChange = vm::chooseMode
        )
    }

    if (showSettings) {
        SettingsDialog(
            onRefresh = {
                vm.loadPlaylist()
                showSettings = false
            },
            onCancel = { showSettings = false }
        )
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home_air_logo")
    val triangleRotation by transition.animateFloat(
        0f, 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2200),
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "triangle"
    )
    val signalRotation by transition.animateFloat(
        0f, -360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1500),
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "signal"
    )

    Box(
        modifier = modifier.background(Color(0xFFFF7A00), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(9.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * .30f
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx + radius, cy)
                lineTo(cx - radius * .7f, cy - radius * .85f)
                lineTo(cx - radius * .7f, cy + radius * .85f)
                close()
            }
            rotate(triangleRotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
                drawPath(path, color = Color.White)
            }
            rotate(signalRotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
                drawLine(
                    Color(0xFFFFD54A),
                    start = androidx.compose.ui.geometry.Offset(cx - radius * .95f, cy + radius * .65f),
                    end = androidx.compose.ui.geometry.Offset(cx + radius * .95f, cy - radius * .65f),
                    strokeWidth = size.minDimension * .055f
                )
            }
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
            Spacer(Modifier.width(1.dp))
            Spacer(Modifier.size(18.dp))
            Text("Home Air Live", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17171A))
            Text("Choose your viewing mode", color = Color.DarkGray)
            Spacer(Modifier.size(28.dp))
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
        Modifier.width(280.dp).size(280.dp, 160.dp).clickable { onChoose(key) }.focusable(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f))
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFFF45100))
            Spacer(Modifier.size(8.dp))
            Text(subtitle, color = Color.DarkGray)
        }
    }
}

@Composable
fun TvScreen(
    channels: List<Channel>,
    current: Int,
    favorites: Set<String>,
    token: String,
    tokenHeader: String,
    epg: EpgSchedule?,
    error: String?,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onFavorite: (String) -> Unit,
    onModeChange: (String) -> Unit
) {
    var sidebar by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        sidebar = true
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> if (sidebar) {
                        sidebar = false
                        true
                    } else false
                    KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!sidebar && channels.isNotEmpty()) {
                            onSelect((current - 1 + channels.size) % channels.size)
                        }
                        true
                    }
                    KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!sidebar && channels.isNotEmpty()) {
                            onSelect((current + 1) % channels.size)
                        }
                        true
                    }
                    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                        val digit = it.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0
                        if (number.length < 4) number += digit.toString()
                        true
                    }
                    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                        if (number.isNotEmpty()) {
                            val idx = channels.indexOfFirst { it.number == number.toIntOrNull() }
                            if (idx >= 0) onSelect(idx)
                            number = ""
                        }
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
            Column(
                Modifier.align(Alignment.TopStart).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelLogo(channels[current].logoUrl, channels[current].name, Modifier.size(46.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("LIVE", color = Color(0xFFFFD54A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(channels[current].name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            EpgOverlay(
                channel = channels[current],
                epg = epg,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp, start = 18.dp, end = 18.dp)
            )
        } else {
            EmptyTvState(error, onRefresh)
        }

        if (number.isNotEmpty()) {
            Surface(
                Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = .76f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(number, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp))
            }
        }

        AnimatedVisibility(
            visible = sidebar,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            TvChannelSidebar(
                channels = channels,
                current = current,
                favorites = favorites,
                onSelect = onSelect,
                onFavorite = onFavorite,
                onClose = { sidebar = false },
                onRefresh = onRefresh,
                onSettings = onSettings
            )
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
fun EmptyTvState(error: String?, onRefresh: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0E0E10)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(Modifier.size(76.dp))
        Spacer(Modifier.size(16.dp))
        Text(error ?: "No live channels configured", color = Color.White, fontSize = 22.sp)
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRefresh) { Text("REFRESH") }
    }
}

@Composable
fun MobileScreen(
    channels: List<Channel>,
    current: Int,
    favorites: Set<String>,
    token: String,
    tokenHeader: String,
    epg: EpgSchedule?,
    epgError: String?,
    error: String?,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onFavorite: (String) -> Unit,
    onModeChange: (String) -> Unit
) {
    var showPlayer by remember { mutableStateOf(false) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var group by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val groups = remember(channels) { listOf("All", "Favorites") + ChannelFilters.groups(channels) }.distinct()
    val filtered = remember(channels, favorites, group, query) {
        ChannelFilters.filter(channels, favorites, group, query)
    }

    BackHandler(enabled = showPlayer) { showPlayer = false }

    if (showPlayer && channels.isNotEmpty()) {
        val channel = channels[current]
        Box(
            Modifier.fillMaxSize().background(Color.Black)
                .pointerInput(channels.size) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, amount -> change.consume(); dragTotal += amount },
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
            LiveVideoPlayer(channel, tokenHeader, token, Modifier.fillMaxSize())
            Row(Modifier.align(Alignment.TopStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(channel.logoUrl, channel.name, Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("LIVE", color = Color(0xFFFFD54A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(channel.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            EpgOverlay(
                channel = channel,
                epg = epg,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFF0E0E10)).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(Modifier.size(52.dp))
                Spacer(Modifier.width(12.dp))
                Text("Home Air Live", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search channels") },
                singleLine = true
            )
            Spacer(Modifier.size(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { item ->
                    OutlinedButton(onClick = { group = item }) {
                        Text(item, color = if (item == group) Color(0xFFFF7A00) else Color.White)
                    }
                }
            }
            Spacer(Modifier.size(10.dp))
            if (error != null) Text(error, color = Color(0xFFFFC107), fontSize = 14.sp)
            if (epgError != null) Text("EPG unavailable", color = Color(0xFFFFC107), fontSize = 12.sp)
            Spacer(Modifier.size(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { c ->
                    Card(
                        Modifier.fillMaxWidth().clickable {
                            val idx = channels.indexOfFirst { it.id == c.id }
                            if (idx >= 0) onSelect(idx)
                            showPlayer = true
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .07f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ChannelLogo(c.logoUrl, c.name, Modifier.size(54.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.number.toString() + "  " + c.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Text(c.group, color = Color.White.copy(alpha = .58f), fontSize = 12.sp)
                                epg?.current(c.tvgId ?: c.id)?.let {
                                    Text("NOW • " + it.title, color = Color(0xFFFFD54A), fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            Text(
                                if (c.id in favorites) "★" else "☆",
                                color = if (c.id in favorites) Color(0xFFFFD54A) else Color.White,
                                fontSize = 23.sp,
                                modifier = Modifier.clickable { onFavorite(c.id) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) { Text("REFRESH") }
                OutlinedButton(onClick = onSettings) { Text("SETTINGS") }
                OutlinedButton(onClick = { onModeChange("TV") }) { Text("TV MODE") }
            }
        }
    }
}

@Composable
fun SettingsDialog(onRefresh: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Home Air Live Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = WorkerConfig.proxyUrl,
                    onValueChange = {},
                    label = { Text("Cloudflare Worker") },
                    readOnly = true,
                    singleLine = true
                )
                Text(
                    "Playlist authentication is handled by the app's Worker configuration.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "EPG is loaded automatically when your M3U header provides x-tvg-url / url-tvg / tvg-url.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = { Button(onClick = onRefresh) { Text("REFRESH") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("CLOSE") } }
    )
}
