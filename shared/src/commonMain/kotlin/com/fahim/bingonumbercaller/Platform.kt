package com.fahim.bingonumbercaller

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform