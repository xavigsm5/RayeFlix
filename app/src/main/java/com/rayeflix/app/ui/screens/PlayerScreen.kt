package com.rayeflix.app.ui.screens

import android.net.Uri
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rayeflix.app.ui.theme.NetflixRed
import com.rayeflix.app.ui.theme.White
import kotlinx.coroutines.delay

import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackGroup
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.common.Format

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoUrl: String, titleArg: String, subtitleArg: String, navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    
    // Inactivity Timer State
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Focus Requester for TV Navigation
    val playButtonFocusRequester = remember { FocusRequester() }
    
    // Decode arguments
    val decodedUrl = remember(videoUrl) { android.net.Uri.decode(videoUrl) }
    val title = remember(titleArg) { android.net.Uri.decode(titleArg) }
    val subtitle = remember(subtitleArg) { android.net.Uri.decode(subtitleArg) }
    
    // Quality / Track Selection States
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    
    var audioTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var videoTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    
    var selectedAudioTrack by remember { mutableStateOf<TrackInfo?>(null) }
    var selectedSubtitleTrack by remember { mutableStateOf<TrackInfo?>(null) }
    
    // Helper to register interaction
    fun onInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (!showControls) showControls = true
    }

    val exoPlayer = remember {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
        
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        // Custom LoadControl for better buffering (4K support)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                15_000, // Min buffer 15s
                50_000, // Max buffer 50s
                5000, // Buffer for playback (start after 5s)
                10_000  // Buffer for rebuffer (adjust to 10s)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(decodedUrl))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerScreen", "ExoPlayer Error: ${error.message}", error)
                    }

                    // ...
                    // Ensure the listener is kept clean, we just replace the block start
                    override fun onTracksChanged(tracks: Tracks) {
                        val newAudio = mutableListOf<TrackInfo>()
                        val newSubtitle = mutableListOf<TrackInfo>()
                        val newVideo = mutableListOf<TrackInfo>()
                        
                        for (group in tracks.groups) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val isSelected = group.isSelected
                                val trackName = when(group.type) {
                                    C.TRACK_TYPE_AUDIO -> "${format.language ?: "Unknown"} ${format.label ?: ""}".trim()
                                    C.TRACK_TYPE_TEXT -> "${format.language ?: "Unknown"} ${format.label ?: ""}".trim()
                                    C.TRACK_TYPE_VIDEO -> {
                                        if (format.height != Format.NO_VALUE) "${format.height}p" else "Auto / Unknown"
                                    } 
                                    else -> ""
                                }
                                
                                val trackId = format.id ?: "$i"
                                val info = TrackInfo(trackId, trackName.ifEmpty { "Track ${i+1}" }, group.mediaTrackGroup, i)
                                
                                when(group.type) {
                                    C.TRACK_TYPE_AUDIO -> {
                                        newAudio.add(info)
                                        if (isSelected) selectedAudioTrack = info
                                    }
                                    C.TRACK_TYPE_TEXT -> {
                                        newSubtitle.add(info)
                                        if (isSelected) selectedSubtitleTrack = info
                                    }
                                    C.TRACK_TYPE_VIDEO -> {
                                        // Filter duplicates or unhelpful labels
                                        if (newVideo.none { it.name == info.name } && info.name.contains("p")) {
                                            newVideo.add(info)
                                        }
                                    }
                                }
                            }
                        }
                        audioTracks = newAudio
                        subtitleTracks = newSubtitle
                        videoTracks = newVideo.sortedByDescending { it.name.replace("p","").toIntOrNull() ?: 0 }
                    }
                })
            }
    }
    
    // Track Selection Helper
    fun selectTrack(track: TrackInfo?, type: Int) {
        val parametersBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        if (track == null) {
            parametersBuilder.clearOverridesOfType(type)
            if (type == C.TRACK_TYPE_TEXT) {
                 parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            }
        } else {
             parametersBuilder.setTrackTypeDisabled(type, false)
             // Correctly passing TrackGroup and index
             parametersBuilder.setOverrideForType(TrackSelectionOverride(track.group, track.index))
        }
        exoPlayer.trackSelectionParameters = parametersBuilder.build()
    }

    // Update progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            try {
                currentPosition = exoPlayer.currentPosition
                totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                isPlaying = exoPlayer.isPlaying
            } catch (e: Exception) {
                break
            }
            delay(1000)
        }
    }

    // Auto hide controls logic
    LaunchedEffect(lastInteractionTime, isPlaying, showControls) {
        if (showControls && isPlaying) {
             delay(3000)
             showControls = false
        }
    }
    
    // Initial Focus
    LaunchedEffect(showControls) {
        if (showControls) {
            try {
                delay(100) 
                playButtonFocusRequester.requestFocus()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
             exoPlayer.stop()
             exoPlayer.clearMediaItems()
             android.os.Handler(android.os.Looper.getMainLooper()).post {
                 try { exoPlayer.release() } catch (e: Exception) { e.printStackTrace() }
             }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onInteraction()
                showControls = !showControls
            }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    onInteraction() 
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            navController.popBackStack()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .focusable() 
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false 
                    // Use TextureView to avoid SurfaceView overlay issues on some emulators/devices causing generic errors
                    // However, PlayerView doesn't have a simple setter for specific surface type at runtime easily without XML. 
                    // But we can try to set it via layout params or assume default.
                    // A common fix for "unrecoverably broken" is enabling TextureView.
                    // But we cant easily set it here without XML layout inflation.
                    // Let keep it simple but ensure keepScreenOn is true.
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                // Top Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var isBackFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .onFocusChanged { isBackFocused = it.isFocused }
                                    .border(2.dp, if (isBackFocused) White else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            var isReplayFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { 
                                    onInteraction()
                                    exoPlayer.seekBack() 
                                },
                                modifier = Modifier
                                    .onFocusChanged { isReplayFocused = it.isFocused }
                                    .border(2.dp, if (isReplayFocused) White else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Replay 10s", tint = White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            var isSkipFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { 
                                    onInteraction()
                                    exoPlayer.seekForward() 
                                },
                                modifier = Modifier
                                    .onFocusChanged { isSkipFocused = it.isFocused }
                                    .border(2.dp, if (isSkipFocused) White else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "Skip", tint = White)
                            }
                        }
                        Text("OPCIONES", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle, color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }

                // Bottom Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Pause Button
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPlayFocused by interactionSource.collectIsFocusedAsState()
                        
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(White)
                                .border(4.dp, if (isPlayFocused) White else Color.Transparent, CircleShape)
                                .clickable(
                                    interactionSource = interactionSource, 
                                    indication = androidx.compose.material.ripple.rememberRipple(bounded = true, color = Color.Red)
                                    ) {
                                    onInteraction()
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    isPlaying = !isPlaying
                                }
                                .focusRequester(playButtonFocusRequester) // Attach FocusRequester
                                .focusable(interactionSource = interactionSource), 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(formatDuration(currentPosition), color = White, fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Slider(
                             value = currentPosition.toFloat(),
                             onValueChange = {}, // Read-only for now or add seek logic
                             valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = NetflixRed,
                                 activeTrackColor = NetflixRed,
                                 inactiveTrackColor = Color.Gray
                             ),
                             modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(formatDuration(totalDuration), color = White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio/Subtitle Options
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chips
                        PlayerOptionChip("Audio", isSelected = false) { 
                            onInteraction()
                            showAudioDialog = true
                        }
                        
                        PlayerOptionChip("Subtítulos", isSelected = false) { 
                             onInteraction()
                             showSubtitleDialog = true
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        var isSettingsFocused by remember { mutableStateOf(false) }
                        
                        // Settings Button with explicit click handling box to avoid missing taps
                        Box(
                            modifier = Modifier
                                .border(2.dp, if (isSettingsFocused) White else Color.Transparent, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    onInteraction()
                                    showQualityDialog = true
                                }
                                .onFocusChanged { isSettingsFocused = it.isFocused }
                                .focusable()
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = White)
                        }
                    }
                }
            }
        }
        
        if (showAudioDialog) {
             TrackSelectionDialog(
                 title = "Audio",
                 tracks = audioTracks,
                 onDismiss = { showAudioDialog = false; onInteraction() },
                 onTrackSelected = { track ->
                     selectedAudioTrack = track
                     selectTrack(track, C.TRACK_TYPE_AUDIO)
                     showAudioDialog = false
                     onInteraction()
                 }
             )
        }

        if (showSubtitleDialog) {
             TrackSelectionDialog(
                 title = "Subtítulos",
                 tracks = subtitleTracks,
                 onDismiss = { showSubtitleDialog = false; onInteraction() },
                 onTrackSelected = { track ->
                     selectedSubtitleTrack = track
                     selectTrack(track, C.TRACK_TYPE_TEXT)
                     showSubtitleDialog = false
                     onInteraction()
                 },
                 includeOff = true
             )
        }
        
        if (showQualityDialog) {
             TrackSelectionDialog(
                 title = "Calidad de Video",
                 tracks = videoTracks,
                 onDismiss = { showQualityDialog = false; onInteraction() },
                 onTrackSelected = { track ->
                      selectTrack(track, C.TRACK_TYPE_VIDEO)
                      showQualityDialog = false
                      onInteraction()
                 },
                 includeOff = true,
                 offLabel = "Automático (Recomendado)"
             )
        }
    }
}

