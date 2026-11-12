package com.widgy.lyrics.domain.model.playlist

enum class PlaylistPlatform {
    SPOTIFY,
    YOUTUBE,
    DEEZER,
    UNKNOWN
}

data class TrackImportQuery(
    val trackName: String,
    val artistName: String,
    val albumName: String? = null,
    val coverUrl: String? = null
)

data class PlaylistExtractResult(
    val playlistTitle: String,
    val platform: PlaylistPlatform,
    val tracks: List<TrackImportQuery>
)

sealed class PlaylistImportState {
    data object Idle : PlaylistImportState()
    data class Extracting(val platform: PlaylistPlatform) : PlaylistImportState()
    data class FetchingLyrics(
        val playlistTitle: String,
        val current: Int,
        val total: Int,
        val currentTrack: String,
        val matchedCount: Int
    ) : PlaylistImportState()
    data class Success(
        val playlistTitle: String,
        val totalTracks: Int,
        val importedCount: Int,
        val skippedCount: Int
    ) : PlaylistImportState()
    data class Cancelled(
        val playlistTitle: String,
        val importedCount: Int
    ) : PlaylistImportState()
    data class Error(val message: String) : PlaylistImportState()
}
