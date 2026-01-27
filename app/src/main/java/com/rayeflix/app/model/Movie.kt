package com.rayeflix.app.model

data class Movie(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val description: String = "",
    val streamUrl: String = "",
    val categories: List<String> = emptyList()
)

val mockMovies = listOf(
    Movie(1, "Wednesday", "https://image.tmdb.org/t/p/w500/9PFonBhy4cQy7Jz20NpMygczOk.jpg", "Smart, sarcastic and a little dead inside, Wednesday Addams investigates a murder spree.", "", listOf("Show", "Fantasy", "Teen")),
    Movie(2, "Black Mirror", "https://image.tmdb.org/t/p/w500/7dFZJ2ZJJdcmkp05B9NWlqTJ5tq.jpg"),
    Movie(3, "Stranger Things", "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg"),
    Movie(4, "The Witcher", "https://image.tmdb.org/t/p/w500/cRLzZsq1kM9sV6r3Zdf5L9.jpg"),
    Movie(5, "Breaking Bad", "https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pWle8xiJD1Hes.jpg"),
    Movie(6, "Squid Game", "https://image.tmdb.org/t/p/w500/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg")
)
