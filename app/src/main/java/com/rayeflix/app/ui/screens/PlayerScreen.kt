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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoUrl: String, titleArg: String, subtitleArg: String, navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    
    // Focus Requester for TV Navigation
    val playButtonFocusRequester = remember { FocusRequester() }
    
    // Decode arguments
    val decodedUrl = remember(videoUrl) { android.net.Uri.decode(videoUrl) }
    val title = remember(titleArg) { android.net.Uri.decode(titleArg) }
    val subtitle = remember(subtitleArg) { android.net.Uri.decode(subtitleArg) }
    
    // Quality / Track Selection States
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Spanish") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(decodedUrl))
            prepare()
            playWhenReady = true
        }
    }

    // Update progress
    LaunchedEffect(exoPlayer) {
        while (true) {
            try {
                currentPosition = exoPlayer.currentPosition
                totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                isPlaying = exoPlayer.isPlaying
            } catch (e: Exception) {
                // Ignore crash if player is released
                break
            }
            delay(1000)
        }
    }

    // Auto hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls) {
            // When controls show, request focus on Play button to enable partial navigation
            if (isPlaying) { 
                 // Only auto-hide if playing. If paused, keep controls.
                 delay(4000)
                 showControls = false
            }
            // Request focus when controls appear
            try {
                playButtonFocusRequester.requestFocus()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                showControls = !showControls
            }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                             if (showControls) {
                                showControls = false
                                return@onKeyEvent true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN, android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                            if (!showControls) {
                                showControls = true
                                return@onKeyEvent true 
                            }
                            // Allow event to go to children (buttons)
                            return@onKeyEvent false
                        }
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
                // ... Top Overlay (same as before) ...
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
                                onClick = { exoPlayer.seekBack() },
                                modifier = Modifier
                                    .onFocusChanged { isReplayFocused = it.isFocused }
                                    .border(2.dp, if (isReplayFocused) White else Color.Transparent, CircleShape)
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "Replay 10s", tint = White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            var isSkipFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { exoPlayer.seekForward() },
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
                        
                        // Progress Bar - Focusable?
                        // Sliders are hard to focus on TV. Usually we just show them. 
                        // If user wants to seek, we use D-pad Left/Right which we capture at top level or on a specific "Seekbar" focusable.
                        // For now we skip focusing the slider directly to avoid trap.
                        Slider(
                             value = currentPosition.toFloat(),
                             onValueChange = {},
                             valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = NetflixRed,
                                 activeTrackColor = NetflixRed,
                                 inactiveTrackColor = Color.Gray
                             ),
                             modifier = Modifier.weight(1f) // Not focusable for now
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
                        // Chips mimicking functionality - In real app, query `exoPlayer.currentTracks`
                        PlayerOptionChip("Español", selectedLanguage == "Español") { 
                            selectedLanguage = "Español"
                            // Logic to switch track would go here:
                            // exoPlayer.trackSelectionParameters = ...
                        }
                        PlayerOptionChip("Inglés [Original]", selectedLanguage.startsWith("Ingl")) { 
                             selectedLanguage = "Inglés [Original]"
                        }
                        PlayerOptionChip("Español (Sub)", selectedLanguage == "Español (Sub)") {
                             selectedLanguage = "Español (Sub)"
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        var isSettingsFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showQualityDialog = true },
                            modifier = Modifier
                                .onFocusChanged { isSettingsFocused = it.isFocused }
                                .border(2.dp, if (isSettingsFocused) White else Color.Transparent, CircleShape)
                                .focusable()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = White)
                        }
                    }
                }
            }
        }
        
        if (showQualityDialog) {
             AlertDialog(
                 containerColor = Color.DarkGray, // Dark theme
                 onDismissRequest = { showQualityDialog = false },
                 title = { Text("Calidad de Video", color = White) },
                 text = {
                     Column {
                         listOf("Auto", "1080p", "720p", "480p").forEach { quality ->
                             var isQualityFocused by remember { mutableStateOf(false) }
                             Text(
                                 text = quality,
                                 color = White,
                                 modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        // Handle quality change logic here
                                        showQualityDialog = false 
                                    }
                                    .onFocusChanged { isQualityFocused = it.isFocused }
                                    .background(if (isQualityFocused) Color.White.copy(alpha=0.1f) else Color.Transparent)
                                    .focusable()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                 fontSize = 16.sp
                             )
                         }
                     }
                 },
                 confirmButton = {
                     TextButton(onClick = { showQualityDialog = false }) {
                         Text("Cerrar", color = NetflixRed)
                     }
                 }
             )
        }
    }
}

@Composable
fun PlayerOptionChip(text: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) White else if (isFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent)
            .border(2.dp, if(isFocused) White else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (isSelected) Color.Black else White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
