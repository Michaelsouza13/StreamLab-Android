package com.streamlab.tv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamlab.tv.data.local.PlaylistEntity

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val m3uUrl by viewModel.m3uUrl.collectAsState()
    val tmdbKey by viewModel.tmdbKey.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val bulkProgress by viewModel.bulkProgress.collectAsState()
    val isBulkSyncing by viewModel.isBulkSyncing.collectAsState()
    val showBulkConfirm by viewModel.showBulkConfirm.collectAsState()

    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistUrl by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    // Bulk sync confirmation dialog
    if (showBulkConfirm) {
        Dialog(
            onDismissRequest = { viewModel.dismissBulkConfirm() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .width(480.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.streamlab.tv.ui.theme.SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Sincronizar capas TMDB?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Detectamos canais sem capa. Deseja buscar automaticamente todas as capas e informações no TMDB agora? Isso pode levar alguns minutos.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = { viewModel.dismissBulkConfirm() },
                            colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) { Text("Depois") }
                        Button(
                            onClick = { viewModel.bulkSyncAllPosters() },
                            colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent)
                        ) { Text("Sincronizar agora") }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        // --- Playlists Section ---
        Column(
            modifier = Modifier
                .width(600.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showAddForm = !showAddForm },
                        colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent)
                    ) {
                        Text(if (showAddForm) "Cancelar" else "+ Nova Playlist")
                    }
                }
            }

            if (showAddForm) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Nome:", color = Color.LightGray, fontSize = 12.sp)
                    TvTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = "Minha Lista"
                    )
                    Text("URL M3U:", color = Color.LightGray, fontSize = 12.sp)
                    TvTextField(
                        value = newPlaylistUrl,
                        onValueChange = { newPlaylistUrl = it },
                        placeholder = "https://exemplo.com/playlist.m3u"
                    )
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank() && newPlaylistUrl.isNotBlank()) {
                                viewModel.addPlaylist(newPlaylistName.trim(), newPlaylistUrl.trim())
                                newPlaylistName = ""
                                newPlaylistUrl = ""
                                showAddForm = false
                            }
                        }
                    ) {
                        Text("Salvar")
                    }
                }
            }

            if (playlists.isEmpty()) {
                Text(
                    text = "Nenhuma playlist adicionada.",
                    color = Color.Gray,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(playlists) { playlist ->
                        val isActive = playlist.id == activePlaylist?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isActive) com.streamlab.tv.ui.theme.PurpleAccent.copy(alpha = 0.2f)
                                    else Color.Black.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ATIVA",
                                            color = com.streamlab.tv.ui.theme.PurpleAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = playlist.url,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isActive) {
                                    Button(
                                        onClick = { viewModel.switchPlaylist(playlist.id) },
                                        colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent)
                                    ) {
                                        Text("Ativar", fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.reSyncPlaylist(playlist) },
                                    enabled = !isSyncing
                                ) {
                                    Text("Sync", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { viewModel.deletePlaylist(playlist) },
                                    colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.ErrorRed)
                                ) {
                                    Text("X", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TMDB Section ---
        Column(
            modifier = Modifier
                .width(550.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "TMDB API Key (Opcional):", color = Color.White, fontWeight = FontWeight.Bold)
            TvTextField(
                value = tmdbKey,
                onValueChange = { viewModel.updateTmdbKey(it) },
                placeholder = "Sua chave API"
            )
            Text(
                text = "Usado para baixar capas originais e metadados",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Bulk sync button + progress
            if (tmdbKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.bulkSyncAllPosters() },
                    enabled = !isBulkSyncing && !isSyncing,
                    colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent)
                ) {
                    Text(if (isBulkSyncing) "Sincronizando..." else "Sincronizar todas as capas agora")
                }

                if (isBulkSyncing && bulkProgress != null) {
                    val (done, total) = bulkProgress!!
                    val progress = if (total > 0) done.toFloat() / total.toFloat() else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$done / $total", color = Color.LightGray, fontSize = 12.sp)
                            Text(text = "${(progress * 100).toInt()}%", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .background(com.streamlab.tv.ui.theme.PurpleAccent)
                            )
                        }
                    }
                }
            }
        }

        if (syncMessage != null) {
            Text(
                text = syncMessage!!,
                color = if (isSyncing || isBulkSyncing) com.streamlab.tv.ui.theme.PurpleAccent else Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onBackPressed) {
                Text("Voltar")
            }
        }
    }
}

@Composable
fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isFocused) com.streamlab.tv.ui.theme.SurfaceVariantDark else Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.Gray)
                }
                innerTextField()
            }
        }
    )
}
