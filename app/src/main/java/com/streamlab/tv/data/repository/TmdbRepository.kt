package com.streamlab.tv.data.repository

import android.util.Log
import com.streamlab.tv.data.model.TmdbMovieResult
import com.streamlab.tv.data.model.TmdbSearchResponse
import com.streamlab.tv.data.remote.TmdbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TmdbMediaInfo(
    val title: String,
    val overview: String?,
    val posterUrl: String?
)

@Singleton
class TmdbRepository @Inject constructor(
    private val tmdbApi: TmdbApi
) {
    suspend fun searchPoster(apiKey: String, query: String): String? = withContext(Dispatchers.IO) {
        val info = searchMediaInfo(apiKey, query)
        return@withContext info?.posterUrl
    }

    suspend fun searchMediaInfo(apiKey: String, query: String): TmdbMediaInfo? = withContext(Dispatchers.IO) {
        val results = searchMediaList(apiKey, query)
        return@withContext results.firstOrNull()
    }

    suspend fun searchMediaList(apiKey: String, query: String): List<TmdbMediaInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || query.isBlank()) return@withContext emptyList()
        
        try {
            // Clean up IPTV tags like [FHD], (2023), 1080p, etc.
            val cleanQuery = query
                .replace(Regex("""\[.*?\]|\(.*?\)|4K|FHD|HD|1080p|720p|Dual|Dublado|Legendado""", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                .trim()
                .replace(Regex("""\s+"""), " ")

            val effectiveQuery = cleanQuery.ifEmpty { query.trim() }

            val response = tmdbApi.searchMulti(apiKey = apiKey, query = effectiveQuery)
            
            return@withContext response.results
                .filter { it.poster_path != null || !it.title.isNullOrEmpty() || !it.name.isNullOrEmpty() }
                .map { item ->
                    val name = item.title ?: item.name ?: query
                    val poster = item.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
                    TmdbMediaInfo(
                        title = name,
                        overview = item.overview,
                        posterUrl = poster
                    )
                }
        } catch (e: Exception) {
            Log.e("TmdbRepository", "Error fetching TMDB for '$query'", e)
        }
        return@withContext emptyList()
    }
}
