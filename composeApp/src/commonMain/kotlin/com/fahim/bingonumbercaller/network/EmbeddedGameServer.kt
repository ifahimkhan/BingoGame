package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.model.ConnectionInfo

expect class EmbeddedGameServer() {
    val isSupported: Boolean
    fun start(port: Int = 8080): ConnectionInfo
    fun stop()
}