data class TrackInfo(
    val id: String,
    val name: String,
    val group: TrackGroup, 
    val index: Int
)

@Composable
fun TrackSelectionDialog(
    title: String, 
    tracks: List<TrackInfo>, 
    onDismiss: () -> Unit, 
    onTrackSelected: (TrackInfo?) -> Unit,
    includeOff: Boolean = false,
    offLabel: String = "Desactivado"
) {
    AlertDialog(
         containerColor = Color.DarkGray,
         onDismissRequest = onDismiss,
         title = { Text(title, color = White) },
         text = {
             Column {
                 if (includeOff) {
                     var isFocused by remember { mutableStateOf(false) }
                     Text(
                         text = offLabel,
                         color = White,
                         modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(null) }
                            .onFocusChanged { isFocused = it.isFocused }
                            .background(if (isFocused) Color.White.copy(alpha=0.1f) else Color.Transparent)
                            .padding(12.dp)
                            .focusable()
                     )
                 }
                 
                 if (tracks.isEmpty()) {
                      Text("No hay opciones disponibles", color = Color.Gray, modifier = Modifier.padding(12.dp))
                 }
                 
                 tracks.forEach { track ->
                     var isFocused by remember { mutableStateOf(false) }
                     Text(
                         text = track.name,
                         color = White,
                         modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackSelected(track) }
                            .onFocusChanged { isFocused = it.isFocused }
                            .background(if (isFocused) Color.White.copy(alpha=0.1f) else Color.Transparent)
                            .focusable()
                            .padding(12.dp)
                     )
                 }
             }
         },
         confirmButton = {
             TextButton(onClick = onDismiss) {
                 Text("Cerrar", color = NetflixRed)
             }
         }
     )
}

@Composable
fun PlayerOptionChip(text: String, isSelected: Boolean, enabled: Boolean = true, onClick: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor = when {
        isSelected -> White
        !enabled -> Color.DarkGray.copy(alpha = 0.5f)
        isFocused -> Color.White.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.Black
        !enabled -> Color.Gray
        else -> White
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(2.dp, if(isFocused && enabled) White else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(enabled = enabled)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}


private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
