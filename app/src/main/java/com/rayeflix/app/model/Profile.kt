package com.rayeflix.app.model

data class Profile(
    val id: Int,
    val name: String,
    val avatarUrl: String, 
    val playlistUrl: String
)

val profiles = listOf(
    Profile(
        1, 
        "Lista 1", 
        "https://upload.wikimedia.org/wikipedia/commons/0/0b/Netflix-avatar.png", 
        "http://tv.zapping.life:8080/get.php?username=gomezyony1&password=8ccc8e1bf35d&type=m3u_plus"
    ),
    Profile(
        2, 
        "Lista 2", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/84c20033850498.56ba69ac290ea.png", 
        "http://tvpromas.com:2082/get.php?username=Pavelreyes&password=vG99cvskSrzN&type=m3u_plus"
    )
)
