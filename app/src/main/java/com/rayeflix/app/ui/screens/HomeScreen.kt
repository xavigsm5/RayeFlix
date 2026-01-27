package com.rayeflix.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
    val movies by viewModel.movies.collectAsState()
    val categorizedMovies by viewModel.categorizedMovies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Sort categories alphabetically or by custom order if needed
    val categories = categorizedMovies.keys.sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
    ) {
        TopBar(onNavigate = { route -> 
            if (route == "profile") {
                // Return to profile selection (pop logic handled by simple navigate if we want stack)
                 navController.navigate("profile") {
                     popUpTo("home") { inclusive = true }
                 }
            } else {
                navController.navigate(route)
            }
        })
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = com.rayeflix.app.ui.theme.NetflixRed)
            }
        } else if (movies.isNotEmpty()) {
            // Pick a random movie for Hero section from the first category or random
            val heroMovie = movies.shuffled().firstOrNull()
            
            heroMovie?.let {
                 HeroSection(movie = it, onPlayClick = { 
                     navController.navigate("player?url=${it.streamUrl}")
                 })
            }

            categories.forEach { category ->
                 // Skip if category has no items (shouldn't happen with map)
                 val categoryMovies = categorizedMovies[category] ?: emptyList()
                 if (categoryMovies.isNotEmpty()) {
                     MovieSection(
                         title = category, 
                         movies = categoryMovies, 
                         onMovieClick = { movie -> 
                             navController.navigate("player?url=${movie.streamUrl}")
                         }
                     )
                 }
            }
        } else {
             // Empty state or retry
             Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                 Text("No content found for this profile.", color = White)
             }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom bar
    }
}

@Composable
// Annotation duplicate removed
// Annotation duplicate removed
fun TopBar(currentSection: String = "Inicio", onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Logo
        Image(
            painter = painterResource(id = android.R.drawable.ic_media_play), // Placeholder for Netflix "N"
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clickable { onNavigate("home") }
        )
        
        Spacer(modifier = Modifier.width(48.dp))

        // Menu Items
        val menuItems = listOf("Inicio", "Series", "Películas", "Mi Netflix")

        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            menuItems.forEach { item ->
                val isSelected = item == currentSection
                if (isSelected) {
                     Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(White)
                            .clickable { 
                                // Map menu items to routes or actions
                                val route = when(item) {
                                    "Inicio" -> "home"
                                    "Mi Netflix" -> "mynetflix"
                                    else -> "home" // Placeholder for Series/Movies filters
                                }
                                onNavigate(route)
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                         Text(item, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = item,
                        color = GrayText,
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier.clickable { 
                             val route = when(item) {
                                "Inicio" -> "home" // Force reload home
                                "Mi Netflix" -> "mynetflix"
                                else -> "home"
                            }
                            onNavigate(route) 
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Search & Profile Icons (Right side)
        Icon(
            Icons.Default.Search, 
            contentDescription = "Search", 
            tint = White, 
            modifier = Modifier
                .size(28.dp)
                .clickable { onNavigate("search") }
        )
        Spacer(modifier = Modifier.width(24.dp))
        // Avatar placeholder -> Go to Profile Selection
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.Blue, RoundedCornerShape(4.dp))
                .clickable { onNavigate("profile") }
        )
    }
}

@Composable
fun HeroSection(movie: Movie, onPlayClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp) // Taller hero
    ) {
        AsyncImage(
            model = movie.imageUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay (Simulating the vignette in Image 3)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f), 
                            Color.Transparent, 
                            DarkBackground
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        // Left side gradient for text legibility
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
                .widthIn(max = 500.dp) // Limit width for TV style
        ) {
            // Title Logo Simulation
            Text(
                text = movie.title.uppercase(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = com.rayeflix.app.ui.theme.NetflixRed, // Use Red for title similarity
                lineHeight = 60.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Metadata Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("TV-MA", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Text("2024", color = GrayText, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("2 Seasons", color = GrayText, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.border(1.dp, GrayText, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp)) {
                    Text("HD", color = GrayText, fontSize = 10.sp)
                }
            }

            Text(
                text = movie.description.ifEmpty { "A fast-paced drama about a young tech genius who uncovers a global conspiracy." },
                color = White,
                fontSize = 16.sp,
                maxLines = 3,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = White),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reproducir", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { /* Info */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(4.dp),
                     contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Más info", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
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

    AsyncImage(
        model = movie.imageUrl,
        contentDescription = movie.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(110.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(4.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .border(2.dp, if (isFocused) White else Color.Transparent, RoundedCornerShape(4.dp))
            .clickable { onClick(movie) }
            .focusable() // Important for TV navigation
    )
}
