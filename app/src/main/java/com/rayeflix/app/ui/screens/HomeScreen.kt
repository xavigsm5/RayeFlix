package com.rayeflix.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rayeflix.app.model.Movie
import com.rayeflix.app.model.mockMovies
import com.rayeflix.app.viewmodel.AppViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import com.rayeflix.app.ui.theme.DarkBackground
import com.rayeflix.app.ui.theme.DarkSurface
import com.rayeflix.app.ui.theme.White
import com.rayeflix.app.ui.theme.GrayText

@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val scrollState = rememberScrollState()
    val displayedContent by viewModel.displayedContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    
    
    // Sort categories alphabetically or by custom order if needed
    
    // Sort categories alphabetically or by custom order if needed
    val categories = displayedContent.keys.sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        TopBar(
            currentSection = currentTab, 
            onNavigate = { route -> 
                if (route == "search" || route == "profile") {
                     navController.navigate(route)
                }
            },
            onTabSelected = { tab ->
                viewModel.switchTab(tab)
            }
        )
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = com.rayeflix.app.ui.theme.NetflixRed)
            }
        } else {
             // Main Content Scrollable
             Column(modifier = Modifier.verticalScroll(scrollState)) {
                 
                 // Hero Section (Random item from first category)
                 val firstCategory = categories.firstOrNull()
                 val heroItem = if (firstCategory != null) displayedContent[firstCategory]?.shuffled()?.firstOrNull() else null
                 
                 heroItem?.let { item ->
                    HeroSection(movie = item, onPlayClick = {
                        if (item.type == com.rayeflix.app.model.ContentType.SERIES) {
                            item.seriesName?.let { name ->
                                val encoded = android.net.Uri.encode(name)
                                navController.navigate("series_detail/$encoded")
                            }
                        } else if (item.type == com.rayeflix.app.model.ContentType.LIVE) {
                            val encodedUrl = android.net.Uri.encode(item.streamUrl)
                            val encodedTitle = android.net.Uri.encode(item.title)
                            val encodedSubtitle = android.net.Uri.encode("En Vivo")
                            navController.navigate("player?url=$encodedUrl&title=$encodedTitle&subtitle=$encodedSubtitle")
                        } else {
                            navController.navigate("movie_detail/${item.id}")
                        }
                    })
                 }

                 categories.forEach { category ->
                     val items = displayedContent[category] ?: emptyList()
                     if (items.isNotEmpty()) {
                         MovieSection(
                             title = category, 
                             movies = items, 
                             onMovieClick = { movie -> 
                                if (movie.type == com.rayeflix.app.model.ContentType.SERIES) {
                                    movie.seriesName?.let { name ->
                                        val encoded = android.net.Uri.encode(name)
                                        navController.navigate("series_detail/$encoded")
                                    }
                                } else if (movie.type == com.rayeflix.app.model.ContentType.LIVE) {
                                     val encodedUrl = android.net.Uri.encode(movie.streamUrl)
                                     val encodedTitle = android.net.Uri.encode(movie.title)
                                     val encodedSubtitle = android.net.Uri.encode(category)
                                     navController.navigate("player?url=$encodedUrl&title=$encodedTitle&subtitle=$encodedSubtitle")
                                } else {
                                     navController.navigate("movie_detail/${movie.id}")
                                }
                             }
                         )
                     }
                 }
                 
                 Spacer(modifier = Modifier.height(100.dp))
             }
        }
    }
    
    // Series Detail Dialog / Bottom Sheet

}

