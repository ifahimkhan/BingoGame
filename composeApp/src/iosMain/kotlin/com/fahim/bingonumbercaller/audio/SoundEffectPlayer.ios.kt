package com.fahim.bingonumbercaller.audio

import platform.AudioToolbox.AudioServicesPlaySystemSound

actual class SoundEffectPlayer actual constructor() {
    actual fun playDrawSound() {
        AudioServicesPlaySystemSound(1057u)
    }

    actual fun playNewGameSound() {
        AudioServicesPlaySystemSound(1025u)
    }
}
