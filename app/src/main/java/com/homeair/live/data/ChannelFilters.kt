package com.homeair.live.data

object ChannelFilters {
    fun groups(channels: List<Channel>): List<String> =
        channels.map { it.group.trim().ifBlank { "Live" } }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun filter(
        channels: List<Channel>,
        favorites: Set<String>,
        group: String,
        query: String
    ): List<Channel> {
        val normalizedQuery = query.trim()
        return channels.asSequence()
            .filter { channel ->
                when {
                    group == "All" -> true
                    group == "Favorites" -> channel.id in favorites
                    else -> (channel.group.trim().ifBlank { "Live" }) == group
                }
            }
            .filter {
                normalizedQuery.isBlank() ||
                    it.name.contains(normalizedQuery, ignoreCase = true) ||
                    it.number.toString().contains(normalizedQuery) ||
                    it.group.contains(normalizedQuery, ignoreCase = true)
            }
            .toList()
    }
}
