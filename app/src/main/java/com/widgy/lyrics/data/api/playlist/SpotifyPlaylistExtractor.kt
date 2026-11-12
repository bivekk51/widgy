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

class SpotifyPlaylistExtractor(
    private val client: OkHttpClient = SharedHttpClient.newBrowserClient()
) : PlaylistExtractor {

    private val playlistIdRegex = Regex("playlist/([a-zA-Z0-9]+)")
    private val nextDataRegex = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">([\\s\\S]*?)</script>")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("open.spotify.com/playlist") || lower.contains("spotify.link")
    }

    override suspend fun extract(url: String): Result<PlaylistExtractResult> = withContext(Dispatchers.IO) {
        try {
            val finalUrl = resolveRedirects(url)
            val playlistId = playlistIdRegex.find(finalUrl)?.groupValues?.get(1)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not extract Spotify playlist ID from: $url"))

            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(embedUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch Spotify embed page: HTTP ${response.code}"))
                }
                val html = response.body?.string().orEmpty()
                Result.success(parseHtml(html, playlistId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseHtml(html: String, fallbackPlaylistId: String): PlaylistExtractResult {
        val match = nextDataRegex.find(html)
            ?: throw IllegalArgumentException("Could not locate __NEXT_DATA__ in Spotify embed HTML")
        val jsonStr = match.groupValues[1]
        val root = json.parseToJsonElement(jsonStr).jsonObject

        val entity = root["props"]?.jsonObject
            ?.get("pageProps")?.jsonObject
            ?.get("state")?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("entity")?.jsonObject
            ?: throw IllegalArgumentException("Spotify JSON payload missing entity node")

        val title = entity["name"]?.jsonPrimitive?.contentOrNull
            ?: entity["title"]?.jsonPrimitive?.contentOrNull
            ?: "Spotify Playlist ($fallbackPlaylistId)"

        val tracks = mutableListOf<TrackImportQuery>()
        val trackList = entity["trackList"]?.jsonArray ?: entity["tracks"]?.jsonArray

        if (trackList != null) {
            for (element in trackList) {
                val item = element.jsonObject
                val trackName = item["title"]?.jsonPrimitive?.contentOrNull
                    ?: item["name"]?.jsonPrimitive?.contentOrNull
                    ?: ""

                var artistName = item["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                if (artistName.isBlank()) {
                    val artistsArray = item["artists"]?.jsonArray
                    if (artistsArray != null && artistsArray.isNotEmpty()) {
                        val names = artistsArray.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.trim() }
                        artistName = names.joinToString(", ")
                    }
                }

                if (trackName.isNotBlank() && artistName.isNotBlank()) {
                    tracks.add(
                        TrackImportQuery(
                            trackName = trackName.trim(),
                            artistName = artistName.trim()
                        )
                    )
                }
            }
        }

        return PlaylistExtractResult(
            playlistTitle = title,
            platform = PlaylistPlatform.SPOTIFY,
            tracks = tracks
        )
    }

    private fun resolveRedirects(url: String): String {
        val cleaned = SharedHttpClient.cleanPlaylistUrl(url)
        if (!cleaned.contains("spotify.link")) return cleaned

        val request = Request.Builder().url(cleaned).head().build()
        return try {
            client.newCall(request).execute().use { it.request.url.toString() }
        } catch (_: Exception) {
            cleaned
        }
    }
}
