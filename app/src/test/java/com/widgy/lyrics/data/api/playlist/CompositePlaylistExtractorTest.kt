package com.widgy.lyrics.data.api.playlist

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositePlaylistExtractorTest {

    private val composite = CompositePlaylistExtractor()

    @Test
    fun canHandle_acceptsAllSupportedPlatforms() {
        assertTrue(composite.canHandle("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertTrue(composite.canHandle("https://spotify.link/AbCdEfGh"))
        assertTrue(composite.canHandle("https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"))
        assertTrue(composite.canHandle("https://music.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"))
        assertTrue(composite.canHandle("https://www.deezer.com/playlist/3155776842"))
    }

    @Test
    fun canHandle_rejectsUnsupportedDomains() {
        assertFalse(composite.canHandle("https://example.com/playlist/123"))
        assertFalse(composite.canHandle("https://apple.music.com/playlist/123"))
    }
}
