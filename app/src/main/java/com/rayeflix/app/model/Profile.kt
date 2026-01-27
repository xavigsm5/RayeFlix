package com.rayeflix.app.model

data class Profile(
    val id: Int,
    val name: String,
    val avatarUrl: String, // Mocked for now
    val playlistUrl: String
)

val profiles = listOf(
    Profile(
        1, 
        "Javier", 
        "https://upload.wikimedia.org/wikipedia/commons/0/0b/Netflix-avatar.png", 
        "http://tvpromas.com:2082/get.php?username=Pavelreyes&password=vG99cvskSrzN&type=m3u_plus"
    ),
    Profile(
        2, 
        "Kids", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/84c20033850498.56ba69ac290ea.png", 
        "http://kstv.us:8080/get.php?username=jTgf2f6Sfj&password=4314648140&type=m3u_plus"
    ),
    Profile(
        3, 
        "Guest", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/bb3a8833850498.56ba69ac33f26.png", 
        "http://tvpromas.com:2082/get.php?username=Pavelreyes&password=vG99cvskSrzN&type=m3u_plus" // Reusing for guest
    ),
    Profile(
        4,
        "Add Profile",
        "https://assets.nflxext.com/ffe/siteui/vlv3/f841d4c7-10e1-40af-bcae-07a3f8dc141a/f6d7434e-d6de-4185-a6d4-c77a2d08737b/US-en-20220502-popsignuptwoweeks-perspective_alpha_website_small.jpg",
        "" // Placeholder
    )
)
