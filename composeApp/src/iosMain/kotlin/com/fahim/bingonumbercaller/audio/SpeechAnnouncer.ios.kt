package com.fahim.bingonumbercaller.audio

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/** iOS AVSpeechSynthesizer. Hosting is Android-only today, so this is only reached if that changes. */
actual class SpeechAnnouncer actual constructor() : NumberAnnouncer {
    private val synthesizer = AVSpeechSynthesizer()

    actual override val isSupported: Boolean = true

    actual override fun announce(text: String) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        synthesizer.speakUtterance(AVSpeechUtterance(string = text))
    }

    actual override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    actual override fun shutdown() {
        stop()
    }
}
