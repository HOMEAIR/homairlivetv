package com.homeair.live.data

data class PlaylistData(
    val channels: List<Channel>,
    val epgUrl: String? = null
)
