package com.streamlab.tv.data.remote

import com.streamlab.tv.data.model.TmdbSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "pt-BR"
    ): TmdbSearchResponse
}
