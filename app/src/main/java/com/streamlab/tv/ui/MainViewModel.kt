package com.streamlab.tv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlab.tv.data.local.ChannelDao
import com.streamlab.tv.data.local.ChannelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val channelDao: ChannelDao
) : ViewModel() {

    val channels: StateFlow<List<ChannelEntity>> = channelDao.getAllChannels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _currentChannel = MutableStateFlow<ChannelEntity?>(null)
    val currentChannel: StateFlow<ChannelEntity?> = _currentChannel.asStateFlow()

    fun playChannel(channel: ChannelEntity) {
        _currentChannel.value = channel
    }

    fun loadMockDataForTesting() {
        viewModelScope.launch {
            val mockData = listOf(
                ChannelEntity(name = "Big Buck Bunny", logo = "", url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", group = "Movies"),
                ChannelEntity(name = "Sintel", logo = "", url = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8", group = "Movies"),
                ChannelEntity(name = "Tears of Steel", logo = "", url = "http://demo.unified-streaming.com/video/tears-of-steel/tears-of-steel.ism/.m3u8", group = "Movies")
            )
            channelDao.insertChannels(mockData)
        }
    }
}
