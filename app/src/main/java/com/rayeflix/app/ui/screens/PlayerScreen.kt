package com.rayeflix.app.ui.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rayeflix.app.ui.theme.NetflixRed
import com.rayeflix.app.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlayerScreen(videoUrl: String, titleArg: String, subtitleArg: String, navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    // Keep Screen On
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }

    // Inactivity Timer State
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Focus Requester for TV Navigation
    val playButtonFocusRequester = remember { FocusRequester() }

    // Decode arguments
    val decodedUrl = remember(videoUrl) { Uri.decode(videoUrl) }
    val title = remember(titleArg) { Uri.decode(titleArg) }
    val subtitle = remember(subtitleArg) { Uri.decode(subtitleArg) }

    // Quality / Track Selection States
    var showQualityDialog by remember { mutableStateOf(false) } // Video track selection? LibVLC usually exposes this as tracks too
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var audioTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var videoTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }

    var selectedAudioTrack by remember { mutableStateOf<TrackInfo?>(null) }
    var selectedSubtitleTrack by remember { mutableStateOf<TrackInfo?>(null) }

    // LibVLC Initialization with optimized buffering options
    val libVLC = remember {
        val options = ArrayList<String>()
        // Buffer aumentado para mejor estabilidad en streams en vivo
        options.add("--network-caching=20000")  // 20s buffer de red
        options.add("--live-caching=8000")       // 8s para live streams
        options.add("--file-caching=3000")       // 3s para archivos
        options.add("--clock-jitter=0")
        options.add("--clock-synchro=0")
        options.add("--http-reconnect")          // Reconexión automática
        options.add("--network-timeout=10000")   // 10s timeout
        
        LibVLC(context, options)
    }

    val mediaPlayer = remember(libVLC) { MediaPlayer(libVLC) }

    // Buffering and loading states
    var isBuffering by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Prepare Media
    LaunchedEffect(decodedUrl) {
        try {
            Log.d("PlayerScreen", "Playing URL: $decodedUrl")
            isLoading = true
            val media = Media(libVLC, Uri.parse(decodedUrl))
            
            // Detect content type for optimal buffer settings
            val titleLower = (title ?: "").lowercase()
            val urlLower = decodedUrl.lowercase()
            
            // Check if it's 4K content
            val is4K = titleLower.contains("4k") || 
                       titleLower.contains("2160") || 
                       titleLower.contains("uhd") ||
                       urlLower.contains("4k") ||
                       urlLower.contains("2160")
            
            // Check if it's live content              
            val isLive = urlLower.contains("/live/") || 
                         titleLower.contains("live") ||
                         !urlLower.endsWith(".mkv") && 
                         !urlLower.endsWith(".mp4") && 
                         !urlLower.endsWith(".avi")
            
            // Use same buffer for all content types to avoid memory issues
            // 4K requires hardware decoding, not larger buffers
            val networkCache = 20000  // 20s for all content
            val liveCache = 8000
            val fileCache = 3000
            
            Log.d("PlayerScreen", "Content: 4K=$is4K, Live=$isLive, Buffer=${networkCache}ms")
            
            // Apply optimized options
            media.addOption(":network-caching=$networkCache")
            media.addOption(":file-caching=$fileCache")
            media.addOption(":live-caching=$liveCache")
            media.addOption(":sout-mux-caching=5000")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")
            media.addOption(":drop-late-frames")
            media.addOption(":skip-frames")
            media.addOption(":http-reconnect")
            media.addOption(":http-continuous=1")
            media.addOption(":network-timeout=15000")  // 15s timeout for slow connections
            
            // Enable hardware decoding - essential for 4K
            media.setHWDecoderEnabled(true, false)
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        } catch (e: Exception) {
            Log.e("PlayerScreen", "Error loading media", e)
            isLoading = false
            hasError = true
        }
    }

    // Event Listener with buffering recovery
    DisposableEffect(mediaPlayer) {
        val listener = object : MediaPlayer.EventListener {
            override fun onEvent(event: MediaPlayer.Event) {
                when (event.type) {
                    MediaPlayer.Event.TimeChanged -> {
                        currentPosition = event.timeChanged
                        if (isBuffering) isBuffering = false
                    }
                    MediaPlayer.Event.LengthChanged -> {
                        totalDuration = event.lengthChanged
                    }
                    MediaPlayer.Event.Buffering -> {
                        val bufferPercent = event.buffering
                        Log.d("PlayerScreen", "Buffering: $bufferPercent%")
                        isBuffering = bufferPercent < 100f
                    }
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                        isBuffering = false
                        isLoading = false  // Video started playing
                        Log.d("PlayerScreen", "Video started playing")
                        if (totalDuration == 0L) totalDuration = mediaPlayer.length
                        
                        val audio = mediaPlayer.audioTracks
                        if (audio != null) {
                            audioTracks = audio.mapNotNull { if (it.id == -1) null else TrackInfo(it.id, it.name) }
                        }
                        
                        val spu = mediaPlayer.spuTracks
                        if (spu != null) {
                             subtitleTracks = spu.mapNotNull { if (it.id == -1) null else TrackInfo(it.id, it.name) }
                        }

                        
                        val vid = mediaPlayer.videoTracks
                        if (vid != null) {
                             videoTracks = vid.mapNotNull { if (it.id == -1) null else TrackInfo(it.id, it.name) }
                        }
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.Stopped -> {
                        // If stopped unexpectedly while playing, try to recover
                        Log.w("PlayerScreen", "Playback stopped unexpectedly")
                        isPlaying = false
                    }
                    MediaPlayer.Event.EndReached -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e("PlayerScreen", "VLC Error encountered")
                        isBuffering = false
                        isLoading = false
                        isPlaying = false
                        hasError = true
                    }
                }
            }
        }
        mediaPlayer.setEventListener(listener)

        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        }
    }
    
    // Helper to register interaction
    fun onInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        if (!showControls) showControls = true
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
        // Video Surface
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    mediaPlayer.attachViews(this, null, false, false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading/Buffering Indicator
        AnimatedVisibility(
            visible = isLoading || isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isLoading) "Cargando..." else "Buffering...",
                        color = White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Error Screen
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "❌",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error al reproducir",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se pudo cargar el contenido",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                    ) {
                        Text("Volver", color = White)
                    }
                }
            }
        }

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
                                    val newTime = (currentPosition - 10000).coerceAtLeast(0)
                                    mediaPlayer.time = newTime
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
                                    val newTime = (currentPosition + 10000).coerceAtMost(totalDuration)
                                    mediaPlayer.time = newTime
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
                                    if (isPlaying) mediaPlayer.pause() else mediaPlayer.play()
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
                             onValueChange = { 
                                // Seek logic
                                val newTime = it.toLong()
                                mediaPlayer.time = newTime
                                currentPosition = newTime
                                onInteraction()
                             }, 
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
                            // Refresh tracks in case they loaded late
                             val audio = mediaPlayer.audioTracks
                            if (audio != null) {
                                audioTracks = audio.mapNotNull { if (it.id == -1) null else TrackInfo(it.id, it.name) }
                            }
                            showAudioDialog = true
                        }
                        
                        PlayerOptionChip("Subtítulos", isSelected = false) { 
                             onInteraction()
                             val spu = mediaPlayer.spuTracks
                            if (spu != null) {
                                 subtitleTracks = spu.mapNotNull { if (it.id == -1) null else TrackInfo(it.id, it.name) }
                            }
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
                     // Remember playing state before track change
                     val wasPlaying = mediaPlayer.isPlaying
                     selectedAudioTrack = track
                     if (track != null) mediaPlayer.setAudioTrack(track.id)
                     // Restore playing state if was playing
                     if (wasPlaying && !mediaPlayer.isPlaying) {
                         mediaPlayer.play()
                     }
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
                     // Remember playing state before track change
                     val wasPlaying = mediaPlayer.isPlaying
                     selectedSubtitleTrack = track
                     if (track != null) mediaPlayer.setSpuTrack(track.id) else mediaPlayer.setSpuTrack(-1)
                     // Restore playing state if was playing
                     if (wasPlaying && !mediaPlayer.isPlaying) {
                         mediaPlayer.play()
                     }
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
                      // Video track switching
                      if (track != null) mediaPlayer.setVideoTrack(track.id)
                      showQualityDialog = false
                      onInteraction()
                 },
                 includeOff = true,
                 offLabel = "Automático (Recomendado)"
             )
        }
    }
}

// Updated TrackInfo for VLC (id is Int)
data class TrackInfo(
    val id: Int,
    val name: String
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

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
