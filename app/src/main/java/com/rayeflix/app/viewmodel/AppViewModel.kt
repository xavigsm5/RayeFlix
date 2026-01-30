package com.rayeflix.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rayeflix.app.data.PlaylistRepository
import com.rayeflix.app.data.TmdbRepository
import com.rayeflix.app.data.TmdbMetadata
import com.rayeflix.app.model.Movie
import com.rayeflix.app.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    
    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    // Data Holders
    private var allMovies = listOf<Movie>()
    private var allSeries = listOf<com.rayeflix.app.model.Series>()
    private var allLive = listOf<Movie>()

    // UI States
    private val _displayedContent = MutableStateFlow<Map<String, List<Movie>>>(emptyMap())
    val displayedContent: StateFlow<Map<String, List<Movie>>> = _displayedContent.asStateFlow()
    
    private val _currentTab = MutableStateFlow("Inicio")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Movie>>(emptyList())
    val searchResults: StateFlow<List<Movie>> = _searchResults.asStateFlow()

    private val _profiles = MutableStateFlow<List<Profile>>(com.rayeflix.app.model.profiles)
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tmdbMetadata = MutableStateFlow<TmdbMetadata?>(null)
    val tmdbMetadata: StateFlow<TmdbMetadata?> = _tmdbMetadata.asStateFlow()


    fun onProfileSelected(profile: Profile) {
        _selectedProfile.value = profile
        if (profile.playlistUrl.isNotEmpty()) {
            fetchPlaylist(profile.playlistUrl)
        }
    }

    fun addProfile(name: String, playlistUrl: String) {
        val currentProfiles = _profiles.value
        val newId = (currentProfiles.maxOfOrNull { it.id } ?: 0) + 1
        val newProfile = Profile(
            id = newId,
            name = name,
            avatarUrl = "https://occ-0-3933-116.1.nflxso.net/dnm/api/v6/K6hjPJd6cR6FpVELC5Pd6ovFWzk/AAAABY5cwIbM7shRfcXkfo8Kt3jTMcN47qLM5PRq_dh5qJ8v7vM8f9r3y7s.png?r=fcd", // Random avatar
            playlistUrl = playlistUrl
        )
        // Insert before "Add Profile" button if it exists
        val listWithoutAdd = currentProfiles.filter { it.name != "Add Profile" }
        _profiles.value = listWithoutAdd + newProfile + Profile(999, "Add Profile", "", "")
    }

    fun updateProfile(profileId: Int, newName: String, newUrl: String) {
        _profiles.value = _profiles.value.map {
            if (it.id == profileId) it.copy(name = newName, playlistUrl = newUrl) else it
        }
    }

    fun deleteProfile(profileId: Int) {
        _profiles.value = _profiles.value.filter { it.id != profileId }
    }

    fun switchTab(tab: String) {
        _currentTab.value = tab
        updateDisplayedContent()
    }
    
    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        val q = query.lowercase()
        val movieMatches = allMovies.filter { it.title.lowercase().contains(q) }
        val liveMatches = allLive.filter { it.title.lowercase().contains(q) }
        
        // Series matches - map back to a "Movie" representation for search result list
        val seriesMatches = allSeries.filter { it.name.lowercase().contains(q) }.map { series ->
             Movie(
                id = -1, // Special ID
                title = series.name,
                imageUrl = series.coverUrl,
                description = "Series",
                streamUrl = "", // Not playable directly
                categories = listOf("Series"),
                type = com.rayeflix.app.model.ContentType.SERIES,
                seriesName = series.name
             )
        }
        
        _searchResults.value = movieMatches + seriesMatches + liveMatches
    }
    
    // Helper to find a specific series object
    fun getSeriesByName(name: String): com.rayeflix.app.model.Series? {
        return allSeries.find { it.name == name }
    }

    fun getMovieById(id: Int): Movie? {
        return allMovies.find { it.id == id } ?: allLive.find { it.id == id }
    }

    fun getSimilarMovies(movie: Movie): List<Movie> {
        val category = movie.categories.firstOrNull() ?: return emptyList()
        // Filter by category, exclude current movie, take 10 randoms
        return allMovies.filter { 
            it.id != movie.id && it.categories.contains(category) 
        }.shuffled().take(10)
    }

    fun fetchTmdbMetadata(title: String, isSeries: Boolean) {
        viewModelScope.launch {
            _tmdbMetadata.value = null // Reset
            val metadata = if (isSeries) {
                TmdbRepository.searchSeries(title)
            } else {
                TmdbRepository.searchMovie(title)
            }
            _tmdbMetadata.value = metadata
        }
    }

    fun clearTmdbMetadata() {
        _tmdbMetadata.value = null
    }


    private fun updateDisplayedContent() {
        val tab = _currentTab.value
        val maxItemsPerCategory = 20 // Limit to prevent ANR from too many images
        
        _displayedContent.value = when (tab) {
            "Series" -> {
                // Group Series by Category
                val grouped = allSeries.groupBy { it.category }
                val displayMap = mutableMapOf<String, List<Movie>>()
                
                grouped.forEach { (category, seriesList) ->
                    displayMap[category] = seriesList.map { series ->
                        Movie(
                            id = series.name.hashCode(),
                            title = series.name,
                            imageUrl = series.coverUrl.ifEmpty { "https://via.placeholder.com/300x450?text=${series.name}" },
                            description = "Series with ${series.episodes.size} Seasons",
                            streamUrl = "series://${series.name}",
                            categories = listOf(category),
                            type = com.rayeflix.app.model.ContentType.SERIES,
                            seriesName = series.name
                        )
                    }
                }
                displayMap
            }
            "TV en vivo" -> {
                allLive.groupBy { it.categories.firstOrNull() ?: "Canales" }
            }
            "Películas" -> {
                allMovies.groupBy { it.categories.firstOrNull() ?: "Películas" }
            }
            "TV Vivo" -> {
                // Show all live TV channels
                allLive.groupBy { it.categories.firstOrNull() ?: "Canales" }
            }
            else -> {
                // Home: Mix of everything - limited
                val allContent = allMovies.take(100) + allLive.take(50)
                allContent.groupBy { it.categories.firstOrNull() ?: "Otros" }
                    .mapValues { it.value.take(maxItemsPerCategory) }
            }
        }
    }

    private fun fetchPlaylist(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Fetch
            val content = PlaylistRepository.fetchPlaylist(url)
            
            allMovies = content.movies
            allSeries = content.series
            allLive = content.liveChannels
            
            // Set default view
            switchTab("Inicio")
            
            _isLoading.value = false
        }
    }
}
