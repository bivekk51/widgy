package com.widgy.lyrics.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ArtworkClient(
    private val client: OkHttpClient = defaultClient()
) {
    suspend fun fetchArtworkUrl(title: String, artist: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val query = "$title $artist".trim()
            if (query.isBlank()) return@runCatching null

            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "WidgyAndroid/1.0 (https://github.com/widgy)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val body = response.body?.string() ?: return@runCatching null
                parseArtworkUrlFromJson(body)
            }
        }
    }

    suspend fun fetchArtworkBytes(artworkUrl: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(artworkUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Failed to download artwork: HTTP ${response.code}")
                response.body?.bytes() ?: throw IllegalStateException("Empty artwork response body")
            }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }

        fun parseArtworkUrlFromJson(responseBody: String): String? {
            return runCatching {
                val element = json.parseToJsonElement(responseBody).jsonObject
                val results = element["results"]?.jsonArray ?: return null
                if (results.isEmpty()) return null
                val first = results[0].jsonObject
                val art100 = first["artworkUrl100"]?.jsonPrimitive?.content ?: return null
                // Upgrade 100x100 to 600x600 for high density mobile rendering
                art100.replace("100x100bb.jpg", "600x600bb.jpg")
            }.getOrNull()
        }
    }
}
