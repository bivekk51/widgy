package com.widgy.lyrics.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LrcTrackDto(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("trackName") val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
) {
    val displayTitle: String
        get() = trackName ?: name ?: "Unknown Title"
}
