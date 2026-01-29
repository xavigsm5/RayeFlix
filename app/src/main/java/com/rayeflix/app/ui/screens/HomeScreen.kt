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
import androidx.compose.ui.unit.Dp
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
import androidx.compose.runtime.LaunchedEffect
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
                    HeroSection(
                        movie = item, 
                        onPlayClick = {
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
                                // Play directly
                                val encodedUrl = android.net.Uri.encode(item.streamUrl)
                                val encodedTitle = android.net.Uri.encode(item.title)
                                navController.navigate("player?url=$encodedUrl&title=$encodedTitle&subtitle=")
                            }
                        },
                        onInfoClick = {
                            if (item.type == com.rayeflix.app.model.ContentType.SERIES) {
                                item.seriesName?.let { name ->
                                    val encoded = android.net.Uri.encode(name)
                                    navController.navigate("series_detail/$encoded")
                                }
                            } else {
                                navController.navigate("movie_detail/${item.id}")
                            }
                        }
                    )
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
    val menuItems = listOf("Inicio", "Series", "Películas", "TV Vivo")
    var selectedIndex by remember { mutableStateOf(menuItems.indexOf(currentSection).coerceAtLeast(0)) }
    
    // Item widths for pill positioning
    val itemWidths = listOf(60.dp, 55.dp, 75.dp, 70.dp) // TV Vivo is shorter
    val spacing = 12.dp
    
    // Calculate offset for the sliding pill
    fun getItemOffset(index: Int): Dp {
        // Account for search icon (24dp + 16dp spacing)
        var offset = 40.dp
        for (i in 0 until index) {
            offset += itemWidths[i] + spacing
        }
        return offset
    }
    
    // Update selectedIndex when currentSection changes
    LaunchedEffect(currentSection) {
        val index = menuItems.indexOf(currentSection)
        if (index >= 0) selectedIndex = index
    }
    
    // Animated pill position
    val pillOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = getItemOffset(selectedIndex),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        )
    )
    
    val pillWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = itemWidths[selectedIndex],
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        )
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Profile Image with dropdown arrow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onNavigate("profile") }
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.DarkGray)
            ) {
                Text(
                    "P", 
                    color = White, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text("▼", color = White, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
        }
        
        // Center: Search + Navigation (weighted to center)
        Spacer(modifier = Modifier.weight(1f))
        
        Box(
            modifier = Modifier.height(40.dp)
        ) {
            // Animated sliding pill background
            Box(
                modifier = Modifier
                    .offset(x = pillOffset)
                    .width(pillWidth)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(White)
            )
            
            // Menu items row with search icon at the start
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                // Search Icon (before menu items)
                var isSearchFocused by remember { mutableStateOf(false) }
                Icon(
                    Icons.Default.Search, 
                    contentDescription = "Search", 
                    tint = if (isSearchFocused) White else Color.Gray, 
                    modifier = Modifier
                        .size(24.dp)
                        .onFocusChanged { isSearchFocused = it.isFocused }
                        .focusable()
                        .clickable { onNavigate("search") }
                )
                
                // Menu Items
                menuItems.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    var isFocused by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .width(itemWidths[index])
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .onFocusChanged { 
                                isFocused = it.isFocused
                                if (it.isFocused) selectedIndex = index
                            }
                            .focusable()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { 
                                selectedIndex = index
                                onTabSelected(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item, 
                            color = if (isSelected) Color.Black else if (isFocused) White else Color.Gray, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Right: Netflix-style "N" or "R" Logo
        Text(
            text = "R",
            color = com.rayeflix.app.ui.theme.NetflixRed,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
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
fun HeroSection(movie: Movie, onPlayClick: () -> Unit, onInfoClick: () -> Unit = {}) {
    var isCardFocused by remember { mutableStateOf(false) }
    var isPlayFocused by remember { mutableStateOf(false) }
    var isInfoFocused by remember { mutableStateOf(false) }
    
    // Main focusable card container
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Hero Card - entire card is focusable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isCardFocused) 4.dp else 0.dp,
                    color = if (isCardFocused) White else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .onFocusChanged { isCardFocused = it.isFocused }
                .focusable()
                .clickable { onPlayClick() }
        ) {
            // Background Image
            AsyncImage(
                model = movie.imageUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                            endX = 600f
                        )
                    )
            )

            // Content at bottom left
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 24.dp, end = 150.dp)
                    .widthIn(max = 500.dp)
            ) {
                // Movie Title
                Text(
                    text = movie.title.uppercase(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    lineHeight = 40.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val category = movie.categories.firstOrNull() ?: "Película"
                    Text(text = category, color = White, fontSize = 13.sp)
                    Text(text = " • ", color = Color.Gray, fontSize = 13.sp)
                    Text(text = "2024", color = White, fontSize = 13.sp)
                    Text(text = " • ", color = Color.Gray, fontSize = 13.sp)
                    Text(text = "1h 45 min", color = White, fontSize = 13.sp)
                    Text(text = " • ", color = Color.Gray, fontSize = 13.sp)
                    Text(text = "13+", color = White, fontSize = 13.sp)
                }
                
                // Description
                Text(
                    text = movie.description.ifEmpty { 
                        "Explora este contenido exclusivo disponible en RayeFlix."
                    },
                    color = White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2
                )
            }
            
            // "Recién agregado" badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(com.rayeflix.app.ui.theme.NetflixRed, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recién agregado", color = White, fontSize = 11.sp)
                }
            }
        }
        
        // Buttons Row - below the card
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp, start = 8.dp)
        ) {
            // Reproducir Button
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlayFocused) Color.White else Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier
                    .onFocusChanged { isPlayFocused = it.isFocused }
                    .focusable()
            ) {
                Icon(
                    Icons.Default.PlayArrow, 
                    contentDescription = null, 
                    tint = Color.Black, 
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Reproducir", 
                    color = Color.Black, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Más Info Button
            Button(
                onClick = onInfoClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInfoFocused) Color.Gray else Color.DarkGray.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier
                    .onFocusChanged { isInfoFocused = it.isFocused }
                    .focusable()
            ) {
                Icon(
                    Icons.Default.Info, 
                    contentDescription = null, 
                    tint = White, 
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Más Info", 
                    color = White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold
                )
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
