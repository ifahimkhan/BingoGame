package com.fahim.bingonumbercaller.audio

import android.speech.tts.TextToSpeech
import android.util.Log
import com.fahim.bingonumbercaller.platform.AndroidAppContext
import java.util.Locale

/**
 * Android TextToSpeech. The engine is created on first use (binding it is slow) and whatever is
 * requested before it finishes initialising is spoken once it is ready.
 */
actual class SpeechAnnouncer actual constructor() : NumberAnnouncer {
    private val context = AndroidAppContext.context

    actual override val isSupported: Boolean = context != null

    private var engine: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    actual override fun announce(text: String) {
        val tts = engine ?: createEngine() ?: return
        if (isReady) {
            speak(tts, text)
        } else {
            // Only the latest call matters if several arrive during start-up
            pendingText = text
        }
    }

    actual override fun stop() {
        pendingText = null
        engine?.stop()
    }

    actual override fun shutdown() {
        pendingText = null
        isReady = false
        engine?.shutdown()
        engine = null
    }

    private fun createEngine(): TextToSpeech? {
        val appContext = context ?: return null
        return TextToSpeech(appContext) { status -> onEngineInitialised(status) }.also { engine = it }
    }

    private fun onEngineInitialised(status: Int) {
        val tts = engine ?: return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Text-to-speech unavailable (status $status); numbers will not be spoken")
            return
        }
        val deviceLanguage = tts.setLanguage(Locale.getDefault())
        if (deviceLanguage == TextToSpeech.LANG_MISSING_DATA || deviceLanguage == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.UK)
        }
        isReady = true
        pendingText?.let { speak(tts, it) }
        pendingText = null
    }

    private fun speak(tts: TextToSpeech, text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val TAG = "SpeechAnnouncer"
        const val UTTERANCE_ID = "bingo-call"
    }
}
