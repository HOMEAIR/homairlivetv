package com.homeair.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.homeair.live.data.Channel
import com.homeair.live.data.ChannelFilters
import android.view.KeyEvent

@Composable
fun TvChannelSidebar(
    channels: List<Channel>,
    current: Int,
    favorites: Set<String>,
    onSelect: (Int) -> Unit,
    onFavorite: (String) -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    var group by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val groups = remember(channels) {
        listOf("All", "Favorites") + ChannelFilters.groups(channels)
    }.distinct()
    val filtered = remember(channels, favorites, group, query) {
        ChannelFilters.filter(channels, favorites, group, query)
    }
    val listState = rememberLazyListState()
    val currentId = channels.getOrNull(current)?.id
    val currentRequester = remember { FocusRequester() }

    LaunchedEffect(filtered, currentId) {
        val index = filtered.indexOfFirst { it.id == currentId }
        if (index >= 0) {
            listState.scrollToItem(index)
            kotlinx.coroutines.delay(80)
            runCatching { currentRequester.requestFocus() }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(440.dp)
            .focusGroup()
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onClose()
                        true
                    }
                    else -> false
                }
            },
        color = Color(0xF218181A)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMiniLogo()
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Live Channels", color = Color.White, fontSize = 25.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("${filtered.size} channels", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { showSearch = true }, modifier = Modifier.weight(1f).focusable()) {
                    Text(if (query.isBlank()) "SEARCH" else "SEARCH: $query")
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.focusable()) { Text("↻") }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                groups.take(7).forEach { item ->
                    val selected = item == group
                    TextButton(onClick = { group = item }, modifier = Modifier.focusable()) {
                        Text(item, color = if (selected) Color(0xFFFF7A00) else Color.White,
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filtered.isEmpty()) {
                    item { Text("No matching channels", color = Color.White.copy(alpha = .70f), modifier = Modifier.padding(20.dp)) }
                } else {
                    items(filtered, key = { it.id }) { channel ->
                        val sourceIndex = channels.indexOfFirst { it.id == channel.id }
                        ChannelSidebarRow(
                            channel = channel,
                            selected = sourceIndex == current,
                            favorite = channel.id in favorites,
                            modifier = if (sourceIndex == current) Modifier.focusRequester(currentRequester) else Modifier,
                            onSelect = { onSelect(sourceIndex) },
                            onFavorite = { onFavorite(channel.id) }
                        )
                    }
                }
            }

            Row {
                TextButton(onClick = onSettings, modifier = Modifier.focusable()) { Text("SETTINGS") }
                TextButton(onClick = onClose, modifier = Modifier.focusable()) { Text("CLOSE") }
            }
        }
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("Search channels") },
            text = {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Channel name or number") },
                    singleLine = true
                )
            },
            confirmButton = { Button(onClick = { showSearch = false }) { Text("DONE") } },
            dismissButton = { TextButton(onClick = { query = ""; showSearch = false }) { Text("CLEAR") } }
        )
    }
}

@Composable
private fun ChannelSidebarRow(
    channel: Channel,
    selected: Boolean,
    favorite: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.hasFocus }
            .background(
                when {
                    selected -> Color(0xFFFF7A00)
                    focused -> Color.White.copy(alpha = .15f)
                    else -> Color.White.copy(alpha = .05f)
                },
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onSelect)
            .focusable()
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(channel.logoUrl, channel.name, Modifier.width(46.dp).height(46.dp))
        Spacer(Modifier.width(10.dp))
        Text(channel.number.toString(), color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.width(42.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontSize = 16.sp, maxLines = 1)
            Text(channel.group, color = Color.White.copy(alpha = .60f), fontSize = 11.sp, maxLines = 1)
        }
        Text(if (favorite) "★" else "☆",
            color = if (favorite) Color(0xFFFFD54A) else Color.White,
            fontSize = 21.sp,
            modifier = Modifier.clickable(onClick = onFavorite))
    }
}

@Composable
private fun BrandMiniLogo() {
    androidx.compose.foundation.layout.Box(
        Modifier.width(50.dp).height(50.dp).background(Color(0xFFFF7A00), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("▶", color = Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}
