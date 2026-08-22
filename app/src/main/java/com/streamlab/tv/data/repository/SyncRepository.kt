package com.streamlab.tv.data.repository

import android.util.Log
import com.streamlab.tv.data.local.ChannelDao
import com.streamlab.tv.data.local.ChannelEntity
import com.streamlab.tv.data.remote.M3uApi
import com.streamlab.tv.utils.M3uParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val m3uApi: M3uApi,
    private val channelDao: ChannelDao,
    private val tmdbRepository: TmdbRepository
) {
    suspend fun syncPlaylist(url: String, tmdbApiKey: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncRepository", "Downloading playlist from: \$url")
            val responseBody = m3uApi.downloadPlaylist(url)
            val inputStream = responseBody.byteStream()
            
            Log.d("SyncRepository", "Parsing playlist...")
            val parsedChannels = M3uParser.parse(inputStream)
            
            if (parsedChannels.isNotEmpty()) {
                Log.d("SyncRepository", "Saving \${parsedChannels.size} channels to DB")
                channelDao.clearChannels()
                channelDao.insertChannels(parsedChannels)

                // TMDB Background Sync for Movies and Series
                if (tmdbApiKey.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d("SyncRepository", "Starting background TMDB sync...")
                        parsedChannels
                            .filter { 
                                it.group.contains("filme", ignoreCase = true) || 
                                it.group.contains("serie", ignoreCase = true) ||
                                it.group.contains("série", ignoreCase = true)
                            }
                            .forEach { channel ->
                                val poster = tmdbRepository.searchPoster(tmdbApiKey, channel.name)
                                if (poster != null) {
                                    channelDao.updateChannel(channel.copy(posterUrl = poster))
                                }
                            }
                        Log.d("SyncRepository", "Background TMDB sync finished.")
                    }
                }
                
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error syncing playlist", e)
            return@withContext false
        }
    }
}
