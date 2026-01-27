package com.rayeflix.app.data

import com.rayeflix.app.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object PlaylistRepository {

    private val client = OkHttpClient()

    // Keywords to filter out
    private val blockedKeywords = listOf(
        "porn", "xxx", "adult", "sex", "18+", "erotic", "hentai", "nude", "brazzers", "playboy", "hustler"
    )

    suspend fun fetchPlaylist(url: String): Pair<List<Movie>, Map<String, List<Movie>>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext Pair(emptyList(), emptyMap())
                }

                val inputStream = response.body?.byteStream()
                if (inputStream != null) {
                    return@withContext parseM3U(inputStream)
                } else {
                    return@withContext Pair(emptyList(), emptyMap())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext Pair(emptyList(), emptyMap())
            }
        }
    }

    private fun parseM3U(inputStream: InputStream): Pair<List<Movie>, Map<String, List<Movie>>> {
        val movies = mutableListOf<Movie>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        var currentTitle: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        
        // Regex to parse EXTINF line
        // Example: #EXTINF:-1 tvg-id="" tvg-name="HBO" tvg-logo="http://..." group-title="Movies", HBO HD
        // This is a naive parser.
        
        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                // Extract attributes
                currentLogo = extractAttribute(trimmed, "tvg-logo")
                currentGroup = extractAttribute(trimmed, "group-title") ?: "Uncategorized"
                
                // Debug log
                println("DEBUG: Found Entry Group: $currentGroup Logo: $currentLogo")

                
                // Extract Title (everything after the last comma)
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentTitle = trimmed.substring(commaIndex + 1).trim()
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                // This is the URL
                if (currentTitle != null) {
                    if (!isAdultContent(currentTitle!!, currentGroup!!)) {
                         val movie = Movie(
                            id = movies.size, // Temporary ID
                            title = currentTitle!!,
                            imageUrl = if (!currentLogo.isNullOrEmpty()) currentLogo!! else "https://via.placeholder.com/300x450?text=No+Image",
                            description = "Group: $currentGroup",
                            streamUrl = trimmed,
                            categories = listOf(currentGroup!!) // Store group as a category
                        )
                         movies.add(movie)
                    }
                }
                // Reset for next entry
                currentTitle = null
                currentLogo = null
                currentGroup = null
            }
        }
        
        // Group by category for the UI
        println("DEBUG: Parsed ${movies.size} movies")
        val categorized = movies.groupBy { it.categories.firstOrNull() ?: "Uncategorized" }
        
        return Pair(movies, categorized)
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = Regex(pattern)
        val match = regex.find(line)
        return match?.groupValues?.get(1)
    }

    private fun isAdultContent(title: String, group: String): Boolean {
        val text = "$title $group".lowercase()
        return blockedKeywords.any { text.contains(it) }
    }
}
