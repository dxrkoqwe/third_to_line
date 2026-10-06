package com.example.match3

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {
    private var toneGenerator: ToneGenerator? = null
    private var initialized = false
    var isEnabled: Boolean = true

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (_: Throwable) {
            toneGenerator = null
        }
    }

    fun playMatch() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
        } catch (_: Throwable) {}
    }

    fun playSwap() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Throwable) {}
    }

    fun playExplosion() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 110)
        } catch (_: Throwable) {}
    }

    fun playWin() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
        } catch (_: Throwable) {}
    }

    fun playLose() {
        if (!isEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 200)
        } catch (_: Throwable) {}
    }

    fun release() {
        try { toneGenerator?.release() } catch (_: Throwable) {}
        toneGenerator = null
        initialized = false
    }
}