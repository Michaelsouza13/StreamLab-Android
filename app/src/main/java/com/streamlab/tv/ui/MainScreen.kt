package com.streamlab.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamlab.tv.data.local.ChannelEntity

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val channels by viewModel.channels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()

    LaunchedEffect(Unit) {
        if (channels.isEmpty()) {
            viewModel.loadMockDataForTesting()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentChannel != null) {
            PlayerScreen(
                channel = currentChannel!!,
                onBackPressed = { viewModel.playChannel(currentChannel!!) } // Simplified, usually clears or handles back
            )
        } else {
            ChannelGrid(
                channels = channels,
                onChannelSelected = { viewModel.playChannel(it) }
            )
        }
    }
}

@Composable
fun ChannelGrid(
    channels: List<ChannelEntity>,
    onChannelSelected: (ChannelEntity) -> Unit
) {
    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(4),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(channels) { channel ->
            ChannelCard(
                channel = channel,
                onClick = { onChannelSelected(channel) }
            )
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(16f / 9f),
        scale = CardDefaults.scale(focusedScale = 1.1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (channel.logo.isNotEmpty()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            } else {
                Text(
                    text = channel.name,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    }
}
