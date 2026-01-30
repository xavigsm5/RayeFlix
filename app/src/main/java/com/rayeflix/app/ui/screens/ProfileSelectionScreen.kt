package com.rayeflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    
    // Edit state
    var editingProfile by remember { mutableStateOf<Profile?>(null) }

    // Find the currently "hovered/selected" profile to show its background
    val activeProfile = profiles.find { it.id == selectedProfileId }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Simple gradient background - no external image loading for better performance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1a1a2e),
                            Color(0xFF16213e),
                            DarkBackground
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Sidebar List
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(420.dp) // Wider sidebar for edit layout
                    .background(Color.Transparent) 
                    .padding(start = 60.dp, top = 60.dp, bottom = 60.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Text
                Text(
                    text = "RayeFlix",
                    color = NetflixRed,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
                )
                Text(
                    text = "¿Quién eres?",
                    color = Color.LightGray,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(bottom = 32.dp).align(Alignment.Start)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(profiles) { profile ->
                        ProfileSidebarItem(
                            profile = profile,
                            isSelected = profile.id == selectedProfileId,
                            onClick = { 
                                selectedProfileId = profile.id
                                onProfileClick(profile) 
                            },
                            onHover = { selectedProfileId = profile.id },
                            onEditClick = { editingProfile = profile }
                        )
                    }
                    
                    // Always show Add Profile button at the end
                    item {
                        AddProfileButton(
                            isSelected = false,
                            onClick = { showAddProfileDialog = true }
                        )
                    }
                }
            }
            
            Box(modifier = Modifier.weight(1f))
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
    
    if (editingProfile != null) {
        EditProfileDialog(
            profile = editingProfile!!,
            onDismiss = { editingProfile = null },
            onSave = { id, name, url ->
                viewModel.updateProfile(id, name, url)
                editingProfile = null
            },
            onDelete = { id ->
                viewModel.deleteProfile(id)
                editingProfile = null
            }
        )
    }
}

@Composable
fun ProfileSidebarItem(
    profile: Profile, 
    isSelected: Boolean, 
    onClick: () -> Unit,
    onHover: () -> Unit,
    onEditClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var isEditFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Main Profile Click Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { 
                    isFocused = it.isFocused 
                    if (isFocused) onHover()
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null 
                ) { 
                    onHover()
                    onClick() 
                }
                .focusable()
        ) {
            // Avatar - Simple text-based avatar for better performance
            val avatarColors = listOf(
                Color(0xFFE50914), // Netflix red
                Color(0xFF0077B5), // Blue
                Color(0xFF00875A), // Green  
                Color(0xFFB24D00), // Orange
                Color(0xFF7B2D8E)  // Purple
            )
            val avatarColor = avatarColors[profile.id % avatarColors.size]
            
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .border(
                        width = if (isFocused) 3.dp else 0.dp,
                        color = if (isFocused) White else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(2.dp) 
                    .clip(RoundedCornerShape(4.dp))
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = profile.name,
                color = if (isFocused) White else Color.Gray,
                fontSize = if (isFocused) 22.sp else 18.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f).animateContentSize()
            )
        }

        // Edit Button (Focusable separately via Right Key)
        IconButton(
            onClick = { onEditClick() },
            modifier = Modifier
                .onFocusChanged { isEditFocused = it.isFocused }
                .border(2.dp, if (isEditFocused) White else Color.Transparent, CircleShape)
                .focusable()
        ) {
            Icon(
                Icons.Default.Edit, 
                contentDescription = "Edit", 
                tint = if(isEditFocused) White else Color.Gray
            )
        }
    }
}

@Composable
fun AddProfileButton(isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) White else Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(if(isFocused) 2.dp else 0.dp)
                .background(Color.Transparent, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
             Icon(Icons.Default.Add, contentDescription = "Add", tint = White, modifier = Modifier.size(32.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
         Text(
            text = "Agregar Perfil",
            color = if (isFocused) White else Color.Gray,
            fontSize = if (isFocused) 22.sp else 18.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(profile: Profile, onDismiss: () -> Unit, onSave: (Int, String, String) -> Unit, onDelete: (Int) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var url by remember { mutableStateOf(profile.playlistUrl) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Editar Perfil", fontSize = 20.sp, color = White, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (showDeleteConfirm) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("¿Eliminar perfil?", color = Color.Red)
                        Row {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("No", color = Color.Gray)
                            }
                            TextButton(onClick = { onDelete(profile.id) }) {
                                Text("Sí, eliminar", color = Color.Red)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Eliminar", color = Color.Red)
                        }

                        Row {
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    if (name.isNotBlank()) {
                                        onSave(profile.id, name.trim(), url.trim()) 
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                enabled = name.isNotBlank()
                            ) {
                                Text("Guardar", color = White)
                            }
                        }
                    }
                }
            }
        }
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (name.isNotBlank() && url.isNotBlank()) {
                                onAdd(name.trim(), url.trim()) 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                        enabled = name.isNotBlank() && url.isNotBlank()
                    ) {
                        Text("Guardar", color = White)
                    }
                }
            }
        }
    }
}
