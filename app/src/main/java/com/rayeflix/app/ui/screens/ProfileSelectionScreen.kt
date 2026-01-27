package com.rayeflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.rayeflix.app.model.Profile
import com.rayeflix.app.ui.theme.DarkBackground
import com.rayeflix.app.ui.theme.NetflixRed
import com.rayeflix.app.ui.theme.White
import com.rayeflix.app.viewmodel.AppViewModel

@Composable
fun ProfileSelectionScreen(
    viewModel: AppViewModel,
    onProfileClick: (Profile) -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    var selectedProfileId by remember { mutableStateOf(profiles.firstOrNull()?.id ?: 1) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    // Find the currently "hovered/selected" profile to show its background
    val activeProfile = profiles.find { it.id == selectedProfileId }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Background Image (Dynamic based on selected profile)
        activeProfile?.let { profile ->
            // Use a random movie image associated with profile or generic one if avatar
            AsyncImage(
                model = androidx.compose.ui.platform.LocalContext.current.let { context ->
                    coil.request.ImageRequest.Builder(context)
                        .data("https://image.tmdb.org/t/p/w1280/r7DuyYJ0N3cD8bRKsR5Ygq2P7oa.jpg")
                        .crossfade(true)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startX = 0f,
                            endX = 1000f
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sidebar List
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp) // Fixed sidebar width
                    .background(Color.Black.copy(alpha = 0.5f)) // Slight scrim
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "RayeFlix",
                    color = NetflixRed,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
                )
                Text(
                    text = "Elige un perfil",
                    color = White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 32.dp).align(Alignment.Start)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profiles) { profile ->
                        if (profile.name == "Add Profile") {
                            AddProfileButton(
                                isSelected = profile.id == selectedProfileId,
                                onClick = { 
                                    selectedProfileId = profile.id 
                                    showAddProfileDialog = true
                                }
                            )
                        } else {
                            ProfileSidebarItem(
                                profile = profile,
                                isSelected = profile.id == selectedProfileId,
                                onClick = { 
                                    selectedProfileId = profile.id
                                    onProfileClick(profile) 
                                },
                                onHover = { selectedProfileId = profile.id }
                            )
                        }
                    }
                }
            }
            
            // Right side area (Empty, reveals background)
            Box(modifier = Modifier.weight(1f)) {
                 activeProfile?.let { 
                     Text(
                        text = it.name,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
                     )
                 }
            }
        }
    }

    if (showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onAdd = { name, url ->
                viewModel.addProfile(name, url)
                showAddProfileDialog = false
            }
        )
    }
}

@Composable
fun ProfileSidebarItem(
    profile: Profile, 
    isSelected: Boolean, 
    onClick: () -> Unit,
    onHover: () -> Unit // Simulate selection for composition
) {
    // In mobile, click is select. In TV, focus is select. 
    // We treat click as select for now, but highlight "selectedProfileId"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onHover() // Select first
                onClick() // Then enter
            }
    ) {
        AsyncImage(
            model = profile.avatarUrl,
            contentDescription = profile.name,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) White else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = profile.name,
            color = if (isSelected) White else Color.LightGray,
            fontSize = if (isSelected) 22.sp else 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AddProfileButton(isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.Transparent)
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) White else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
       // Text("Añadir perfil", color = Color.Gray, fontSize = 18.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Añadir Perfil", fontSize = 20.sp, color = White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NetflixRed,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL Lista IPTV") },
                     colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NetflixRed,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        cursorColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(name, url) },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
                    ) {
                        Text("Guardar", color = White)
                    }
                }
            }
        }
    }
}

// Extension for alpha since modifier.alpha needs import
// End of file
