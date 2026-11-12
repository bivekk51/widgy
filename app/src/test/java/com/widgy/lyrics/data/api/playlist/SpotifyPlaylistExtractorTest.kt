package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistExtractorTest {

    private val extractor = SpotifyPlaylistExtractor()

    @Test
    fun parseHtml_parsesEmbedPayloadSuccessfully() {
        val htmlFixture = """
            <!DOCTYPE html>
            <html>
            <head>
            <script id="__NEXT_DATA__" type="application/json">
            {
              "props": {
                "pageProps": {
                  "state": {
                    "data": {
                      "entity": {
                        "name": "Late Night Vibes",
                        "trackList": [
                          {
                            "title": "Stop The Wedding!",
                            "subtitle": "Ashe",
                            "duration": 198000
                          },
                          {
                            "title": "Daydreaming",
                            "subtitle": "Radiohead",
                            "duration": 384000
                          }
                        ]
                      }
                    }
                  }
                }
              }
            }
            </script>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val result = extractor.parseHtml(htmlFixture, "37i9dQZF1DXcBWIGoYBM5M")

        assertEquals("Late Night Vibes", result.playlistTitle)
        assertEquals(PlaylistPlatform.SPOTIFY, result.platform)
        assertEquals(2, result.tracks.size)
        assertEquals("Stop The Wedding!", result.tracks[0].trackName)
        assertEquals("Ashe", result.tracks[0].artistName)
        assertEquals("Daydreaming", result.tracks[1].trackName)
        assertEquals("Radiohead", result.tracks[1].artistName)
    }

    @Test
    fun canHandle_matchesSpotifyUrls() {
        assertTrue(extractor.canHandle("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertTrue(extractor.canHandle("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abcdef123"))
        assertTrue(extractor.canHandle("https://spotify.link/AbCdEfGh"))
    }
}
