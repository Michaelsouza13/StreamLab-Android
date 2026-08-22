package com.streamlab.tv.ui

import android.view.KeyEvent
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.streamlab.tv.R
import com.streamlab.tv.data.local.ChannelEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TvResizeMode(val label: String, val mode: Int) {
    FIT("Ajustar à Tela", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    ZOOM("Zoom / Preencher", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FILL("Esticar (16:9)", AspectRatioFrameLayout.RESIZE_MODE_FILL)
}

enum class PlayerDialogType {
    NONE, AUDIO, SUBTITLES, ASPECT_RATIO
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channelUrl: String,
    onBackPressed: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsState()

    var currentUrl by remember { mutableStateOf(channelUrl) }
    var currentChannel by remember { mutableStateOf<ChannelEntity?>(null) }

    // Synchronize current channel info when channels or url changes
    LaunchedEffect(currentUrl, channels) {
        val match = channels.firstOrNull { it.url.equals(currentUrl, ignoreCase = true) }
        currentChannel = match ?: ChannelEntity(
            name = if (currentUrl.contains("/")) currentUrl.substringAfterLast("/") else "Canal",
            logo = "",
            url = currentUrl,
            group = "Geral"
        )
    }

    // UI States
    var isOverlayVisible by remember { mutableStateOf(false) }
    var isSidePanelVisible by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf(PlayerDialogType.NONE) }
    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLiveStream by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentResizeMode by remember { mutableStateOf(TvResizeMode.FIT) }

    // User interaction tick to reset auto-hide countdown
    var interactionTick by remember { mutableLongStateOf(0L) }

    // VOD Timings
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Clock
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(10000)
        }
    }

    // Focus Requesters
    val primaryControlRequester = remember { FocusRequester() }
    val sidePanelFirstItemRequester = remember { FocusRequester() }
    val modalFirstItemRequester = remember { FocusRequester() }
    val errorRetryRequester = remember { FocusRequester() }
    val sidePanelListState = rememberLazyListState()

    // Request focus when overlay opens
    LaunchedEffect(isOverlayVisible) {
        if (isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE) {
            delay(120)
            try {
                primaryControlRequester.requestFocus()
            } catch (e: Exception) {
                // Focus safe catch
            }
        }
    }

    // Request focus when modal opens
    LaunchedEffect(activeDialog) {
        if (activeDialog != PlayerDialogType.NONE) {
            delay(120)
            try {
                modalFirstItemRequester.requestFocus()
            } catch (e: Exception) {
                // Focus safe catch
            }
        }
    }

    // Request focus when error screen appears
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(120)
            try {
                errorRetryRequester.requestFocus()
            } catch (e: Exception) {
                // Focus safe catch
            }
        }
    }

    // Scroll and focus side panel to current channel when opened
    LaunchedEffect(isSidePanelVisible) {
        if (isSidePanelVisible) {
            val currentIndex = channels.indexOfFirst { it.url.equals(currentUrl, ignoreCase = true) }
            if (currentIndex >= 0) {
                sidePanelListState.scrollToItem(currentIndex.coerceAtLeast(0))
            }
            delay(150)
            try {
                sidePanelFirstItemRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request safe catch
            }
        }
    }

    // Tracks
    var availableTracks by remember { mutableStateOf<Tracks?>(null) }

    // Build resilient ExoPlayer with IPTV capabilities
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)
            .setKeepPostFor302Redirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20000, // 20s min buffer
                60000, // 60s max buffer
                2000,  // 2s to start
                4000   // 4s after rebuffering
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build().apply {
                playWhenReady = true
            }
    }

    // Bind Player Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                    isLiveStream = exoPlayer.isCurrentMediaItemLive || exoPlayer.duration <= 0
                    durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val reason = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Falha de conexão com a rede."
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Tempo de resposta do servidor esgotado."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Canal indisponível no servidor (Erro HTTP)."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato do canal não reconhecido."
                    else -> "Erro na transmissão (${error.errorCodeName})."
                }
                errorMessage = reason
            }

            override fun onTracksChanged(tracks: Tracks) {
                availableTracks = tracks
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load & Play Stream function
    fun loadStream(url: String) {
        errorMessage = null
        isBuffering = true
        val cleanUrl = url.trim()

        val mimeType = when {
            cleanUrl.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
            cleanUrl.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
            cleanUrl.contains(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
            cleanUrl.contains(".mkv", ignoreCase = true) -> MimeTypes.VIDEO_MATROSKA
            cleanUrl.contains(".ts", ignoreCase = true) -> MimeTypes.VIDEO_MP2T
            else -> null
        }

        val item = MediaItem.Builder()
            .setUri(cleanUrl)
            .apply {
                if (mimeType != null) {
                    setMimeType(mimeType)
                }
            }
            .build()

        exoPlayer.stop()
        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    LaunchedEffect(currentUrl) {
        loadStream(currentUrl)
    }

    // Position tracker for VOD
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            delay(1000)
        }
    }

    // Auto-hide overlay after 7 seconds of inactivity
    LaunchedEffect(isOverlayVisible, isSidePanelVisible, activeDialog, interactionTick) {
        if (isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE) {
            delay(7000)
            isOverlayVisible = false
        }
    }

    // Back Handler Logic
    BackHandler {
        when {
            activeDialog != PlayerDialogType.NONE -> activeDialog = PlayerDialogType.NONE
            isSidePanelVisible -> isSidePanelVisible = false
            isOverlayVisible -> isOverlayVisible = false
            else -> onBackPressed()
        }
    }

    // Channel Switching (Zapping)
    fun switchChannel(offset: Int) {
        if (channels.isNotEmpty()) {
            val currentIndex = channels.indexOfFirst { it.url.equals(currentUrl, ignoreCase = true) }
            val nextIndex = if (currentIndex != -1) {
                (currentIndex + offset + channels.size) % channels.size
            } else {
                0
            }
            val newChannel = channels[nextIndex]
            currentUrl = newChannel.url
            loadStream(newChannel.url)
            isOverlayVisible = true
            interactionTick++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable(!isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE && errorMessage == null)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER -> {
                            if (activeDialog == PlayerDialogType.NONE && !isSidePanelVisible && errorMessage == null) {
                                if (!isOverlayVisible) {
                                    isOverlayVisible = true
                                    interactionTick++
                                    true
                                } else {
                                    false
                                }
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (!isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE && errorMessage == null) {
                                switchChannel(offset = -1)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (!isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE && errorMessage == null) {
                                switchChannel(offset = 1)
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (!isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE && errorMessage == null) {
                                isSidePanelVisible = true
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isOverlayVisible = true
                            interactionTick++
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            exoPlayer.play()
                            isOverlayVisible = true
                            interactionTick++
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            exoPlayer.pause()
                            isOverlayVisible = true
                            interactionTick++
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            exoPlayer.seekForward()
                            isOverlayVisible = true
                            interactionTick++
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            exoPlayer.seekBack()
                            isOverlayVisible = true
                            interactionTick++
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        // Video Render Surface with TextureView for native Android TV hardware transparency and proper z-order
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.player_view, null) as PlayerView
                view.player = exoPlayer
                view.resizeMode = currentResizeMode.mode
                view
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = currentResizeMode.mode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvLoadingSpinner(modifier = Modifier.size(52.dp))
                    Text(
                        text = "Carregando transmissão...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (currentChannel != null) {
                        Text(
                            text = currentChannel!!.name,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Error Screen
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Erro",
                        tint = com.streamlab.tv.ui.theme.ErrorRed,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Não foi possível carregar este canal",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage ?: "Falha na transmissão",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { loadStream(currentUrl) },
                            modifier = Modifier.focusRequester(errorRetryRequester)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentar Novamente")
                        }
                        Button(
                            onClick = {
                                isSidePanelVisible = true
                                errorMessage = null
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Outros Canais")
                        }
                        Button(
                            onClick = onBackPressed
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sair")
                        }
                    }
                }
            }
        }

        // Main Player Controls & Info Overlay
        AnimatedVisibility(
            visible = isOverlayVisible && !isSidePanelVisible && activeDialog == PlayerDialogType.NONE,
            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                // Top Bar: Channel Header & Clock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val headerImage = if (!currentChannel?.posterUrl.isNullOrEmpty()) currentChannel?.posterUrl else currentChannel?.logo
                        if (!headerImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = headerImage,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(4.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = currentChannel?.name ?: "Canal",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (currentChannel?.isFavorite == true) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Favoritado",
                                        tint = Color.Yellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                if (isLiveStream) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(com.streamlab.tv.ui.theme.ErrorRed)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "AO VIVO",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(com.streamlab.tv.ui.theme.PurpleAccent)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "VOD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = currentChannel?.group ?: "Geral",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }

                    // Clock & Format Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = currentResizeMode.label,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = currentTimeString,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Bottom Bar: Progress (if VOD) and Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // VOD Timeline
                    if (!isLiveStream && durationMs > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(currentPositionMs),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatDuration(durationMs),
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Progress Bar
                            val progress = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(com.streamlab.tv.ui.theme.PurpleAccent)
                                )
                            }
                        }
                    }

                    // Interactive Action Controls Row with Easy Remote Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play / Pause Button
                        Button(
                            onClick = {
                                interactionTick++
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = com.streamlab.tv.ui.theme.PurpleAccent.copy(alpha = 0.7f),
                                focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                            ),
                            modifier = Modifier.focusRequester(primaryControlRequester)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "Pausar" else "Reproduzir")
                        }

                        // Rewind / Forward for VOD
                        if (!isLiveStream && durationMs > 0) {
                            Button(
                                onClick = {
                                    interactionTick++
                                    exoPlayer.seekBack()
                                    currentPositionMs = exoPlayer.currentPosition
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "-10s")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("-10s")
                            }
                            Button(
                                onClick = {
                                    interactionTick++
                                    exoPlayer.seekForward()
                                    currentPositionMs = exoPlayer.currentPosition
                                }
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "+10s")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+10s")
                            }
                        }

                        // Audio Track Selector
                        Button(
                            onClick = {
                                interactionTick++
                                activeDialog = PlayerDialogType.AUDIO
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Áudio")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Áudio")
                        }

                        // Subtitles Selector
                        Button(
                            onClick = {
                                interactionTick++
                                activeDialog = PlayerDialogType.SUBTITLES
                            }
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Legendas")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Legendas")
                        }

                        // Aspect Ratio Toggle
                        Button(
                            onClick = {
                                interactionTick++
                                activeDialog = PlayerDialogType.ASPECT_RATIO
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Formato")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Formato")
                        }

                        // Channels List Button
                        Button(
                            onClick = {
                                interactionTick++
                                isSidePanelVisible = true
                                isOverlayVisible = false
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Canais")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Canais")
                        }

                        // Close Overlay Button
                        Button(
                            onClick = {
                                isOverlayVisible = false
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Ocultar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ocultar")
                        }

                        // Favorite Button
                        Button(
                            onClick = {
                                interactionTick++
                                currentChannel?.let { channel ->
                                    viewModel.toggleFavorite(channel)
                                }
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = if (currentChannel?.isFavorite == true)
                                    com.streamlab.tv.ui.theme.PurpleAccent.copy(alpha = 0.7f)
                                else Color.White.copy(alpha = 0.1f),
                                focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                            )
                        ) {
                            Icon(
                                imageVector = if (currentChannel?.isFavorite == true) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (currentChannel?.isFavorite == true) "Remover dos favoritos" else "Adicionar aos favoritos",
                                tint = if (currentChannel?.isFavorite == true) Color.Yellow else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentChannel?.isFavorite == true) "Favoritado" else "Favoritar")
                        }
                    }
                }
            }
        }

        // Zapping Side Panel (DPAD Left or Channels button)
        AnimatedVisibility(
            visible = isSidePanelVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(380.dp)
                    .background(Color.Black.copy(alpha = 0.94f))
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.15f))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Guia de Canais",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${channels.size} canais disponíveis",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = { isSidePanelVisible = false },
                            colors = ButtonDefaults.colors(containerColor = Color.Transparent)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        state = sidePanelListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(channels) { index, channel ->
                            val isCurrentPlaying = channel.url.equals(currentUrl, ignoreCase = true)
                            val itemModifier = if (index == 0) {
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(sidePanelFirstItemRequester)
                            } else {
                                Modifier.fillMaxWidth()
                            }

                            Surface(
                                onClick = {
                                    currentUrl = channel.url
                                    loadStream(channel.url)
                                    isSidePanelVisible = false
                                    isOverlayVisible = true
                                    interactionTick++
                                },
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isCurrentPlaying) com.streamlab.tv.ui.theme.PurpleAccent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                    focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                                ),
                                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                                modifier = itemModifier
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    val thumb = if (!channel.posterUrl.isNullOrEmpty()) channel.posterUrl else channel.logo
                                    if (!thumb.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = channel.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.4f))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.name,
                                            color = Color.White,
                                            fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = channel.group,
                                            color = if (isCurrentPlaying) com.streamlab.tv.ui.theme.PurpleAccent else Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (isCurrentPlaying) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Em reprodução",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modals / Dialogs: Audio, Subtitles, Aspect Ratio
        if (activeDialog != PlayerDialogType.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(450.dp)
                        .background(com.streamlab.tv.ui.theme.SurfaceDark, RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeDialog) {
                                    PlayerDialogType.AUDIO -> "Faixas de Áudio"
                                    PlayerDialogType.SUBTITLES -> "Legendas"
                                    PlayerDialogType.ASPECT_RATIO -> "Formato de Tela"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Button(
                                onClick = {
                                    activeDialog = PlayerDialogType.NONE
                                    isOverlayVisible = true
                                    interactionTick++
                                }
                            ) {
                                Text("Fechar")
                            }
                        }

                        when (activeDialog) {
                            PlayerDialogType.AUDIO -> {
                                AudioTrackList(
                                    exoPlayer = exoPlayer,
                                    availableTracks = availableTracks,
                                    firstItemRequester = modalFirstItemRequester,
                                    onSelected = {
                                        activeDialog = PlayerDialogType.NONE
                                        isOverlayVisible = true
                                        interactionTick++
                                    }
                                )
                            }
                            PlayerDialogType.SUBTITLES -> {
                                SubtitleTrackList(
                                    exoPlayer = exoPlayer,
                                    availableTracks = availableTracks,
                                    firstItemRequester = modalFirstItemRequester,
                                    onSelected = {
                                        activeDialog = PlayerDialogType.NONE
                                        isOverlayVisible = true
                                        interactionTick++
                                    }
                                )
                            }
                            PlayerDialogType.ASPECT_RATIO -> {
                                TvResizeMode.values().forEachIndexed { index, mode ->
                                    val isSelected = currentResizeMode == mode
                                    val modifier = if (index == 0) {
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(modalFirstItemRequester)
                                    } else {
                                        Modifier.fillMaxWidth()
                                    }

                                    Surface(
                                        onClick = {
                                            currentResizeMode = mode
                                            activeDialog = PlayerDialogType.NONE
                                            isOverlayVisible = true
                                            interactionTick++
                                        },
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) com.streamlab.tv.ui.theme.PurpleAccent else Color.White.copy(alpha = 0.08f),
                                            focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                                        ),
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                        modifier = modifier
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = Color.White,
                                            modifier = Modifier.padding(14.dp),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TvLoadingSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "tv_spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "tv_spinner_rotation"
    )

    Canvas(modifier = modifier.rotate(rotation)) {
        drawArc(
            color = Color.White.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFBB86FC),
            startAngle = 0f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AudioTrackList(
    exoPlayer: ExoPlayer,
    availableTracks: Tracks?,
    firstItemRequester: FocusRequester,
    onSelected: () -> Unit
) {
    val audioTrackGroups = remember(availableTracks) {
        val list = mutableListOf<Pair<TrackGroup, Int>>()
        availableTracks?.groups?.forEach { groupInfo ->
            if (groupInfo.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until groupInfo.length) {
                    list.add(Pair(groupInfo.mediaTrackGroup, i))
                }
            }
        }
        list
    }

    if (audioTrackGroups.isEmpty()) {
        Text(
            text = "Nenhuma faixa de áudio secundária encontrada.",
            color = Color.LightGray,
            modifier = Modifier.padding(8.dp)
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(audioTrackGroups.size) { index ->
                val group = audioTrackGroups[index].first
                val trackIndex = audioTrackGroups[index].second
                val format = group.getFormat(trackIndex)
                val lang = format.language ?: "Padrão"
                val label = format.label ?: "Faixa ${trackIndex + 1} ($lang)"
                val isSelected = (format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0

                val modifier = if (index == 0) {
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(firstItemRequester)
                } else {
                    Modifier.fillMaxWidth()
                }

                Surface(
                    onClick = {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group, trackIndex))
                            .build()
                        onSelected()
                    },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) com.streamlab.tv.ui.theme.PurpleAccent else Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    modifier = modifier
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SubtitleTrackList(
    exoPlayer: ExoPlayer,
    availableTracks: Tracks?,
    firstItemRequester: FocusRequester,
    onSelected: () -> Unit
) {
    val textTrackGroups = remember(availableTracks) {
        val list = mutableListOf<Pair<TrackGroup, Int>>()
        availableTracks?.groups?.forEach { groupInfo ->
            if (groupInfo.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until groupInfo.length) {
                    list.add(Pair(groupInfo.mediaTrackGroup, i))
                }
            }
        }
        list
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Surface(
                onClick = {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                    onSelected()
                },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(firstItemRequester)
            ) {
                Text(
                    text = "Desativar Legendas",
                    color = Color.White,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        items(textTrackGroups.size) { index ->
            val group = textTrackGroups[index].first
            val trackIndex = textTrackGroups[index].second
            val format = group.getFormat(trackIndex)
            val lang = format.language ?: "Padrão"
            val label = format.label ?: "Legenda (${lang})"

            Surface(
                onClick = {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(TrackSelectionOverride(group, trackIndex))
                        .build()
                    onSelected()
                },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = com.streamlab.tv.ui.theme.PurpleAccent
                ),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
