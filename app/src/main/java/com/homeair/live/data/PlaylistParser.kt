package com.homeair.live.data

data class ParsedPlaylist(
    val channels: List<Channel>,
    val epgUrl: String? = null
)

object PlaylistParser {
    fun parse(text: String): ParsedPlaylist {
        val firstLine = text.lineSequence()
            .firstOrNull { it.trim().startsWith("#EXTM3U", ignoreCase = true) }
            .orEmpty()

        val epgUrl = listOf(
            attribute(firstLine, "x-tvg-url"),
            attribute(firstLine, "url-tvg"),
            attribute(firstLine, "tvg-url")
        ).firstOrNull { !it.isNullOrBlank() }

        return ParsedPlaylist(
            channels = M3uParser.parse(text),
            epgUrl = epgUrl
        )
    }
}

private fun attribute(line: String, name: String): String? {
    val regex = Regex("""$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    return regex.find(line)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null }
}
