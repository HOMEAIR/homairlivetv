package com.homeair.live.data

private val attrRegex = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

object M3uParser {
    fun parse(text: String): List<Channel> {
        val result = ArrayList<Channel>()
        var pending = mutableMapOf<String, String>()
        var pendingName: String? = null
        var generated = 1
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.indexOf(',')
                    val metadata = if (comma >= 0) line.substring(0, comma) else line
                    pendingName = if (comma >= 0) line.substring(comma + 1).trim().ifBlank { null } else null
                    pending = attrRegex.findAll(metadata).associate { it.groupValues[1] to it.groupValues[2] }.toMutableMap()
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val name = pending["tvg-name"] ?: pendingName ?: "Channel $generated"
                    val number = pending["channel-number"]?.toIntOrNull() ?: pending["tvg-chno"]?.toIntOrNull() ?: generated
                    result += Channel(
                        number = number,
                        id = pending["tvg-id"]?.ifBlank { null } ?: "channel-${number}-${result.size}",
                        name = name,
                        url = line,
                        logoUrl = pending["tvg-logo"]?.ifBlank { null },
                        group = pending["group-title"]?.ifBlank { null } ?: "Live",
                        tvgId = pending["tvg-id"]?.ifBlank { null },
                        tvgName = pending["tvg-name"]?.ifBlank { null },
                        userAgent = pending["http-user-agent"]?.ifBlank { null },
                        referrer = pending["http-referrer"]?.ifBlank { null }
                    )
                    generated++
                    pending = mutableMapOf()
                    pendingName = null
                }
            }
        }
        return result.filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
            .distinctBy { it.id }
            .sortedBy { it.number }
    }
}
