package io.github.jreyn419.circuittrainer.ui.play

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Beeps and vibration cues for the workout player.
 * Uses [ToneGenerator] so no sound assets are needed.
 */
class Feedback(context: Context) {

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    } catch (_: RuntimeException) {
        null
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** Short tick for the 3-2-1 countdown at the end of a phase. */
    fun countdownTick(sound: Boolean) {
        if (sound) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    /** Longer cue when a new phase starts. */
    fun phaseChange(sound: Boolean, vibrate: Boolean) {
        if (sound) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        if (vibrate) vibrateOneShot(250)
    }

    /** Celebration cue when the whole workout is done. */
    fun workoutFinished(sound: Boolean, vibrate: Boolean) {
        if (sound) toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
        if (vibrate) vibrateOneShot(500)
    }

    private fun vibrateOneShot(millis: Long) {
        vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        toneGenerator?.release()
    }
}
