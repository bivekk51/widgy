package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult

class CompositePlaylistExtractor(
    private val extractors: List<PlaylistExtractor> = listOf(
        SpotifyPlaylistExtractor(),
        YouTubePlaylistExtractor(),
        DeezerPlaylistExtractor()
    )
) : PlaylistExtractor {

    override fun canHandle(url: String): Boolean {
        return extractors.any { it.canHandle(url) }
    }

    override suspend fun extract(url: String): Result<PlaylistExtractResult> {
        val extractor = extractors.firstOrNull { it.canHandle(url) }
            ?: return Result.failure(
                IllegalArgumentException("Unsupported playlist URL. Please provide a public Spotify, YouTube, or Deezer playlist link.")
            )

        return extractor.extract(url)
    }
}
