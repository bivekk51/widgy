package com.widgy.lyrics.data.api.playlist

object YouTubeTitleCleaner {

    // Unicode brackets: ASCII (), [], full-width 【】, 「」, （）, ［］
    private val NOISE_TAGS = Regex(
        "(?i)\\s*[\\(\\[【「（［](official\\s*)?(music\\s*|lyric(al)?\\s*)?(video|audio|visualizer|mv|hd|4k|lyrics|remastered(\\s*\\d+)?)[\\)\\]】」）］]"
    )

    // Bracketed featuring expressions, e.g. (feat. Drake), [ft. Justin Bieber]
    private val BRACKETED_FEATURING = Regex(
        "(?i)\\s*[\\(\\[【「（［](ft\\.|feat\\.|featuring)\\s+[^\\)\\]】」）］]+[\\)\\]】」）］]"
    )

    // Trailing unbracketed featuring expressions, e.g. "ft. Justin Bieber"
    private val UNBRACKETED_FEATURING = Regex(
        "(?i)\\s+(ft\\.|feat\\.|featuring)\\s+.*"
    )

    // Unicode dashes: ASCII -, En-dash –, Em-dash —, Minus sign −, Fullwidth hyphen －, Pipe |
    private val ARTIST_SEPARATOR = Regex("\\s+[\\-–—−－|]\\s+")

    // Channel noise suffixes
    private val CHANNEL_SUFFIXES = listOf(
        Regex("(?i)\\s*-\\s*Topic$"),
        Regex("(?i)\\s*-\\s*VEVO$"),
        Regex("(?i)VEVO$"),
        Regex("(?i)\\s+Official$"),
        Regex("(?i)\\s+Music$")
    )

    private val CAMEL_CASE_SPLIT = Regex("(?<=[a-z])(?=[A-Z])")

    /**
     * Cleans a raw YouTube video title and channel byline into a Pair of (TrackTitle, ArtistName).
     */
    fun clean(rawTitle: String, channelByline: String): Pair<String, String> {
        var cleaned = rawTitle
            .replace(NOISE_TAGS, "")
            .replace(BRACKETED_FEATURING, "")
            .replace(UNBRACKETED_FEATURING, "")
            .trim()

        val parts = cleaned.split(ARTIST_SEPARATOR, limit = 2)
        return if (parts.size == 2) {
            val artist = cleanArtist(parts[0])
            val title = cleanTitle(parts[1])
            title to artist
        } else {
            val title = cleanTitle(cleaned)
            val artist = cleanChannel(channelByline)
            title to artist
        }
    }

    fun cleanChannel(rawChannel: String): String {
        var channel = rawChannel.trim()
        val hadVevo = channel.endsWith("VEVO", ignoreCase = true)
        for (pattern in CHANNEL_SUFFIXES) {
            channel = channel.replace(pattern, "").trim()
        }
        if (hadVevo && !channel.contains(' ')) {
            channel = channel.replace(CAMEL_CASE_SPLIT, " ").trim()
        }
        return channel
    }

    private val TRIMMABLE_CHARS = charArrayOf(
        ' ', '"', '\'', '-', '–', '—', ':', '.',
        '‘', '’', '“', '”', '《', '》', '『', '』', '「', '」', '।', '॥'
    )

    private fun cleanTitle(title: String): String {
        return title
            .replace(NOISE_TAGS, "")
            .replace(BRACKETED_FEATURING, "")
            .replace(UNBRACKETED_FEATURING, "")
            .trim(*TRIMMABLE_CHARS)
    }

    private fun cleanArtist(artist: String): String {
        return cleanChannel(artist)
            .trim(*TRIMMABLE_CHARS)
    }
}
