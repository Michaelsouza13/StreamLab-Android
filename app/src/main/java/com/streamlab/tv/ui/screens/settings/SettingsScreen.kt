package com.streamlab.tv.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val m3uUrl by viewModel.m3uUrl.collectAsState()
    val tmdbKey by viewModel.tmdbKey.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

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

        Column(
            modifier = Modifier
                .width(500.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Playlist M3U8 URL:", color = Color.White)
            TvTextField(
                value = m3uUrl,
                onValueChange = { viewModel.updateM3uUrl(it) },
                placeholder = "https://exemplo.com/playlist.m3u"
            )
        }

        Column(
            modifier = Modifier
                .width(500.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "TMDB API Key (Opcional):", color = Color.White)
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
        }

        if (syncMessage != null) {
            Text(
                text = syncMessage!!,
                color = if (isSyncing) com.streamlab.tv.ui.theme.PurpleAccent else Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.syncPlaylist() },
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "Sincronizando..." else "Salvar e Sincronizar")
            }
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

