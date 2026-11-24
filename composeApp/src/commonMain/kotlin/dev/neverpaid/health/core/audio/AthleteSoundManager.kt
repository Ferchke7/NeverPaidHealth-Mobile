package dev.neverpaid.health.core.audio

import dev.neverpaid.health.core.platform.PlatformBridge

/**
 * Sound cues and tactile haptic vibration manager for athletic training.
 * Provides instant feedback during active workouts, rest timers, and focus sessions.
 */
object AthleteSoundManager {

    /**
     * Called on each second of the final countdown (3, 2, 1, 0).
     */
    fun playRestCountdown(secondsRemaining: Int) {
        when (secondsRemaining) {
            3, 2, 1 -> {
                // Short alert tick
                PlatformBridge.vibrate()
            }
            0 -> {
                // Rest period finished: sound cue + vibration alert
                PlatformBridge.playAlertSound()
                PlatformBridge.vibrate()
            }
        }
    }

    /**
     * Called when a working set is checked off and completed.
     */
    fun playSetComplete() {
        PlatformBridge.vibrate()
    }

    /**
     * Called when an athlete breaks an all-time 1RM or Volume Personal Record.
     */
    fun playPersonalRecordFanfare() {
        PlatformBridge.playAlertSound()
        PlatformBridge.vibrate()
    }

    /**
     * Called when a Pomodoro focus or fasting interval completes.
     */
    fun playIntervalGong() {
        PlatformBridge.playFocusAlarm(preset = "gong", durationSeconds = 8)
        PlatformBridge.vibrate()
    }

    /**
     * Plays a distinct loud sustained Focus Timer alarm (default 8 seconds),
     * or a brief ~3s preview when choosing presets.
     */
    fun playFocusAlarm(preset: String = "radar", preview: Boolean = false) {
        PlatformBridge.playFocusAlarm(
            preset = preset,
            durationSeconds = if (preview) 3 else 8
        )
        PlatformBridge.vibrate()
    }

    fun stopFocusAlarm() {
        PlatformBridge.stopFocusAlarm()
    }
}
