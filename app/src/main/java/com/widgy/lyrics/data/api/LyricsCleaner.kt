package com.widgy.lyrics.data.api

data class LyricLine(
    val index: Int,
    val text: String,
    val isHook: Boolean = false
)

object LyricsCleaner {
    private val timestampRegex = Regex("""\[\d{2}:\d{2}\.\d{2,3}\]""")
    private val lrcMetadataTagRegex = Regex("""^\[(ar|ti|al|by|length|offset|re|ve|id):[^\]]*\]$""", RegexOption.IGNORE_CASE)
    private val sectionHeaderRegex = Regex("""^\[(chorus|hook|refrain|verse|bridge|outro|intro|pre-chorus|post-chorus|instrumental)[^\]]*\]$""", RegexOption.IGNORE_CASE)
    private val hookMarkerRegex = Regex("""^\[(chorus|hook|refrain)[^\]]*\]$""", RegexOption.IGNORE_CASE)

    fun cleanLyrics(raw: String): List<LyricLine> {
        val rawLines = raw.lines()
        val result = mutableListOf<LyricLine>()
        var currentSectionIsHook = false

        for (line in rawLines) {
            val stripped = line.replace(timestampRegex, "").trim()
            if (stripped.isBlank()) {
                continue
            }

            // Skip standard LRC metadata tags like [ar:Kendrick Lamar], [ti:HUMBLE.]
            if (lrcMetadataTagRegex.matches(stripped)) {
                continue
            }

            if (sectionHeaderRegex.matches(stripped)) {
                currentSectionIsHook = hookMarkerRegex.matches(stripped)
                continue
            }

            result.add(
                LyricLine(
                    index = result.size,
                    text = stripped,
                    isHook = currentSectionIsHook
                )
            )
        }
        return result
    }
}
