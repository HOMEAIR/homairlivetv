package com.homeair.live.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesMetadataAndSkipsInvalidUrls() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news1" tvg-name="News One" tvg-logo="https://cdn/logo.png" group-title="News" channel-number="101",News One
            https://example.com/news.m3u8
            not-a-url
            #EXTINF:-1 group-title="Sports",Sports Channel
            http://example.com/sports.ts
        """.trimIndent()
        val channels = M3uParser.parse(text)
        assertEquals(2, channels.size)
        assertEquals(101, channels[0].number)
        assertEquals("News One", channels[0].name)
        assertEquals("News", channels[0].group)
        assertEquals("https://cdn/logo.png", channels[0].logoUrl)
        assertTrue(channels[1].number > 0)
    }

    @Test
    fun duplicateUrlsAreRemoved() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id="same1",One
            https://example.com/live.m3u8
            #EXTINF:-1 tvg-id="same1",Two
            https://example.com/live.m3u8
        """.trimIndent()
        assertEquals(1, M3uParser.parse(text).size)
    }
}
