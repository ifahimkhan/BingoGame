package com.fahim.bingonumbercaller.protocol

/** Limits shared by the game server and client so both enforce the same wire contract. */
object ProtocolLimits {
    /**
     * Largest WebSocket frame either side accepts. Real messages (a ticket, 90 called
     * numbers) are well under 4 KB; anything bigger is malformed or hostile.
     */
    const val MAX_FRAME_BYTES: Long = 64 * 1024L
}
