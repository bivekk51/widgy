package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePlaylistExtractorTest {

    private val extractor = YouTubePlaylistExtractor()

    @Test
    fun parseHtml_parsesPlaylistVideoRendererSuccessfully() {
        val htmlFixture = """
            <!DOCTYPE html>
            <html>
            <script>
            var ytInitialData = {
              "header": {
                "playlistHeaderRenderer": {
                  "title": { "simpleText": "Morning Coffee Acoustics" }
                }
              },
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "itemSectionRenderer": {
                                  "contents": [
                                    {
                                      "playlistVideoListRenderer": {
                                        "contents": [
                                          {
                                            "playlistVideoRenderer": {
                                              "title": { "runs": [{ "text": "Lady Gaga, Bruno Mars - Die With A Smile (Official Music Video)" }] },
                                              "shortBylineText": { "runs": [{ "text": "Lady Gaga" }] }
                                            }
                                          },
                                          {
                                            "playlistVideoRenderer": {
                                              "title": { "runs": [{ "text": "Radiohead - Daydreaming" }] },
                                              "shortBylineText": { "runs": [{ "text": "Radiohead - Topic" }] }
                                            }
                                          }
                                        ]
                                      }
                                    }
                                  ]
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            };</script>
            </html>
        """.trimIndent()

        val result = extractor.parseHtml(htmlFixture, "PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj")

        assertEquals("Morning Coffee Acoustics", result.playlistTitle)
        assertEquals(PlaylistPlatform.YOUTUBE, result.platform)
        assertEquals(2, result.tracks.size)
        assertEquals("Die With A Smile", result.tracks[0].trackName)
        assertEquals("Lady Gaga, Bruno Mars", result.tracks[0].artistName)
        assertEquals("Daydreaming", result.tracks[1].trackName)
        assertEquals("Radiohead", result.tracks[1].artistName)
    }

    @Test
    fun parseHtml_parsesModernLockupViewModelSuccessfully() {
        val htmlFixture = """
            <!DOCTYPE html>
            <html>
            <script>
            ytInitialData = {
              "contents": {
                "section": [
                  {
                    "contentImage": {},
                    "metadata": {
                      "lockupMetadataViewModel": {
                        "title": { "content": "Billie Eilish - Birds of a Feather" },
                        "metadata": {
                          "contentMetadataViewModel": {
                            "metadataRows": [
                              {
                                "metadataParts": [
                                  { "text": { "content": "Billie Eilish" } }
                                ]
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                ]
              }
            };</script>
            </html>
        """.trimIndent()

        val result = extractor.parseHtml(htmlFixture, "PL12345")
        assertEquals(1, result.tracks.size)
        assertEquals("Birds of a Feather", result.tracks[0].trackName)
        assertEquals("Billie Eilish", result.tracks[0].artistName)
    }

    @Test
    fun canHandle_matchesYouTubeAndYouTubeMusicUrls() {
        assertTrue(extractor.canHandle("https://www.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"))
        assertTrue(extractor.canHandle("https://music.youtube.com/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"))
        assertTrue(extractor.canHandle("https://youtu.be/playlist?list=PLMC9KNkIncKtPzgY-5rmhvj7fax8fdxoj"))
    }
}
