package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharedHttpClientTest {

    @Test
    fun extractUrlFromText_extractsCleanUrlFromCompositeShareString() {
        val spotifyShare = "Check out this playlist on Spotify: https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345"
        val extracted = SharedHttpClient.extractUrlFromText(spotifyShare)
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345", extracted)
    }

    @Test
    fun extractUrlFromText_handlesParenthesesAndTrailingPunctuation() {
        val textWithPunctuation = "Listen here (https://youtu.be/playlist?list=PL123456)."
        val extracted = SharedHttpClient.extractUrlFromText(textWithPunctuation)
        assertEquals("https://youtu.be/playlist?list=PL123456", extracted)
    }

    @Test
    fun extractUrlFromText_returnsNullOnBlankOrNoUrl() {
        assertNull(SharedHttpClient.extractUrlFromText(null))
        assertNull(SharedHttpClient.extractUrlFromText("   "))
        assertNull(SharedHttpClient.extractUrlFromText("Just listening to music"))
    }

    @Test
    fun cleanPlaylistUrl_stripsTrackingParamsForSpotify() {
        val dirtySpotify = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abcdef123&pt=456"
        val cleaned = SharedHttpClient.cleanPlaylistUrl(dirtySpotify)
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M", cleaned)
    }

    @Test
    fun cleanPlaylistUrl_preservesListParamForYouTube() {
        val dirtyYt = "https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj&si=abc12345"
        val cleaned = SharedHttpClient.cleanPlaylistUrl(dirtyYt)
        assertEquals("https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj", cleaned)
    }

    @Test
    fun detectPlatform_identifiesSupportedPlatforms() {
        assertEquals(
            PlaylistPlatform.SPOTIFY,
            SharedHttpClient.detectPlatform("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")
        )
        assertEquals(
            PlaylistPlatform.SPOTIFY,
            SharedHttpClient.detectPlatform("https://spotify.link/AbCdEfGh")
        )
        assertEquals(
            PlaylistPlatform.YOUTUBE,
            SharedHttpClient.detectPlatform("https://www.youtube.com/playlist?list=PL123456")
        )
        assertEquals(
            PlaylistPlatform.YOUTUBE,
            SharedHttpClient.detectPlatform("https://music.youtube.com/playlist?list=PL123456")
        )
        assertEquals(
            PlaylistPlatform.DEEZER,
            SharedHttpClient.detectPlatform("https://www.deezer.com/playlist/3155776842")
        )
        assertEquals(
            PlaylistPlatform.UNKNOWN,
            SharedHttpClient.detectPlatform("https://example.com/not-supported")
        )
    }
}
