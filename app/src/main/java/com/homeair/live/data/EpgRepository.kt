package com.homeair.live.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class EpgRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    suspend fun fetch(url: String): Result<EpgSchedule> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/xml, text/xml, */*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("EPG HTTP ${response.code}")
                val body = response.body?.byteStream() ?: error("EPG response is empty")
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = false
                    runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                    runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                    runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                    runCatching { isXIncludeAware = false }
                    runCatching { isExpandEntityReferences = false }
                }
                val document = factory.newDocumentBuilder().parse(body)

                val programs = LinkedHashMap<String, MutableList<EpgProgram>>()
                val nodes = document.getElementsByTagName("programme")

                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as? org.w3c.dom.Element ?: continue
                    val channelId = element.getAttribute("channel").trim()
                    if (channelId.isBlank()) continue

                    val start = parseXmlTvTime(element.getAttribute("start")) ?: continue
                    val end = parseXmlTvTime(element.getAttribute("stop")) ?: continue
                    if (end <= start) continue

                    val title = childText(element, "title") ?: continue
                    val description = childText(element, "desc")

                    programs.getOrPut(channelId) { mutableListOf() }.add(
                        EpgProgram(channelId, title, description, start, end)
                    )
                }

                EpgSchedule(
                    programs.mapValues { (_, value) -> value.sortedBy { it.startEpochMs } }
                )
            }
        }
    }

    private fun childText(element: org.w3c.dom.Element, tag: String): String? =
        element.getElementsByTagName(tag).item(0)?.textContent?.trim()?.ifBlank { null }

    private fun parseXmlTvTime(value: String): Long? {
        val text = value.trim()
        if (text.isBlank()) return null

        val patterns = listOf(
            "yyyyMMddHHmmss Z",
            "yyyyMMddHHmmss"
        )

        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (!pattern.contains("Z")) {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }.parse(text)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
