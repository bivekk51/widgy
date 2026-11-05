package com.widgy.lyrics.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsCleanerTest {
    @Test
    fun cleanLyrics_stripsTimestampsAndMetadataHeaders() {
        val raw = "[00:12.34]First line\n[Chorus]\n[00:18.00]Second line"
        val cleaned = LyricsCleaner.cleanLyrics(raw)
        assertEquals(listOf("First line", "Second line"), cleaned.map { it.text })
        assertTrue(cleaned[1].isHook)
    }

    @Test
    fun cleanLyrics_handlesEmptyLinesAndTrimming() {
        val raw = "\n  [01:05.12]  Some line   \n\n[Verse 1]\nAnother line\n"
        val cleaned = LyricsCleaner.cleanLyrics(raw)
        assertEquals(listOf("Some line", "Another line"), cleaned.map { it.text })
    }

    @Test
    fun cleanLyrics_stripsLrcMetadataTags() {
        val raw = "[ar:Kendrick Lamar]\n[ti:HUMBLE.]\n[al:DAMN.]\n[by:Spotify]\n[00:10.00]Nobody pray for me\n[00:12.00]Even a day for me"
        val cleaned = LyricsCleaner.cleanLyrics(raw)
        assertEquals(listOf("Nobody pray for me", "Even a day for me"), cleaned.map { it.text })
    }
}
