package com.rayeflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
// Imports removed (duplicates)
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rayeflix.app.model.Movie
import com.rayeflix.app.model.mockMovies
import com.rayeflix.app.ui.theme.DarkBackground
import com.rayeflix.app.ui.theme.DarkSurface
import com.rayeflix.app.ui.theme.GrayText
import com.rayeflix.app.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Side: Keyboard & Categories (Simplified for Mobile/Tablet touch)
        // Image 4 shows a full keyboard. We can simulate this column.
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
             TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search...", color = GrayText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = White,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                modifier = Modifier.fillMaxWidth().border(1.dp, GrayText, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Categories List
            val categories = listOf("Comedies", "Action", "Sci-Fi", "Horror", "Documentaries", "Anime", "Romance")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(categories) { category ->
                    Text(
                        text = category,
                        color = GrayText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { query = category }
                    )
                }
            }
        }

        // Right Side: Results Grid
        Column(modifier = Modifier.weight(1f).padding(24.dp)) {
            Text(
                text = "Your Search Recommendations",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mockMovies) { movie ->
                    Column {
                        AsyncImage(
                            model = movie.imageUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f) // Portrait aspect ratio
                                .clip(RoundedCornerShape(4.dp))
                        )
                         // Overlay logo or title could go here
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItem(movie: Movie) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(bottom = 2.dp)
            .background(DarkSurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = movie.imageUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
        )
        
        Text(
            text = movie.title,
            color = White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            fontWeight = FontWeight.Bold
        )
        
        Icon(
            imageVector = Icons.Default.PlayCircle,
            contentDescription = "Play",
            tint = White,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}
