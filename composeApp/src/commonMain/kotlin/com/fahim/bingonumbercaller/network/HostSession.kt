package com.fahim.bingonumbercaller.network

import com.fahim.bingonumbercaller.model.ConnectionInfo

/**
 * A running embedded game server as seen by the host app.
 *
 * @property publicInfo what players see (QR code / manual code): Wi-Fi IP, port, session token.
 * @property hostSecret private credential for the host's own socket. Never shown or encoded in the QR.
 */
class HostSession(
    val publicInfo: ConnectionInfo,
    val hostSecret: String
) {
    /** The host talks to its own server over loopback, so Wi-Fi changes can't cut it off. */
    val hostSocketUrl: String
        get() = "ws://$LOOPBACK_HOST:${publicInfo.port}/game"

    override fun toString(): String = "HostSession(publicInfo=$publicInfo, hostSecret=<redacted>)"

    private companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
    }
}
