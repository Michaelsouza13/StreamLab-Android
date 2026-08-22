package com.streamlab.tv.data.repository

import android.util.Log
import com.streamlab.tv.data.local.ChannelDao
import com.streamlab.tv.data.local.PlaylistDao
import com.streamlab.tv.data.remote.M3uApi
import com.streamlab.tv.utils.M3uParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val m3uApi: M3uApi,
    private val channelDao: ChannelDao,
    private val playlistDao: PlaylistDao,
    private val tmdbRepository: TmdbRepository
) {
    suspend fun syncPlaylist(url: String, tmdbApiKey: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncRepository", "Downloading playlist from: $url")
            val responseBody = m3uApi.downloadPlaylist(url)
            val inputStream = responseBody.byteStream()

            Log.d("SyncRepository", "Parsing playlist...")
            val parsedChannels = M3uParser.parse(inputStream)

            if (parsedChannels.isNotEmpty()) {
                Log.d("SyncRepository", "Saving ${parsedChannels.size} channels to DB")
                channelDao.clearChannels()
                channelDao.insertChannels(parsedChannels)

                if (tmdbApiKey.isNotBlank()) {
                    // Fetch persisted channels with real IDs
                    val persisted = channelDao.getAllChannelsSync()
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.d("SyncRepository", "Starting background TMDB sync...")
                        persisted
                            .filter {
                                it.group.contains("filme", ignoreCase = true) ||
                                it.group.contains("serie", ignoreCase = true) ||
                                it.group.contains("série", ignoreCase = true)
                            }
                            .forEach { channel ->
                                val info = tmdbRepository.searchMediaInfo(tmdbApiKey, channel.name)
                                if (info?.posterUrl != null) {
                                    channelDao.updateChannel(channel.copy(posterUrl = info.posterUrl, description = info.overview))
                                }
                                delay(250)
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

    suspend fun syncPlaylistForId(
        playlistId: Long,
        url: String,
        tmdbApiKey: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncRepository", "Downloading playlist #$playlistId from: $url")
            val responseBody = m3uApi.downloadPlaylist(url)
            val inputStream = responseBody.byteStream()

            Log.d("SyncRepository", "Parsing playlist...")
            val parsedChannels = M3uParser.parse(inputStream)

            if (parsedChannels.isNotEmpty()) {
                Log.d("SyncRepository", "Saving ${parsedChannels.size} channels for playlist #$playlistId")
                channelDao.clearChannelsForPlaylist(playlistId)
                val channelsWithPlaylistId = parsedChannels.map { it.copy(playlistId = playlistId) }
                channelDao.insertChannels(channelsWithPlaylistId)

                if (tmdbApiKey.isNotBlank()) {
                    val persisted = channelDao.getAllChannelsSync().filter { it.playlistId == playlistId }
                    CoroutineScope(Dispatchers.IO).launch {
                        persisted
                            .filter {
                                it.group.contains("filme", ignoreCase = true) ||
                                it.group.contains("serie", ignoreCase = true) ||
                                it.group.contains("série", ignoreCase = true)
                            }
                            .forEach { channel ->
                                val info = tmdbRepository.searchMediaInfo(tmdbApiKey, channel.name)
                                if (info?.posterUrl != null) {
                                    channelDao.updateChannel(channel.copy(posterUrl = info.posterUrl, description = info.overview))
                                }
                                delay(250)
                            }
                    }
                }

                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error syncing playlist #$playlistId", e)
            return@withContext false
        }
    }

    fun bulkSyncPosters(apiKey: String): Flow<Pair<Int, Int>> = flow {
        val allChannels = channelDao.getAllChannelsSync()
        val toSync = allChannels.filter { it.posterUrl.isNullOrEmpty() }
        val total = toSync.size
        if (total == 0) {
            emit(0 to 0)
            return@flow
        }
        var done = 0
        var updated = 0
        for (channel in toSync) {
            val info = tmdbRepository.searchMediaInfo(apiKey, channel.name)
            if (info?.posterUrl != null) {
                channelDao.updateChannel(channel.copy(posterUrl = info.posterUrl, description = info.overview ?: channel.description))
                updated++
            }
            done++
            emit(done to total)
            delay(300)
        }
        Log.d("SyncRepository", "Bulk sync finished: $updated/$total updated")
    }
}
