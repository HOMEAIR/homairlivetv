package com.homeair.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homeair.live.data.Channel
import com.homeair.live.data.EpgSchedule
import kotlinx.coroutines.delay

@Composable
fun EpgOverlay(channel: Channel, epg: EpgSchedule?, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(channel.id, epg) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    val channelKey = channel.tvgId ?: channel.id
    val current = epg?.current(channelKey, now)
    val next = epg?.next(channelKey, now)

    if (current == null && next == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = .70f))
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        current?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NOW", color = Color(0xFFFFD54A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(it.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            }
            LinearProgressIndicator(
                progress = { it.progressAt(now) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFF7A00),
                trackColor = Color.White.copy(alpha = .20f)
            )
        }
        next?.let {
            Text("NEXT  •  ${it.title}", color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
        }
    }
}
