package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistExtractResult
import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import com.widgy.lyrics.domain.model.playlist.TrackImportQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class YouTubePlaylistExtractor(
    private val client: OkHttpClient = SharedHttpClient.newBrowserClient()
) : PlaylistExtractor {

    private val listIdRegex = Regex("[?&]list=([a-zA-Z0-9_-]+)")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return (lower.contains("youtube.com") || lower.contains("youtu.be")) &&
                (lower.contains("list=") || lower.contains("/playlist"))
    }

    override suspend fun extract(url: String): Result<PlaylistExtractResult> = withContext(Dispatchers.IO) {
        try {
            val listId = listIdRegex.find(url)?.groupValues?.get(1)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not extract YouTube playlist ID from: $url"))

            val playlistUrl = "https://www.youtube.com/playlist?list=$listId"
            val request = Request.Builder()
                .url(playlistUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch YouTube playlist: HTTP ${response.code}"))
                }
                val html = response.body?.string().orEmpty()
                Result.success(parseHtml(html, listId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseHtml(html: String, fallbackPlaylistId: String): PlaylistExtractResult {
        val token = "ytInitialData"
        val tokenIndex = html.indexOf(token)
        if (tokenIndex == -1) {
            throw IllegalArgumentException("Could not locate ytInitialData in YouTube HTML")
        }

        val openBrace = html.indexOf('{', tokenIndex)
        if (openBrace == -1) {
            throw IllegalArgumentException("Could not locate opening brace for ytInitialData")
        }

        val scriptEnd = html.indexOf("</script>", openBrace)
        val jsonSlice = if (scriptEnd != -1) {
            html.substring(openBrace, scriptEnd).trim().trimEnd(';')
        } else {
            html.substring(openBrace).trim().trimEnd(';')
        }

        val root = json.parseToJsonElement(jsonSlice).jsonObject

        val title = extractTitle(root).ifBlank { "YouTube Playlist ($fallbackPlaylistId)" }
        val rawVideos = mutableListOf<Pair<String, String>>()
        findVideosRecursive(root, rawVideos)

        val tracks = rawVideos.mapNotNull { (rawTitle, channel) ->
            val (cleanTitle, cleanArtist) = YouTubeTitleCleaner.clean(rawTitle, channel)
            if (cleanTitle.isNotBlank() && cleanArtist.isNotBlank()) {
                TrackImportQuery(trackName = cleanTitle, artistName = cleanArtist)
            } else null
        }

        return PlaylistExtractResult(
            playlistTitle = title,
            platform = PlaylistPlatform.YOUTUBE,
            tracks = tracks
        )
    }

    private fun extractTitle(root: JsonObject): String {
        root["header"]?.jsonObject
            ?.get("playlistHeaderRenderer")?.jsonObject
            ?.get("title")?.jsonObject
            ?.get("simpleText")?.jsonPrimitive?.contentOrNull
            ?.let { if (it.isNotBlank()) return it }

        root["metadata"]?.jsonObject
            ?.get("playlistMetadataRenderer")?.jsonObject
            ?.get("title")?.jsonPrimitive?.contentOrNull
            ?.let { if (it.isNotBlank()) return it }

        root["microformat"]?.jsonObject
            ?.get("microformatDataRenderer")?.jsonObject
            ?.get("title")?.jsonPrimitive?.contentOrNull
            ?.let { if (it.isNotBlank()) return it }

        return ""
    }

    private fun findVideosRecursive(node: JsonElement?, out: MutableList<Pair<String, String>>) {
        when (node) {
            is JsonObject -> {
                // Check classic playlistVideoRenderer
                val pvr = node["playlistVideoRenderer"]?.jsonObject
                if (pvr != null) {
                    val title = pvr["title"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val channel = pvr["shortBylineText"]?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (title.isNotBlank()) {
                        out.add(title to channel)
                    }
                }

                // Check modern lockupViewModel
                if (node.containsKey("contentImage") && node.containsKey("metadata")) {
                    val meta = node["metadata"]?.jsonObject?.get("lockupMetadataViewModel")?.jsonObject
                    if (meta != null) {
                        val title = meta["title"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                            ?: meta["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val channel = meta["metadata"]?.jsonObject
                            ?.get("contentMetadataViewModel")?.jsonObject
                            ?.get("metadataRows")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("metadataParts")?.jsonArray
                            ?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty()
                        if (title.isNotBlank()) {
                            out.add(title to channel)
                        }
                    }
                }

                for (entry in node.entries) {
                    findVideosRecursive(entry.value, out)
                }
            }
            is JsonArray -> {
                for (element in node) {
                    findVideosRecursive(element, out)
                }
            }
            else -> {}
        }
    }
}
