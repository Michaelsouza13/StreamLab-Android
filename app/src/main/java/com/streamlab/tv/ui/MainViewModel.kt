package com.streamlab.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlab.tv.data.local.ChannelDao
import com.streamlab.tv.data.local.ChannelEntity
import com.streamlab.tv.data.local.PlaylistDao
import com.streamlab.tv.data.local.PlaylistEntity
import com.streamlab.tv.data.repository.SettingsRepository
import com.streamlab.tv.data.repository.SyncRepository
import com.streamlab.tv.data.repository.TmdbMediaInfo
import com.streamlab.tv.data.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val channelDao: ChannelDao,
    private val playlistDao: PlaylistDao,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    private val _activePlaylistId = MutableStateFlow<Long>(0L)

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activePlaylist: StateFlow<PlaylistEntity?> = playlistDao.getActivePlaylist()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val channels: StateFlow<List<ChannelEntity>> = _activePlaylistId.flatMapLatest { playlistId ->
        if (playlistId > 0) {
            channelDao.getChannelsByPlaylist(playlistId)
        } else {
            channelDao.getAllChannels()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentChannel: StateFlow<ChannelEntity?> = _currentChannel.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _tmdbSearchResults = MutableStateFlow<List<TmdbMediaInfo>>(emptyList())
    val tmdbSearchResults: StateFlow<List<TmdbMediaInfo>> = _tmdbSearchResults.asStateFlow()

    private val _isTmdbSearching = MutableStateFlow(false)
    val isTmdbSearching: StateFlow<Boolean> = _isTmdbSearching.asStateFlow()

    private val _tmdbApiKey = MutableStateFlow("")
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    init {
        viewModelScope.launch {
            val key = settingsRepository.tmdbKeyFlow.first()
            _tmdbApiKey.value = key
        }
        viewModelScope.launch {
            settingsRepository.activePlaylistIdFlow.collect { id ->
                _activePlaylistId.value = id
            }
        }
    }

    fun syncDefaultPlaylistIfNeeded() {
        viewModelScope.launch {
            val existingPlaylists = playlists.value
            if (existingPlaylists.isEmpty()) {
                val m3uUrl = settingsRepository.m3uUrlFlow.first()
                val effectiveUrl = if (m3uUrl.isBlank()) SettingsRepository.DEFAULT_M3U_URL else m3uUrl
                val tmdbKey = settingsRepository.tmdbKeyFlow.first()

                _isSyncing.value = true
                val playlistId = playlistDao.insertPlaylist(
                    PlaylistEntity(
                        name = "Padrão",
                        url = effectiveUrl,
                        isDefault = true,
                        isActive = true
                    )
                )
                settingsRepository.saveActivePlaylistId(playlistId)
                _activePlaylistId.value = playlistId

                val currentChannels = channelDao.getChannelsByPlaylist(playlistId).first()
                if (currentChannels.isEmpty()) {
                    syncRepository.syncPlaylistForId(playlistId, effectiveUrl, tmdbKey)
                }
                _isSyncing.value = false
            }
        }
    }

    fun switchPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deactivateAll()
            playlistDao.setActive(playlistId)
            settingsRepository.saveActivePlaylistId(playlistId)
            _activePlaylistId.value = playlistId
        }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(
                PlaylistEntity(name = name, url = url)
            )
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
            if (playlist.isActive) {
                val remaining = playlists.value.firstOrNull { it.id != playlist.id }
                if (remaining != null) {
                    switchPlaylist(remaining.id)
                } else {
                    settingsRepository.saveActivePlaylistId(0L)
                    _activePlaylistId.value = 0L
                }
            }
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.updatePlaylist(playlist)
        }
    }

    fun reSyncPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            val tmdbKey = settingsRepository.tmdbKeyFlow.first()
            syncRepository.syncPlaylistForId(playlist.id, playlist.url, tmdbKey)
            if (playlist.isActive) {
                _activePlaylistId.value = playlist.id
            }
            _isSyncing.value = false
        }
    }

    fun updateChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            channelDao.updateChannel(channel)
        }
    }

    fun deleteChannels(channelsToDelete: List<ChannelEntity>) {
        viewModelScope.launch {
            channelsToDelete.forEach { channel ->
                channelDao.deleteChannel(channel)
            }
        }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            channelDao.toggleFavorite(channel.id, !channel.isFavorite)
        }
    }

    fun searchTmdb(query: String, customApiKey: String? = null) {
        viewModelScope.launch {
            _isTmdbSearching.value = true
            _tmdbSearchResults.value = emptyList()

            val key = if (!customApiKey.isNullOrBlank()) {
                customApiKey
            } else {
                val savedKey = settingsRepository.tmdbKeyFlow.first()
                if (savedKey.isNotBlank()) savedKey else "8414a0a520bf05b6329c368d1844e1f7"
            }

            val results = tmdbRepository.searchMediaList(key, query)
            _tmdbSearchResults.value = results
            _isTmdbSearching.value = false
        }
    }

    fun clearTmdbSearchResults() {
        _tmdbSearchResults.value = emptyList()
    }
}
