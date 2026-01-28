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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable

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
fun SearchScreen(navController: androidx.navigation.NavController, viewModel: com.rayeflix.app.viewmodel.AppViewModel) {
    var query by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Side: Keyboard & Categories
        Column(
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
             TextField(
                value = query,
                onValueChange = { 
                    query = it
                    viewModel.search(it)
                },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // On screen keyboard grid (Simulated functionality)
            val keys = listOf(
                "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R",
                "S", "T", "U", "V", "W", "X",
                "Y", "Z", "1", "2", "3", "4",
                "5", "6", "7", "8", "9", "0"
            )
            
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(6),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(keys) { key ->
                    var isFocused by remember { mutableStateOf(false) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFocused) Color.LightGray else Color.Transparent)
                            .border(1.dp, if (isFocused) White else Color.Gray.copy(alpha=0.5f), RoundedCornerShape(4.dp))
                            .clickable { 
                                val newQ = query + key 
                                query = newQ
                                viewModel.search(newQ)
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                    ) {
                        Text(key, color = if (isFocused) Color.Black else White, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Space bar
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                     var isFocused by remember { mutableStateOf(false) }
                     Box(
                         contentAlignment = Alignment.Center,
                         modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFocused) Color.LightGray else Color.Transparent)
                             .border(1.dp, if (isFocused) White else Color.Gray.copy(alpha=0.5f), RoundedCornerShape(4.dp))
                            .clickable { 
                                val newQ = query + " "
                                query = newQ
                                viewModel.search(newQ)
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                     ) { 
                         Text("SPACE", color = if (isFocused) Color.Black else White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                     }
                }
                // Backspace
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                     var isFocused by remember { mutableStateOf(false) }
                     Box(
                         contentAlignment = Alignment.Center,
                         modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFocused) Color.LightGray else Color.Transparent)
                             .border(1.dp, if (isFocused) White else Color.Gray.copy(alpha=0.5f), RoundedCornerShape(4.dp))
                            .clickable { 
                                if (query.isNotEmpty()) {
                                    val newQ = query.dropLast(1)
                                    query = newQ
                                    viewModel.search(newQ)
                                }
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                     ) { 
                         Icon(Icons.Filled.Backspace, contentDescription = "Backspace", tint = if (isFocused) Color.Black else White)
                     }
                }
            }
        }

        // Right Side: Results Grid
        Column(modifier = Modifier.weight(1f).padding(24.dp)) {
            Text(
                text = if (query.isEmpty()) "Comience a escribir para buscar..." else "Resultados para \"$query\"",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(searchResults) { movie ->
                    var isFocused by remember { mutableStateOf(false) }
                    Column {
                        AsyncImage(
                            model = movie.imageUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(4.dp))
                                .onFocusChanged { isFocused = it.isFocused }
                                .border(3.dp, if (isFocused) White else Color.Transparent, RoundedCornerShape(4.dp))
                                .focusable()
                                .clickable { 
                                    if(movie.type == com.rayeflix.app.model.ContentType.SERIES) {
                                         val encodedName = android.net.Uri.encode(movie.seriesName ?: movie.title)
                                         navController.navigate("series_detail/$encodedName")
                                    } else if (movie.type == com.rayeflix.app.model.ContentType.LIVE) {
                                        val encodedUrl = android.net.Uri.encode(movie.streamUrl)
                                        val encodedTitle = android.net.Uri.encode(movie.title)
                                        val encodedSubtitle = android.net.Uri.encode("Busqueda")
                                        navController.navigate("player?url=$encodedUrl&title=$encodedTitle&subtitle=$encodedSubtitle")
                                    } else {
                                        navController.navigate("movie_detail/${movie.id}")
                                    }
                                }
                        )
                        if (isFocused) {
                            Text(movie.title, color = White, maxLines = 1, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
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