@Composable
fun TopBar(currentSection: String, onNavigate: (String) -> Unit, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Text(
            text = "RayeFlix",
            color = com.rayeflix.app.ui.theme.NetflixRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onTabSelected("Inicio") }
                .padding(end = 48.dp)
        )

        // Menu Items
        val menuItems = listOf("Inicio", "Series", "Películas", "TV en vivo")

        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            menuItems.forEach { item ->
                val isSelected = item == currentSection
                var isFocused by remember { mutableStateOf(false) }
                
                // Keep the focused/selected logic
                val isActive = isFocused
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) White else Color.Transparent)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            onTabSelected(item)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                     Text(
                         text = item, 
                         color = if (isActive) Color.Black else if (isSelected) White else GrayText, 
                         fontWeight = FontWeight.Bold, 
                         fontSize = 16.sp
                     )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Search & Profile
        Icon(
            Icons.Default.Search, 
            contentDescription = "Search", 
            tint = White, 
            modifier = Modifier
                .size(28.dp)
                .clickable { onNavigate("search") }
        )
        Spacer(modifier = Modifier.width(24.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.Blue, RoundedCornerShape(4.dp))
                .clickable { onNavigate("profile") }
        )
    }
}

@Composable
fun SeriesDetailDialog(
    series: com.rayeflix.app.model.Series,
    onDismiss: () -> Unit,
    onEpisodeClick: (Movie) -> Unit
) {
    var selectedSeason by remember { mutableStateOf(series.episodes.keys.minOrNull() ?: 1) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Cover Image
                AsyncImage(
                    model = series.coverUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                
                // Details
                Column(modifier = Modifier.weight(2f).padding(24.dp)) {
                    Text(series.name, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Season Selector
                    Text("Temporadas", color = GrayText, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(series.episodes.keys.sorted()) { season ->
                            val isSelected = season == selectedSeason
                            Button(
                                onClick = { selectedSeason = season },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) com.rayeflix.app.ui.theme.NetflixRed else Color.DarkGray
                                )
                            ) {
                                Text("Temporada $season", color = White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Episodes List
                    val episodes = series.episodes[selectedSeason] ?: emptyList()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(episodes) { episode ->
                            var isFocused by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isFocused) Color.Gray else Color.Transparent)
                                    .clickable { onEpisodeClick(episode) }
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${episode.episodeNumber}. ${episode.title.substringAfter("E${episode.episodeNumber}")}", 
                                    color = White, 
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

// Re-using HeroSection, MovieSection, MovieItem from previous file (assuming imports are cleaner in replace, 
// but since I'm rewriting the file content partially I need to be careful).
// The user tool `replace_file_content` replaces a contiguous block. 
// I should make sure I include the rest of the functions if I'm replacing the whole file implicitly or relying on block definition.
// The previous file had HeroSection, MovieSection, MovieItem. I will include them here to be safe and ensure they handle the new logic if needed.
// Actually, MovieItem handles generic Movie. HeroSection handles generic Movie. They should work fine with "Series as Movie".

@Composable
fun HeroSection(movie: Movie, onPlayClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
    ) {
        AsyncImage(
            model = movie.imageUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // ... (Gradients kept simple for brevity in update, but logic is same)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, DarkBackground),
                        startY = 0f, endY = Float.POSITIVE_INFINITY
                    )
                )
        )
         Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        endX = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp, bottom = 48.dp)
                .widthIn(max = 500.dp)
        ) {
            Text(
                text = movie.title.uppercase(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = com.rayeflix.app.ui.theme.NetflixRed,
                lineHeight = 60.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = movie.description.ifEmpty { "Explora este contenido exclusivo en RayeFlix." },
                color = White,
                fontSize = 16.sp,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = White),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (movie.type == com.rayeflix.app.model.ContentType.SERIES) "Ver Temporadas" else "Reproducir", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MovieSection(title: String, movies: List<Movie>, onMovieClick: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(movies) { movie ->
                 MovieItem(movie, onMovieClick)
            }
        }
    }
}

@Composable
fun MovieItem(movie: Movie, onClick: (Movie) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null 
            ) { onClick(movie) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(4.dp, if (isFocused) White else Color.Transparent, RoundedCornerShape(4.dp))
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray)
        ) {
            if (movie.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = movie.imageUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(movie.title.take(1).uppercase(), color = White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Series Indicator
            if (movie.type == com.rayeflix.app.model.ContentType.SERIES) {
                 Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(com.rayeflix.app.ui.theme.NetflixRed, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp)) {
                     Text("SERIE", color = White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                 }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.title,
            color = if (isFocused) White else GrayText,
            fontSize = 12.sp,
            maxLines = 1,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
