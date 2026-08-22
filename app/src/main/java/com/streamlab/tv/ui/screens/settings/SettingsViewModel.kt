package com.streamlab.tv.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlab.tv.data.local.PlaylistDao
import com.streamlab.tv.data.local.PlaylistEntity
import com.streamlab.tv.data.repository.SettingsRepository
import com.streamlab.tv.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val playlistDao: PlaylistDao
) : ViewModel() {

    private val _m3uUrl = MutableStateFlow("")
    val m3uUrl: StateFlow<String> = _m3uUrl.asStateFlow()

    private val _tmdbKey = MutableStateFlow("")
    val tmdbKey: StateFlow<String> = _tmdbKey.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

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

    init {
        viewModelScope.launch {
            settingsRepository.m3uUrlFlow.collectLatest { _m3uUrl.value = it }
        }
        viewModelScope.launch {
            settingsRepository.tmdbKeyFlow.collectLatest { _tmdbKey.value = it }
        }
    }

    fun updateM3uUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.saveM3uUrl(url)
        }
    }

    fun updateTmdbKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveTmdbKey(key)
        }
    }

    fun syncPlaylist() {
        if (_m3uUrl.value.isBlank()) {
            _syncMessage.value = "Por favor, insira uma URL válida."
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Sincronizando canais..."
            
            val success = syncRepository.syncPlaylist(_m3uUrl.value, _tmdbKey.value)
            
            if (success) {
                _syncMessage.value = "Sincronização concluída com sucesso! Capas baixando em background."
            } else {
                _syncMessage.value = "Falha ao sincronizar a playlist."
            }
            
            _isSyncing.value = false
        }
    }

    fun addPlaylist(name: String, url: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name, url = url))
            _syncMessage.value = "Playlist \"$name\" criada."
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
                }
            }
            _syncMessage.value = "Playlist removida."
        }
    }

    fun switchPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deactivateAll()
            playlistDao.setActive(playlistId)
            settingsRepository.saveActivePlaylistId(playlistId)
            _syncMessage.value = "Playlist ativa alterada."
        }
    }

    fun reSyncPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Sincronizando \"${playlist.name}\"..."
            val success = syncRepository.syncPlaylistForId(playlist.id, playlist.url, _tmdbKey.value)
            if (success) {
                _syncMessage.value = "\"${playlist.name}\" sincronizada com sucesso!"
            } else {
                _syncMessage.value = "Falha ao sincronizar \"${playlist.name}\"."
            }
            _isSyncing.value = false
        }
    }
    
    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
