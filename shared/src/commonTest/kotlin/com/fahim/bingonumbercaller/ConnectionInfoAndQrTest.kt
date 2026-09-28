package com.fahim.bingonumbercaller

import com.fahim.bingonumbercaller.domain.QrCodeGenerator
import com.fahim.bingonumbercaller.model.ConnectionInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConnectionInfoAndQrTest {

    @Test
    fun testConnectionInfoEncodeDecode() {
        val info = ConnectionInfo(host = "192.168.1.50", port = 8080, sessionToken = "tok12345")
        val payload = info.toPayloadString()
        assertEquals("bingolive://connect?host=192.168.1.50&port=8080&token=tok12345", payload)

        val parsed = ConnectionInfo.parse(payload)
        assertNotNull(parsed)
        assertEquals(info.host, parsed.host)
        assertEquals(info.port, parsed.port)
        assertEquals(info.sessionToken, parsed.sessionToken)

        // Fallback plain address test
        val fallback = ConnectionInfo.parse("192.168.1.50:8080")
        assertNotNull(fallback)
        assertEquals("192.168.1.50", fallback.host)
        assertEquals(8080, fallback.port)

        // With trailing slash and scheme
        val withSlash = ConnectionInfo.parse("bingolive://connect/?host=192.168.1.50&port=8080&token=tok12345")
        assertNotNull(withSlash)
        assertEquals("192.168.1.50", withSlash.host)
        assertEquals(8080, withSlash.port)
        assertEquals("tok12345", withSlash.sessionToken)

        // HTTP URL format
        val httpUrl = ConnectionInfo.parse("http://192.168.1.50:8080/game")
        assertNotNull(httpUrl)
        assertEquals("192.168.1.50", httpUrl.host)
        assertEquals(8080, httpUrl.port)

        // WebSocket URL format
        val wsUrl = ConnectionInfo.parse("ws://192.168.1.50:8080/game")
        assertNotNull(wsUrl)
        assertEquals("192.168.1.50", wsUrl.host)
        assertEquals(8080, wsUrl.port)

        // host:port:token format
        val withToken = ConnectionInfo.parse("192.168.1.50:8080:mytoken")
        assertNotNull(withToken)
        assertEquals("192.168.1.50", withToken.host)
        assertEquals(8080, withToken.port)
        assertEquals("mytoken", withToken.sessionToken)
    }

    @Test
    fun testQrMatrixGeneration() {
        val payload = "bingolive://connect?host=192.168.1.50&port=8080&token=tok12345"
        val matrix = QrCodeGenerator.generateQrMatrix(payload)

        assertTrue(matrix.isNotEmpty())
        assertEquals(matrix.size, matrix[0].size, "QR matrix must be square")
        assertTrue(matrix.size >= 21, "QR matrix must be at least version 1 size (21x21)")

        // Top-left finder pattern must be dark at (0,0) and light at (1,1) etc.
        assertTrue(matrix[0][0])
        assertTrue(matrix[0][6])
        assertTrue(matrix[6][0])
    }
}
