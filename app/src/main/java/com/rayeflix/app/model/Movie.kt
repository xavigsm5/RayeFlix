package com.rayeflix.app.model

enum class ContentType {
    MOVIE, SERIES, LIVE
}

data class Movie(
    val id: Int,
    val title: String, // Episode title if series
    val imageUrl: String,
    val description: String = "",
    val streamUrl: String = "",
    val categories: List<String> = emptyList(),
    val type: ContentType = ContentType.MOVIE,
    val seriesName: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

data class Series(
    val name: String,
    val coverUrl: String,
    val episodes: Map<Int, List<Movie>>, // Map Season Number -> List of Episodes
    val category: String = "Uncategorized"
)

// Mock Data removed or updated if needed, but keeping it minimal for now
val mockMovies = emptyList<Movie>()
