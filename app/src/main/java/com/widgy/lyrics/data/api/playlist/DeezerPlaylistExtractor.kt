package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult
import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import com.widgy.lyrics.domain.model.playlist.TrackImportQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DeezerPlaylistExtractor(
    private val client: OkHttpClient = SharedHttpClient.baseClient
) : PlaylistExtractor {

    private val playlistIdRegex = Regex("playlist/([0-9]+)")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("deezer.com") || lower.contains("deezer.page.link")
    }

    override suspend fun extract(url: String): Result<PlaylistExtractResult> = withContext(Dispatchers.IO) {
        try {
            val finalUrl = resolveRedirects(url)
            val playlistId = playlistIdRegex.find(finalUrl)?.groupValues?.get(1)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not extract Deezer playlist ID from: $url"))

            val apiUrl = "https://api.deezer.com/playlist/$playlistId"
            val request = Request.Builder()
                .url(apiUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch Deezer playlist API: HTTP ${response.code}"))
                }
                val jsonStr = response.body?.string().orEmpty()
                Result.success(parseJson(jsonStr, playlistId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseJson(jsonStr: String, fallbackPlaylistId: String): PlaylistExtractResult {
        val root = json.parseToJsonElement(jsonStr).jsonObject
        if (root.containsKey("error")) {
            val errorMsg = root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: "Deezer API error"
            throw IllegalArgumentException(errorMsg)
        }

        val title = root["title"]?.jsonPrimitive?.contentOrNull
            ?: "Deezer Playlist ($fallbackPlaylistId)"

        val tracksArray = root["tracks"]?.jsonObject?.get("data")?.jsonArray
        val tracks = mutableListOf<TrackImportQuery>()

        if (tracksArray != null) {
            for (element in tracksArray) {
                val item = element.jsonObject
                val trackName = item["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val artistName = item["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val albumName = item["album"]?.jsonObject?.get("title")?.jsonPrimitive?.contentOrNull?.trim()
                val coverUrl = item["album"]?.jsonObject?.get("cover_big")?.jsonPrimitive?.contentOrNull
                    ?: item["album"]?.jsonObject?.get("cover_medium")?.jsonPrimitive?.contentOrNull

                if (trackName.isNotBlank() && artistName.isNotBlank()) {
                    tracks.add(
                        TrackImportQuery(
                            trackName = trackName,
                            artistName = artistName,
                            albumName = albumName,
                            coverUrl = coverUrl
                        )
                    )
                }
            }
        }

        return PlaylistExtractResult(
            playlistTitle = title,
            platform = PlaylistPlatform.DEEZER,
            tracks = tracks
        )
    }

    private fun resolveRedirects(url: String): String {
        val cleaned = SharedHttpClient.cleanPlaylistUrl(url)
        if (!cleaned.contains("deezer.page.link")) return cleaned

        val request = Request.Builder().url(cleaned).head().build()
        return try {
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (_: Exception) {
            cleaned
        }
    }
}
