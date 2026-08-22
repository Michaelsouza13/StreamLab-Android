package com.streamlab.tv.data.model

data class TmdbSearchResponse(
    val results: List<TmdbMovieResult>
)

data class TmdbMovieResult(
    val id: Int,
    val title: String?,
    val name: String?, // For TV shows
    val overview: String?,
    val poster_path: String?
)
