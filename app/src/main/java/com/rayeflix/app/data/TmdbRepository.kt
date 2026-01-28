package com.rayeflix.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log

data class TmdbMetadata(
    val title: String,
    val description: String,
    val backdropUrl: String?, 
    val posterUrl: String?,   
    val rating: Double,
    val releaseDate: String?,
    val genres: List<String> = emptyList()
)

object TmdbRepository {
    private const val API_KEY = "f43d9cdd8aed779e256c91adef6b77eb"
    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original"
    
    private val client = getUnsafeOkHttpClient()

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Cache to avoid hitting API repeatedly for same title
    private val cache = mutableMapOf<String, TmdbMetadata?>()

    suspend fun searchMovie(rawTitle: String): TmdbMetadata? {
        val cleanedTitle = cleanTitle(rawTitle)
        if (cache.containsKey(cleanedTitle)) return cache[cleanedTitle]

        // If no API Key, return null immediately (or mock data if debugging)


        return withContext(Dispatchers.IO) {
            try {
                Log.d("TmdbRepo", "Searching Movie: '$cleanedTitle'")
                val searchUrl = "$BASE_URL/search/movie?api_key=$API_KEY&query=${android.net.Uri.encode(cleanedTitle)}&language=es-ES"
                val searchRequest = Request.Builder().url(searchUrl).build()
                val searchResponse = client.newCall(searchRequest).execute()
                val responseStr = searchResponse.body?.string() ?: "{}"
                
                if (!searchResponse.isSuccessful) Log.e("TmdbRepo", "Error Code: ${searchResponse.code}")
                
                val searchJson = JSONObject(responseStr)
                val results = searchJson.optJSONArray("results")
                
                Log.d("TmdbRepo", "Movie Results found: ${results?.length() ?: 0}")

                if (results != null && results.length() > 0) {
                    val firstResult = results.getJSONObject(0)
                    val id = firstResult.getInt("id")
                    
                    // 2. Get Details (for better info usually, but search result is often enough)
                    // We'll use search result properties for speed
                    val overview = firstResult.optString("overview")
                    val title = firstResult.optString("title")
                    val backdropPath = firstResult.optString("backdrop_path")
                    val posterPath = firstResult.optString("poster_path")
                    val voteAverage = firstResult.optDouble("vote_average", 0.0)
                    val releaseDate = firstResult.optString("release_date")
                    
                    val metadata = TmdbMetadata(
                        title = title,
                        description = overview,
                        backdropUrl = if (backdropPath.isNotEmpty() && backdropPath != "null") "$IMAGE_BASE_URL$backdropPath" else null,
                        posterUrl = if (posterPath.isNotEmpty() && posterPath != "null") "$IMAGE_BASE_URL$posterPath" else null,
                        rating = voteAverage,
                        releaseDate = releaseDate
                    )
                    
                    cache[cleanedTitle] = metadata
                    metadata
                } else {
                    cache[cleanedTitle] = null
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    suspend fun searchSeries(rawTitle: String): TmdbMetadata? {
        val cleanedTitle = cleanTitle(rawTitle)
        if (cache.containsKey(cleanedTitle)) return cache[cleanedTitle]



        return withContext(Dispatchers.IO) {
            try {
                Log.d("TmdbRepo", "Searching Series: '$cleanedTitle'")
                val searchUrl = "$BASE_URL/search/tv?api_key=$API_KEY&query=${android.net.Uri.encode(cleanedTitle)}&language=es-ES"
                val searchRequest = Request.Builder().url(searchUrl).build()
                val searchResponse = client.newCall(searchRequest).execute()
                val responseStr = searchResponse.body?.string() ?: "{}"
                
                 if (!searchResponse.isSuccessful) Log.e("TmdbRepo", "Error Code: ${searchResponse.code}")

                val searchJson = JSONObject(responseStr)
                val results = searchJson.optJSONArray("results")
                
                Log.d("TmdbRepo", "Series Results found: ${results?.length() ?: 0}")

                if (results != null && results.length() > 0) {
                    val firstResult = results.getJSONObject(0)
                    
                    val overview = firstResult.optString("overview")
                    val name = firstResult.optString("name")
                    val backdropPath = firstResult.optString("backdrop_path")
                    val posterPath = firstResult.optString("poster_path")
                    val voteAverage = firstResult.optDouble("vote_average", 0.0)
                    val firstAirDate = firstResult.optString("first_air_date")
                    
                    val metadata = TmdbMetadata(
                        title = name,
                        description = overview,
                        backdropUrl = if (backdropPath.isNotEmpty() && backdropPath != "null") "$IMAGE_BASE_URL$backdropPath" else null,
                        posterUrl = if (posterPath.isNotEmpty() && posterPath != "null") "$IMAGE_BASE_URL$posterPath" else null,
                        rating = voteAverage,
                        releaseDate = firstAirDate
                    )
                    
                    cache[cleanedTitle] = metadata
                    metadata
                } else {
                    cache[cleanedTitle] = null
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun cleanTitle(original: String): String {
        // Regex to remove common scene tags
        val noiseRegex = Regex("""(\(\d{4}\))|(\[.*?\])|(S\d+.*)|(E\d+.*)|(HD)|(FHD)|(4K)|(LATINO)|(ESPAÑOL)|(SUB)|(MKV)|(AVI)|(MP4)""", RegexOption.IGNORE_CASE)
        val cleaned = noiseRegex.replace(original, "")
        // Remove special chars and extra spaces
        return cleaned.replace(Regex("""[^a-zA-Z0-9\sñÑáéíóúÁÉÍÓÚ]"""), " ").trim().replace(Regex("""\s+"""), " ")
    }
}
