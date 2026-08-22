package com.streamlab.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamlab.tv.data.local.ChannelEntity
import com.streamlab.tv.data.repository.TmdbMediaInfo
import com.streamlab.tv.ui.MainViewModel
import com.streamlab.tv.ui.TvLoadingSpinner
import kotlinx.coroutines.delay

@Composable
fun EditChannelDialog(
    channel: ChannelEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (ChannelEntity) -> Unit,
    onDelete: (ChannelEntity) -> Unit
) {
    var channelName by remember { mutableStateOf(channel.name) }
    var channelGroup by remember { mutableStateOf(channel.group) }
    var channelPosterUrl by remember { mutableStateOf(channel.posterUrl ?: channel.logo) }
    
    val tmdbResults by viewModel.tmdbSearchResults.collectAsState()
    val isSearchingTmdb by viewModel.isTmdbSearching.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.clearTmdbSearchResults()
    }

    LaunchedEffect(Unit) {
        delay(150)
        try { nameFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    val nameFocusRequester = remember { FocusRequester() }
    val groupFocusRequester = remember { FocusRequester() }
    val searchButtonFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(com.streamlab.tv.ui.theme.SurfaceDark)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp)
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = com.streamlab.tv.ui.theme.PurpleAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Editar Canal & Informações TMDB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Row (Form on Left, TMDB Results & Preview on Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Column: Edit Form
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Nome do Canal / Filme",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        CustomTvInput(
                            value = channelName,
                            onValueChange = { channelName = it },
                            placeholder = "Digite o nome...",
                            modifier = Modifier.focusRequester(nameFocusRequester),
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.searchTmdb(channelName)
                            }
                        )

                        Text(
                            text = "Categoria / Grupo",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        CustomTvInput(
                            value = channelGroup,
                            onValueChange = { channelGroup = it },
                            placeholder = "Ex: Filmes, Séries, Esportes...",
                            modifier = Modifier.focusRequester(groupFocusRequester)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // TMDB Search Trigger Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.searchTmdb(channelName)
                            },
                            colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchButtonFocusRequester)
                        ) {
                            if (isSearchingTmdb) {
                                TvLoadingSpinner(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscando no TMDB...")
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Buscar TMDB")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buscar Informações no TMDB")
                            }
                        }

                        // Current Preview Card
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pré-visualização do Card",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (channelPosterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = channelPosterUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(65.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .width(65.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Sem Capa", color = Color.LightGray, fontSize = 10.sp)
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = channelName.ifEmpty { "Nome do Canal" },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channelGroup.ifEmpty { "Geral" },
                                        color = com.streamlab.tv.ui.theme.PurpleAccent,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: TMDB Suggestions
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Resultados Encontrados no TMDB",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Clique em um item para aplicar nome e poster oficial",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isSearchingTmdb) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                TvLoadingSpinner(modifier = Modifier.size(40.dp))
                            }
                        } else if (tmdbResults.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum resultado ainda.\nClique em 'Buscar no TMDB' para pesquisar.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(tmdbResults.size) { index ->
                                    val tmdbItem = tmdbResults[index]
                                    TmdbResultItem(
                                        item = tmdbItem,
                                        modifier = if (index == 0) Modifier.focusRequester(firstResultFocusRequester) else Modifier,
                                        onSelect = {
                                            channelName = tmdbItem.title
                                            if (!tmdbItem.posterUrl.isNullOrEmpty()) {
                                                channelPosterUrl = tmdbItem.posterUrl
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onDelete(channel) },
                        colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.ErrorRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Canal")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.colors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val updated = channel.copy(
                                    name = channelName.trim(),
                                    group = channelGroup.trim().ifEmpty { "Geral" },
                                    posterUrl = channelPosterUrl.ifEmpty { null }
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.colors(containerColor = com.streamlab.tv.ui.theme.PurpleAccent)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar Alterações")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTvInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) com.streamlab.tv.ui.theme.PurpleAccent else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.Gray,
                style = TextStyle(fontSize = 14.sp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(com.streamlab.tv.ui.theme.PurpleAccent),
            keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Done),
            keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

@Composable
fun TmdbResultItem(
    item: TmdbMediaInfo,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!item.posterUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(42.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.overview.isNullOrEmpty()) {
                    Text(
                        text = item.overview,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
