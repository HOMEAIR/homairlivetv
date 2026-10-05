package com.homeair.live.data

import com.homeair.live.network.PlaylistApi

class PlaylistRepository(
    private val api: PlaylistApi = PlaylistApi()
) {
    suspend fun load(): Result<ParsedPlaylist> {
        return api.fetchPlaylist().mapCatching(PlaylistParser::parse)
    }
}
