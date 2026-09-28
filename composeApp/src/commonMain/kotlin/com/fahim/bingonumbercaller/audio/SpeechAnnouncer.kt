package com.fahim.bingonumbercaller.audio

/** Platform text-to-speech. Android: TextToSpeech. iOS: AVSpeechSynthesizer. */
expect class SpeechAnnouncer() : NumberAnnouncer {
    override val isSupported: Boolean
    override fun announce(text: String)
    override fun stop()
    override fun shutdown()
}
