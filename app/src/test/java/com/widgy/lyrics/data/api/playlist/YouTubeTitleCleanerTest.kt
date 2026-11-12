package com.widgy.lyrics.data.api.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeTitleCleanerTest {

    @Test
    fun clean_standardArtistTitleWithOfficialVideo() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "Lady Gaga, Bruno Mars - Die With A Smile (Official Music Video)",
            channelByline = "Lady Gaga"
        )
        assertEquals("Die With A Smile", title)
        assertEquals("Lady Gaga, Bruno Mars", artist)
    }

    @Test
    fun clean_withFeaturingInsideTitle() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "DJ Snake - Let Me Love You (Official Music Video) ft. Justin Bieber",
            channelByline = "DJ Snake"
        )
        assertEquals("Let Me Love You", title)
        assertEquals("DJ Snake", artist)
    }

    @Test
    fun clean_withBracketedFeaturingWithoutLeavingDanglingParens() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "Billie Eilish - Lovely (feat. Khalid) [Official Audio]",
            channelByline = "Billie Eilish"
        )
        assertEquals("Lovely", title)
        assertEquals("Billie Eilish", artist)
    }

    @Test
    fun clean_withUnicodeJapaneseBracketsAndEnDash() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "米津玄師 – Lemon 【MV】",
            channelByline = "Kenshi Yonezu"
        )
        assertEquals("Lemon", title)
        assertEquals("米津玄師", artist)
    }

    @Test
    fun clean_withDevanagariNepaliMusic() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "सज्जन राज वैद्य — हताशिन (Official Lyrical Video)",
            channelByline = "Sajjan Raj Vaidya"
        )
        assertEquals("हताशिन", title)
        assertEquals("सज्जन राज वैद्य", artist)
    }

    @Test
    fun clean_whenNoSeparator_fallsBackToCleanedChannelByline() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "Bohemian Rhapsody (Remastered 2011)",
            channelByline = "Queen Official - Topic"
        )
        assertEquals("Bohemian Rhapsody", title)
        assertEquals("Queen", artist)
    }

    @Test
    fun clean_stripsVevoAndTopicSuffixFromChannel() {
        assertEquals("Taylor Swift", YouTubeTitleCleaner.cleanChannel("TaylorSwiftVEVO"))
        assertEquals("Adele", YouTubeTitleCleaner.cleanChannel("Adele - Topic"))
        assertEquals("Kendrick Lamar", YouTubeTitleCleaner.cleanChannel("Kendrick Lamar Official"))
    }

    @Test
    fun clean_trimsUnicodeQuotesAndBrackets() {
        val (title, artist) = YouTubeTitleCleaner.clean(
            rawTitle = "Artist - “Song Title”",
            channelByline = "Artist"
        )
        assertEquals("Song Title", title)
        assertEquals("Artist", artist)

        val (cjkTitle, _) = YouTubeTitleCleaner.clean(
            rawTitle = "Artist - 《夜に駆ける》",
            channelByline = "Artist"
        )
        assertEquals("夜に駆ける", cjkTitle)
    }
}
