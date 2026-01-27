package com.rayeflix.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayeflix.app.data.PlaylistRepository
import com.rayeflix.app.model.Movie
import com.rayeflix.app.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    
    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()
    
    // Mutable profiles to support adding new ones
    private val _profiles = MutableStateFlow<List<Profile>>(com.rayeflix.app.model.profiles)
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()
    
    // Categorized movies for Home Screen
    private val _categorizedMovies = MutableStateFlow<Map<String, List<Movie>>>(emptyMap())
    val categorizedMovies: StateFlow<Map<String, List<Movie>>> = _categorizedMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onProfileSelected(profile: Profile) {
        _selectedProfile.value = profile
        if (profile.playlistUrl.isNotEmpty()) {
            fetchPlaylist(profile.playlistUrl)
        } else {
             // Handle Add Profile or Empty
        }
    }

    fun addProfile(name: String, playlistUrl: String) {
        val newId = (_profiles.value.maxOfOrNull { it.id } ?: 0) + 1
        val newProfile = Profile(
            id = newId,
            name = name,
            avatarUrl = "https://occ-0-3933-116.1.nflxso.net/dnm/api/v6/K6hjPJd6cR6FpVELC5Pd6ovFWzk/AAAABY5cwIbM7shRfcXkfo8Kt3jTMcN47qLM5PRq_dh5qJ8v7vM8f9r3y7s.png?r=fcd", // Random avatar
            playlistUrl = playlistUrl
        )
        // Add before the last one if assuming last is "Add Profile", but here we handle "Add" logic in UI
        _profiles.value = _profiles.value.filter { it.name != "Add Profile" } + newProfile + Profile(999, "Add Profile", "", "")
    }

    private fun fetchPlaylist(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Clear old data to free memory before loading new
            _movies.value = emptyList()
            _categorizedMovies.value = emptyMap()
            
            val (allMovies, categorized) = PlaylistRepository.fetchPlaylist(url)
            _movies.value = allMovies
            _categorizedMovies.value = categorized
            _isLoading.value = false
        }
    }
}
