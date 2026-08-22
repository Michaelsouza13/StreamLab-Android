package com.streamlab.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlab.tv.data.local.ChannelDao
import com.streamlab.tv.data.local.ChannelEntity
import com.streamlab.tv.data.repository.SettingsRepository
import com.streamlab.tv.data.repository.SyncRepository
import com.streamlab.tv.data.repository.TmdbMediaInfo
import com.streamlab.tv.data.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val channelDao: ChannelDao,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    val channels: StateFlow<List<ChannelEntity>> = channelDao.getAllChannels()
        .stateIn(
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
    }

    fun syncDefaultPlaylistIfNeeded() {
        viewModelScope.launch {
            val currentChannels = channelDao.getAllChannels().first()
            if (currentChannels.isEmpty()) {
                _isSyncing.value = true
                val m3uUrl = settingsRepository.m3uUrlFlow.first()
                val effectiveUrl = if (m3uUrl.isBlank()) SettingsRepository.DEFAULT_M3U_URL else m3uUrl
                val tmdbKey = settingsRepository.tmdbKeyFlow.first()
                syncRepository.syncPlaylist(effectiveUrl, tmdbKey)
                _isSyncing.value = false
            }
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
                // Default fallback demo TMDB key if none provided to ensure TMDB lookup works out-of-the-box
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
