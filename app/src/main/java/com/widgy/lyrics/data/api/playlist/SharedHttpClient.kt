package com.widgy.lyrics.data.api.playlist

import com.widgy.lyrics.domain.model.playlist.PlaylistPlatform
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SharedHttpClient {

    private val URL_REGEX = Regex("https?://[^\\s<>\"'\\[\\]()]+")

    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun newBrowserClient(): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    )
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text)?.value ?: return null
        // Strip trailing punctuation like '.', ',', ')', ']'
        return match.trimEnd('.', ',', ')', ']', ';', '!')
    }

    fun cleanPlaylistUrl(url: String): String {
        val platform = detectPlatform(url)
        return when (platform) {
            PlaylistPlatform.SPOTIFY -> {
                // Strip query parameters (?si=..., etc.)
                url.substringBefore('?')
            }
            PlaylistPlatform.YOUTUBE -> {
                // Keep the 'list' parameter and remove others like si=, index=
                val listId = if (url.contains("list=")) {
                    url.substringAfter("list=").substringBefore('&').substringBefore('#')
                } else null

                if (!listId.isNullOrBlank()) {
                    val base = if (url.contains("music.youtube.com")) {
                        "https://music.youtube.com/playlist"
                    } else {
                        "https://www.youtube.com/playlist"
                    }
                    "$base?list=$listId"
                } else {
                    url
                }
            }
            PlaylistPlatform.DEEZER -> {
                url.substringBefore('?')
            }
            PlaylistPlatform.UNKNOWN -> url
        }
    }

    fun detectPlatform(url: String): PlaylistPlatform {
        val lower = url.lowercase()
        return when {
            lower.contains("spotify.com") || lower.contains("spotify.link") -> PlaylistPlatform.SPOTIFY
            lower.contains("youtube.com") || lower.contains("youtu.be") -> PlaylistPlatform.YOUTUBE
            lower.contains("deezer.com") || lower.contains("deezer.page.link") -> PlaylistPlatform.DEEZER
            else -> PlaylistPlatform.UNKNOWN
        }
    }
}
