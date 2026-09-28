package com.fahim.bingonumbercaller.audio

/** Speaks called numbers aloud on the host device. */
interface NumberAnnouncer {
    /** False when this platform/device has no usable speech engine; the voice toggle is hidden. */
    val isSupported: Boolean

    /** Speaks [text], interrupting anything still being spoken. */
    fun announce(text: String)
    fun stop()

    /** Releases the speech engine. The announcer is not used afterwards. */
    fun shutdown()
}
