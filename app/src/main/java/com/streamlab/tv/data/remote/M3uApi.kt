package com.streamlab.tv.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface M3uApi {
    @GET
    suspend fun downloadPlaylist(@Url url: String): ResponseBody
}
