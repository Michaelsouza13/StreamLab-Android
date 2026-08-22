package com.streamlab.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamlab.tv.data.local.ChannelEntity
import com.streamlab.tv.ui.components.AppDrawer
import com.streamlab.tv.ui.components.EditChannelDialog

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit = {}
) {
    val channels by viewModel.channels.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    
    // Category Filter State
    var selectedCategory by remember { mutableStateOf("Todos") }
    
    // Edit & Channel Dialog State
    var isEditMode by remember { mutableStateOf(false) }
    var editingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    val selectedChannels = remember { mutableStateListOf<ChannelEntity>() }

    // Auto-sync default playlist on startup if empty
    LaunchedEffect(Unit) {
        viewModel.syncDefaultPlaylistIfNeeded()
    }

    // Extract unique groups/categories
    val categories = remember(channels) {
        val uniqueGroups = channels.map { it.group.trim() }.filter { it.isNotEmpty() }.distinct()
        listOf("Todos", "Favoritos") + uniqueGroups
    }

    val filteredChannels = remember(channels, selectedCategory) {
        when {
            selectedCategory == "Todos" -> channels
            selectedCategory == "Favoritos" -> channels.filter { it.isFavorite }
            else -> channels.filter { it.group.equals(selectedCategory, ignoreCase = true) }
        }
    }

    AppDrawer(
        currentRoute = "main",
        onNavigate = { route ->
            when (route) {
                "settings" -> onNavigateToSettings()
                "search" -> onNavigateToSearch()
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { false }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                onClick = { selectedCategory = category },
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isSelected) com.streamlab.tv.ui.theme.PurpleAccent else Color.White.copy(alpha = 0.08f),
                                    focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                                ),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = category,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Edit Actions / Mode Button
                    if (isEditMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Clique em um canal para editar ou no lixo para excluir em lote",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                            
                            if (selectedChannels.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.deleteChannels(selectedChannels)
                                        selectedChannels.clear()
                                        isEditMode = false
                                    },
                                    colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.ErrorRed)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Excluir (${selectedChannels.size})")
                                }
                            }

                            Button(
                                onClick = {
                                    selectedChannels.clear()
                                    isEditMode = false
                                },
                                colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.12f))
                            ) {
                                Text("Concluir")
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isEditMode = true }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar / TMDB")
                            }
                        }
                    }
                }

                // Main Content (Loading, Empty or Channels Grid)
                if (isSyncing && channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TvLoadingSpinner(modifier = Modifier.size(48.dp))
                            Text(
                                text = "Baixando lista de canais padrão...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    ChannelGrid(
                        channels = filteredChannels,
                        isEditMode = isEditMode,
                        selectedChannels = selectedChannels,
                        onChannelClick = { channel ->
                            if (isEditMode) {
                                editingChannel = channel
                            } else {
                                onNavigateToPlayer(channel.url)
                            }
                        },
                        onChannelToggleSelect = { channel ->
                            if (selectedChannels.contains(channel)) {
                                selectedChannels.remove(channel)
                            } else {
                                selectedChannels.add(channel)
                            }
                        },
                        onToggleFavorite = { channel ->
                            viewModel.toggleFavorite(channel)
                        }
                    )
                }
            }

            // Edit Channel Dialog with TMDB
            editingChannel?.let { channelToEdit ->
                EditChannelDialog(
                    channel = channelToEdit,
                    viewModel = viewModel,
                    onDismiss = { editingChannel = null },
                    onSave = { updatedChannel ->
                        viewModel.updateChannel(updatedChannel)
                        editingChannel = null
                    },
                    onDelete = { channelToDelete ->
                        viewModel.deleteChannels(listOf(channelToDelete))
                        editingChannel = null
                    }
                )
            }
        }
    }
}

@Composable
fun ChannelGrid(
    channels: List<ChannelEntity>,
    isEditMode: Boolean,
    selectedChannels: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onChannelToggleSelect: (ChannelEntity) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit
) {
    if (channels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum canal encontrado nesta categoria.",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(channels) { channel ->
                ChannelCard(
                    channel = channel,
                    isSelected = selectedChannels.contains(channel),
                    isEditMode = isEditMode,
                    onClick = { onChannelClick(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) }
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelEntity,
    isSelected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .focusable(true)
            .onKeyEvent {
                isFocused = it.isFocused
                false
            }
            .border(
                width = when {
                    isFocused && !isEditMode -> 3.dp
                    isSelected -> 3.dp
                    isEditMode -> 1.dp
                    else -> 0.dp
                },
                color = when {
                    isFocused && !isEditMode -> com.streamlab.tv.ui.theme.PurpleAccent
                    isSelected -> com.streamlab.tv.ui.theme.ErrorRed
                    isEditMode -> com.streamlab.tv.ui.theme.PurpleAccent.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                shape = MaterialTheme.shapes.medium
            ),
        scale = CardDefaults.scale(focusedScale = 1.08f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = if (!channel.posterUrl.isNullOrEmpty()) channel.posterUrl else channel.logo
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = channel.name,
                    contentScale = if (!channel.posterUrl.isNullOrEmpty()) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (!channel.posterUrl.isNullOrEmpty()) 0.dp else 16.dp)
                )
            } else {
                Text(
                    text = channel.name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Bottom gradient overlay for channel title readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Star (top-left, always visible when not in edit mode)
            if (!isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (channel.isFavorite) com.streamlab.tv.ui.theme.PurpleAccent
                            else Color.Black.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (channel.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (channel.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(com.streamlab.tv.ui.theme.PurpleAccent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Editar",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isEditMode && isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = com.streamlab.tv.ui.theme.ErrorRed,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
        }
    }
}
