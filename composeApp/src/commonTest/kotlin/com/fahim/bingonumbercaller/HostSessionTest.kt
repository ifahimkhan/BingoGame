package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.model.ConnectionInfo
import com.fahim.bingonumbercaller.network.HostSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HostSessionTest {

    private val session = HostSession(
        publicInfo = ConnectionInfo(host = "192.168.1.20", port = 8080, sessionToken = "123456"),
        hostSecret = "super-secret-host-key"
    )

    @Test
    fun `host connects over loopback not the Wi-Fi address`() {
        assertEquals("ws://127.0.0.1:8080/game", session.hostSocketUrl)
    }

    @Test
    fun `QR payload never contains the host secret`() {
        assertFalse(session.publicInfo.toPayloadString().contains(session.hostSecret))
    }

    @Test
    fun `toString redacts the host secret`() {
        assertFalse(session.toString().contains(session.hostSecret))
    }
}
