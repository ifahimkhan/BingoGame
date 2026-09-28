package com.fahim.bingonumbercaller.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

actual class SoundEffectPlayer actual constructor() {

    private val toneGenerator: ToneGenerator? by lazy {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun playDrawSound() {
        thread(isDaemon = true, name = "BingoDrawAudio") {
            val played = playSynthesizedChime(
                frequencies = doubleArrayOf(784.0, 1175.0),
                durationSec = 0.22,
                decayRate = 14.0
            )
            if (!played) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                } catch (_: Throwable) {
                }
            }
        }
    }

    actual fun playNewGameSound() {
        thread(isDaemon = true, name = "BingoNewGameAudio") {
            val played = playArpeggio(
                frequencies = listOf(523.25, 659.25, 783.99, 1046.50),
                stepDurationSec = 0.08
            )
            if (!played) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun playSynthesizedChime(
        frequencies: DoubleArray,
        durationSec: Double,
        decayRate: Double
    ): Boolean {
        return try {
            val sampleRate = 44100
            val totalSamples = (sampleRate * durationSec).toInt()
            val buffer = ShortArray(totalSamples)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-t * decayRate)
                var sample = 0.0
                for (f in frequencies) {
                    sample += sin(2.0 * PI * f * t)
                }
                sample = (sample / frequencies.size) * envelope
                buffer[i] = (sample * Short.MAX_VALUE * 0.9)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }

            playPcmBuffer(buffer, sampleRate)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun playArpeggio(
        frequencies: List<Double>,
        stepDurationSec: Double
    ): Boolean {
        return try {
            val sampleRate = 44100
            val stepSamples = (sampleRate * stepDurationSec).toInt()
            val totalSamples = stepSamples * frequencies.size
            val buffer = ShortArray(totalSamples)

            for (noteIndex in frequencies.indices) {
                val freq = frequencies[noteIndex]
                val offset = noteIndex * stepSamples
                for (i in 0 until stepSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-t * 8.0)
                    val sample = sin(2.0 * PI * freq * t) * envelope
                    buffer[offset + i] = (sample * Short.MAX_VALUE * 0.85)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
            }

            playPcmBuffer(buffer, sampleRate)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((buffer.size * 1000L / sampleRate) + 50)
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Throwable) {
            }
        }
    }
}
