package com.rayeflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rayeflix.app.model.Movie
import com.rayeflix.app.ui.theme.DarkBackground
import com.rayeflix.app.ui.theme.DarkSurface
import com.rayeflix.app.ui.theme.NetflixRed
import com.rayeflix.app.ui.theme.White
import com.rayeflix.app.viewmodel.AppViewModel

@Composable
fun SeriesDetailScreen(
    navController: NavController,
    viewModel: AppViewModel,
    seriesName: String
) {
    val series = remember(seriesName) { viewModel.getSeriesByName(seriesName) }
    val tmdbInfo by viewModel.tmdbMetadata.collectAsState()
    
    // Trigger fetch on enter
    LaunchedEffect(seriesName) {
        viewModel.fetchTmdbMetadata(seriesName, isSeries = true)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearTmdbMetadata()
        }
    }
    
    // State for selected season
    var selectedSeasonNumber by remember { mutableStateOf(1) }
    
    // Ensure we default to the first available season if season 1 doesn't exist
    LaunchedEffect(series) {
        if (series != null && !series.episodes.containsKey(selectedSeasonNumber)) {
            series.episodes.keys.minOrNull()?.let { selectedSeasonNumber = it }
        }
    }

    if (series == null) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
            Text("Series not found", color = White)
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        }
        return
    }

    val episodes = series.episodes[selectedSeasonNumber] ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Background Image with gradient overlay
        AsyncImage(
            model = tmdbInfo?.backdropUrl ?: series.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.3f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkBackground.copy(alpha = 0.8f),
                            DarkBackground.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // LEFT COLUMN: Info & Seasons
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .padding(end = 24.dp)
            ) {
                // Back Button
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = series.name,
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Metadata
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${series.episodes.size} Temporadas",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = series.category,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // Description from TMDB
                tmdbInfo?.description?.let { desc ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = desc,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        maxLines = 4,
                        lineHeight = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Seasons List
                Text("Temporadas", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(series.episodes.keys.sorted()) { seasonNum ->
                        val isSelected = seasonNum == selectedSeasonNumber
                        var isFocused by remember { mutableStateOf(false) }
                        
                        val backgroundColor = when {
                            isSelected -> NetflixRed
                            isFocused -> Color.White.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(backgroundColor)
                                .border(2.dp, if(isFocused && !isSelected) White else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { selectedSeasonNumber = seasonNum }
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Temporada $seasonNum", 
                                color = White, 
                                fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                "${series.episodes[seasonNum]?.size ?: 0} episodios", 
                                color = if(isSelected) White else Color.Gray, 
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            // RIGHT COLUMN: Episodes
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .background(Color.Transparent) // Cleaner look
            ) {
                Text(
                    text = "Temporada $selectedSeasonNumber",
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(episodes) { episode ->
                        EpisodeItem(episode = episode, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(episode: Movie, navController: NavController) {
    var isFocused by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.1f) else DarkSurface)
            .border(2.dp, if (isFocused) White else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { 
                val encodedUrl = android.net.Uri.encode(episode.streamUrl)
                val encodedTitle = android.net.Uri.encode(episode.title) // Use episode title
                val encodedSubtitle = android.net.Uri.encode("T${episode.seasonNumber}:E${episode.episodeNumber}")
                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&subtitle=$encodedSubtitle")
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(8.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .aspectRatio(16f/9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
        ) {
             AsyncImage(
                model = episode.imageUrl, // Episode specific image if available, else Series cover needs to be passed? 
                // Currently repository puts Series cover in episodes usually if specific parsing not done, or specific image.
                // If URL is empty, fallback.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Play overlay
            if (isFocused) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Info
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "E${episode.episodeNumber}: ${episode.title}",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = episode.description.takeIf { it.isNotEmpty() && !it.startsWith("Group:") } ?: "Descripción no disponible",
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 3,
                lineHeight = 16.sp
            )
        }
    }
}


