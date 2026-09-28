package com.fahim.bingonumbercaller.server

import java.security.SecureRandom

/** Unguessable tokens for seat reclaim. 32 bytes from a CSPRNG, hex encoded. */
object SecureTokens {
    private const val TOKEN_BYTES = 32
    private val random = SecureRandom()

    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
