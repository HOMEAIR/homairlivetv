package com.homeair.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ChannelLogo(url: String?, contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .10f)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            HomeAirMark()
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun HomeAirMark() {
    androidx.compose.material3.Text(
        "▶",
        color = Color.White,
        fontSize = androidx.compose.ui.unit.sp(18f)
    )
}
