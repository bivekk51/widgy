package com.widgy.lyrics.data.api

import com.widgy.lyrics.data.model.LrcTrackDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class LrcLibClient(
    baseUrl: String = "https://lrclib.net/",
    private val okHttpClient: OkHttpClient = defaultOkHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val api: LrcLibApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(LrcLibApi::class.java)

    suspend fun searchTracks(query: String): Result<List<LrcTrackDto>> {
        return runCatching {
            api.searchTracks(query)
        }
    }

    suspend fun getLyrics(trackName: String, artistName: String): Result<LrcTrackDto> {
        return runCatching {
            api.getLyrics(trackName, artistName)
        }
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "WidgyAndroid/1.0 (https://github.com/widgy)")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
    }
}
