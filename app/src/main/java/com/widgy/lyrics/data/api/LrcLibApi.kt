package com.widgy.lyrics.data.api

import com.widgy.lyrics.data.model.LrcTrackDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/search")
    suspend fun searchTracks(@Query("q") query: String): List<LrcTrackDto>

    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LrcTrackDto
}
