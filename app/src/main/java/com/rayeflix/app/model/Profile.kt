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
        "Lista 1", 
        "https://upload.wikimedia.org/wikipedia/commons/0/0b/Netflix-avatar.png", 
        "http://tv.zapping.life:8080/get.php?username=gomezyony1&password=8ccc8e1bf35d&type=m3u_plus"
    ),
    Profile(
        2, 
        "Lista 2", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/84c20033850498.56ba69ac290ea.png", 
        "http://stklatino.dynns.com:2082/get.php?username=56923830688w&password=170525act&type=m3u_plus"
    ),
    Profile(
        3, 
        "Lista 3", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/bb3a8833850498.56ba69ac33f26.png", 
        "http://matrivagoweb.xyz:2095/get.php?username=hsandoval758&password=123456&type=m3u_plus"
    ),
    Profile(
        4, 
        "Lista 4", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/64623a33850498.56ba69ac2a6f7.png", 
        "http://mgoplus.org:2086/get.php?username=fermin.ruedavalle2508&password=12345678.&type=m3u_plus"
    ),
    Profile(
        5, 
        "Lista 5", 
        "https://mir-s3-cdn-cf.behance.net/project_modules/disp/bf6e4a33850498.56ba69ac3064f.png", 
        "http://alfatv.lat:2082/get.php?username=Nvouser11&password=6aNLSuAvfJAR&type=m3u_plus"
    )
)
