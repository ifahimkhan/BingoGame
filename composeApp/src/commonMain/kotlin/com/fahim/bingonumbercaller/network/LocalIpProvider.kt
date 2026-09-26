package com.fahim.bingonumbercaller.network

expect object LocalIpProvider {
    fun getLocalIpAddress(): String?
}
