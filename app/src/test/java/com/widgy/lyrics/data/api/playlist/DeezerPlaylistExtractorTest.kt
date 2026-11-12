package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerPlaylistExtractorTest {

    private val extractor = DeezerPlaylistExtractor()

    @Test
    fun parseJson_parsesDeezerRestPayloadSuccessfully() {
        val jsonFixture = """
            {
              "id": 3155776842,
              "title": "Top Worldwide",
              "tracks": {
                "data": [
                  {
                    "title": "Been By Now",
                    "artist": {
                      "name": "Morgan Wallen"
                    },
                    "album": {
                      "title": "Been By Now",
                      "cover_big": "https://cdn-images.dzcdn.net/images/cover/123/500x500.jpg"
                    }
                  },
                  {
                    "title": "Creep",
                    "artist": {
                      "name": "Radiohead"
                    },
                    "album": {
                      "title": "Pablo Honey",
                      "cover_big": "https://cdn-images.dzcdn.net/images/cover/456/500x500.jpg"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val result = extractor.parseJson(jsonFixture, "3155776842")

        assertEquals("Top Worldwide", result.playlistTitle)
        assertEquals(PlaylistPlatform.DEEZER, result.platform)
        assertEquals(2, result.tracks.size)
        assertEquals("Been By Now", result.tracks[0].trackName)
        assertEquals("Morgan Wallen", result.tracks[0].artistName)
        assertEquals("Been By Now", result.tracks[0].albumName)
        assertEquals("https://cdn-images.dzcdn.net/images/cover/123/500x500.jpg", result.tracks[0].coverUrl)
    }

    @Test
    fun canHandle_matchesDeezerUrls() {
        assertTrue(extractor.canHandle("https://www.deezer.com/playlist/3155776842"))
        assertTrue(extractor.canHandle("https://deezer.page.link/AbCdEf"))
    }
}
