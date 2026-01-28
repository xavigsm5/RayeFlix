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

    private val client = OkHttpClient()

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
        
        // Regex for Series Title
        val seriesRegex = Regex("""(.+?)\s+S(\d+)\s*E(\d+).*""", RegexOption.IGNORE_CASE)
        // Regex for Duration: #EXTINF:-1 or #EXTINF:3600
        val durationRegex = Regex("""#EXTINF:(-?\d+)""")

        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                currentLogo = extractAttribute(trimmed, "tvg-logo")
                currentGroup = extractAttribute(trimmed, "group-title") ?: "Uncategorized"
                currentTypeStr = extractAttribute(trimmed, "type") // m3u_plus tags: stream, movie, series
                
                // Duration Check
                val durMatch = durationRegex.find(trimmed)
                val duration = durMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                // Guess type from flags if explicit "type" missing (or sometimes even if present if unreliable)
                // However, "type=stream" usually means Live. "type=movie" means Movie. "type=series" means Series.
                
                // Refined Content Type Logic (pre-calculation)
                var type = com.rayeflix.app.model.ContentType.MOVIE // Default
                
                // 1. Explicit Type from 'type' attribute (common in m3u_plus)
                // "stream" is often used for Live TV in Xtream Codes lines
                if (currentTypeStr.equals("series", ignoreCase = true)) type = com.rayeflix.app.model.ContentType.SERIES
                else if ((currentTypeStr.equals("live", ignoreCase = true) || currentTypeStr.equals("stream", ignoreCase = true)) && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) type = com.rayeflix.app.model.ContentType.LIVE
                
                // 2. If no explicit type, check regex/duration/group
                if (currentTypeStr.isNullOrEmpty()) {
                     // Check if it looks like a Series (S01 E01)
                     if (seriesRegex.containsMatchIn(trimmed)) {
                         type = com.rayeflix.app.model.ContentType.SERIES
                     } 
                     // Check if it looks like Live TV (Duration -1 is standard for live streams)
                     // But avoid flagging items in "Movies" groups as Live just because duration is missing
                     else if (duration == -1 && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) {
                         type = com.rayeflix.app.model.ContentType.LIVE
                     }
                     // Check Keywords in Group
                     else {
                         val groupLower = currentGroup!!.lowercase()
                         if (groupLower.contains("live") || groupLower.contains("tv") || groupLower.contains("vivo") || groupLower.contains("canales") || groupLower.contains("noticias")) {
                             type = com.rayeflix.app.model.ContentType.LIVE
                         }
                     }
                }
                
                // Store determined Type string temporarily in currentTypeStr for next block to use 
                // (though we recalculate below, let's just rely on logic in the next block using variables)
                // Better: Store the detected type in a var outside if needed, OR just redo logic inside title block. 
                // To avoid complexity, let's keep the attributes state and do logic in URL block.
                
                // Hack: Pass the detected type via a thread-local or just infer again below? 
                // Let's infer below, but we need the duration/type info passed.
                // Since `currentTypeStr` is member var, update it to our detected type if it was null? 
                // No, better to replicate logic or store in a robust var. 
                // Let's store "isLive" flag in a temp var? No, Reader loop is linear.
                // We will just re-run the logic below.
                
                // Re-assign type tag for clarity if we detected something specific
                if (type == com.rayeflix.app.model.ContentType.LIVE) currentTypeStr = "live"
                if (type == com.rayeflix.app.model.ContentType.SERIES) currentTypeStr = "series"

                // Title extraction
                val commaIndex = trimmed.lastIndexOf(',')
                if (commaIndex != -1) {
                    currentTitle = trimmed.substring(commaIndex + 1).trim()
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                if (currentTitle != null) {
                    // Logic to finalize Type
                    var type = com.rayeflix.app.model.ContentType.MOVIE
                    
                    if (currentTypeStr == "series") type = com.rayeflix.app.model.ContentType.SERIES
                    else if (currentTypeStr == "live" && currentGroup?.lowercase()?.contains("pelicula") != true && currentGroup?.lowercase()?.contains("movie") != true) type = com.rayeflix.app.model.ContentType.LIVE
                    
                    // Improved VOD Detection based on URL extension
                    val urlLower = trimmed.lowercase()
                    if (urlLower.endsWith(".mkv") || urlLower.endsWith(".mp4") || urlLower.endsWith(".avi")) {
                        // If it was detected as LIVE due to duration -1, revert to MOVIE (or keep SERIES if it looks like one)
                        if (type == com.rayeflix.app.model.ContentType.LIVE) {
                            type = com.rayeflix.app.model.ContentType.MOVIE
                        }
                    }
                    
                    // Regex check for Series specifics (needed for Name/Season/Ep parsing anyway)
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
        
        // Group Series into Series Objects
        val groupedSeries = seriesEpisodes
            .groupBy { it.seriesName ?: it.title } // Fallback to title if regex failed but marked as series
            .map { (name, episodes) ->
                // Try to find a cover image from the first episode or most frequent one
                val cover = episodes.firstOrNull { it.imageUrl.isNotEmpty() }?.imageUrl ?: ""
                
                // Find most common category/group
                val category = episodes.groupingBy { it.categories.firstOrNull() ?: "Uncategorized" }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: "Uncategorized"

                // Group by Season
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
