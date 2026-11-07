package com.widgy.lyrics.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkClientTest {

    @Test
    fun parseArtworkUrl_validJson_extractsAndUpgradesResolution() {
        val json = """
            {
                "resultCount": 1,
                "results": [
                    {
                        "trackName": "HUMBLE.",
                        "artistName": "Kendrick Lamar",
                        "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/100x100bb.jpg"
                    }
                ]
            }
        """.trimIndent()

        val parsedUrl = ArtworkClient.parseArtworkUrlFromJson(json)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/600x600bb.jpg", parsedUrl)
    }

    @Test
    fun parseArtworkUrl_emptyResults_returnsNull() {
        val json = """{"resultCount": 0, "results": []}"""
        val parsedUrl = ArtworkClient.parseArtworkUrlFromJson(json)
        assertNull(parsedUrl)
    }
}
