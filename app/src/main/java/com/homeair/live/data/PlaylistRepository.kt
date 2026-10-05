package com.homeair.live.data

import com.homeair.live.network.PlaylistApi

class PlaylistRepository(
    private val api: PlaylistApi = PlaylistApi()
) {
    suspend fun load(): Result<List<Channel>> {
        return api.fetchPlaylist().mapCatching(M3uParser::parse)
    }
}
