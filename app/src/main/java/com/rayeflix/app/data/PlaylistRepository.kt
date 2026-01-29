package com.rayeflix.app.data

import com.rayeflix.app.model.Movie
import com.rayeflix.app.model.Series

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object PlaylistRepository {

    private val client = NetworkClient.okHttpClient

    // Keywords to filter out
    private val blockedKeywords = listOf(
        "porn", "porno", "xxx", "adult", "adultos", "sex", "sexo", "18+", "+18", 
        "erotic", "erotica", "hentai", "nude", "desnudo", "onlyfans", "only fans",
        "brazzers", "playboy", "hustler", "penthouse", "venus", "private", "xlove",
        "uncensored", "nsfw", "milf", "hardcore", "softcore", "red light", "redlight",
        "hot", "sensual", "intimo"
    )

    data class PlaylistContent(
        val movies: List<Movie>,
        val series: List<
                Series>,
        val liveChannels: List<Movie>
    )

    suspend fun fetchPlaylist(url: String): PlaylistContent {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    return@withContext PlaylistContent(emptyList(), emptyList(), emptyList())
                }

                val inputStream = response.body?.byteStream()
                if (inputStream != null) {
                    return@withContext parseM3U(inputStream)
                } else {
                    return@withContext PlaylistContent(emptyList(), emptyList(), emptyList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext PlaylistContent(emptyList(), emptyList(), emptyList())
            }
        }
    }

    private fun parseM3U(inputStream: InputStream): PlaylistContent {
        val movies = mutableListOf<Movie>()
        val liveChannels = mutableListOf<Movie>()
        val seriesEpisodes = mutableListOf<Movie>()
        
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        var currentTitle: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTypeStr: String? = null
        val seriesRegex = Regex("""(.+?)\s+S(\d+)\s*E(\d+).*""", RegexOption.IGNORE_CASE)
        
        val durationRegex = Regex("""#EXTINF:(-?\d+)""")

        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                currentLogo = extractAttribute(trimmed, "tvg-logo")
                currentGroup = extractAttribute(trimmed, "group-title") ?: "Uncategorized"
                currentTypeStr = extractAttribute(trimmed, "type") 
                
               
                val durMatch = durationRegex.find(trimmed)
                val duration = durMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                var type = com.rayeflix.app.model.ContentType.MOVIE 
        
                if (currentTypeStr.equals("series", ignoreCase = true)) type = com.rayeflix.app.model.ContentType.SERIES
                else if ((currentTypeStr.equals("live", ignoreCase = true) || currentTypeStr.equals("stream", ignoreCase = true)) && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) type = com.rayeflix.app.model.ContentType.LIVE
              
                if (currentTypeStr.isNullOrEmpty()) {
                     
                     if (seriesRegex.containsMatchIn(trimmed)) {
                         type = com.rayeflix.app.model.ContentType.SERIES
                     } 
                     
                     else if (duration == -1 && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) {
                         type = com.rayeflix.app.model.ContentType.LIVE
                     }
                     
                     else {
                         val groupLower = currentGroup!!.lowercase()
                         if (groupLower.contains("live") || groupLower.contains("tv") || groupLower.contains("vivo") || groupLower.contains("canales") || groupLower.contains("noticias")) {
                             type = com.rayeflix.app.model.ContentType.LIVE
                         }
                     }
                }
                
                if (type == com.rayeflix.app.model.ContentType.LIVE) currentTypeStr = "live"
                if (type == com.rayeflix.app.model.ContentType.SERIES) currentTypeStr = "series"

                
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentTitle = trimmed.substring(commaIndex + 1).trim()
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                if (currentTitle != null) {
                    
                    var type = com.rayeflix.app.model.ContentType.MOVIE
                    
                    if (currentTypeStr == "series") type = com.rayeflix.app.model.ContentType.SERIES
                    else if (currentTypeStr == "live" && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) type = com.rayeflix.app.model.ContentType.LIVE
                    
                    
                    val urlLower = trimmed.lowercase()
                    if (urlLower.endsWith(".mkv") || urlLower.endsWith(".mp4") || urlLower.endsWith(".avi")) {
                        if (type == com.rayeflix.app.model.ContentType.LIVE) {
                            type = com.rayeflix.app.model.ContentType.MOVIE
                        }
                    }
                    
                    
                    var seriesName: String? = null
                    var season: Int? = null
                    var episode: Int? = null
                    
                    val match = seriesRegex.find(currentTitle!!)
                    if (match != null) {
                        type = com.rayeflix.app.model.ContentType.SERIES
                        seriesName = match.groupValues[1].trim()
                        season = match.groupValues[2].toIntOrNull()
                        episode = match.groupValues[3].toIntOrNull()
                    }

                    if (!isAdultContent(currentTitle!!, currentGroup!!)) {
                         val item = Movie(
                            id = (movies.size + liveChannels.size + seriesEpisodes.size),
                            title = currentTitle!!,
                            imageUrl = if (!currentLogo.isNullOrEmpty()) currentLogo!! else "",
                            description = "Group: $currentGroup",
                            streamUrl = trimmed,
                            categories = listOf(currentGroup!!),
                            type = type,
                            seriesName = seriesName,
                            seasonNumber = season,
                            episodeNumber = episode
                        )
                        
                        when(type) {
                            com.rayeflix.app.model.ContentType.SERIES -> seriesEpisodes.add(item)
                            com.rayeflix.app.model.ContentType.LIVE -> liveChannels.add(item)
                            else -> movies.add(item)
                        }
                    }
                }
                
                currentTitle = null
                currentLogo = null
                currentGroup = null
                currentTypeStr = null
            }
        }
        
      
        val groupedSeries = seriesEpisodes
            .groupBy { it.seriesName ?: it.title }
            .map { (name, episodes) ->
                
                val cover = episodes.firstOrNull { it.imageUrl.isNotEmpty() }?.imageUrl ?: ""
             
                val category = episodes.groupingBy { it.categories.firstOrNull() ?: "Uncategorized" }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: "Uncategorized"
                val seasons = episodes.groupBy { it.seasonNumber ?: 1 }
                
                com.rayeflix.app.model.Series(name, cover, seasons, category)
            }

        
        return PlaylistContent(movies, groupedSeries, liveChannels)
    }

    private fun extractAttribute(line: String, attribute: String): String? {
        val pattern = "$attribute=\"([^\"]*)\""
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groupValues?.get(1)
    }

    private fun isAdultContent(title: String, group: String): Boolean {
        val text = "$title $group".lowercase()
        return blockedKeywords.any { text.contains(it) }
    }
}
