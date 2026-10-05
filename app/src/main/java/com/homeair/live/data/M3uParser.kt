package com.homeair.live.data

private fun attribute(line: String, name: String): String? {
    val regex = Regex(name + "=\\\"([^\\\"]*)\\\"", RegexOption.IGNORE_CASE)
    return regex.find(line)?.groupValues?.getOrNull(1)?.ifBlank { null }
}

object M3uParser {
    fun parse(text: String): List<Channel> {
        val result = ArrayList<Channel>()
        var metadataLine: String? = null
        var displayName: String? = null
        var generated = 1

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                metadataLine = line.substringBefore(',')
                displayName = line.substringAfter(',', "").trim().ifBlank { null }
                continue
            }

            if (line.isEmpty() || line.startsWith("#")) continue
            if (metadataLine != null && (line.startsWith("http://") || line.startsWith("https://"))) {
                val metadata = metadataLine!!
                val number = attribute(metadata, "channel-number")?.toIntOrNull()
                    ?: attribute(metadata, "tvg-chno")?.toIntOrNull()
                    ?: generated
                val name = attribute(metadata, "tvg-name") ?: displayName ?: "Channel $generated"
                result += Channel(
                    number = number,
                    id = attribute(metadata, "tvg-id") ?: "channel-${number}-${result.size}",
                    name = name,
                    url = line,
                    logoUrl = attribute(metadata, "tvg-logo"),
                    group = attribute(metadata, "group-title") ?: "Live",
                    tvgId = attribute(metadata, "tvg-id"),
                    tvgName = attribute(metadata, "tvg-name"),
                    userAgent = attribute(metadata, "http-user-agent"),
                    referrer = attribute(metadata, "http-referrer")
                )
                generated++
            }
            metadataLine = null
            displayName = null
        }

        return result.distinctBy { it.url }.sortedBy { it.number }
    }
}
