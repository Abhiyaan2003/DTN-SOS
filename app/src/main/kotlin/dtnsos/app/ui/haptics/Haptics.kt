package dtnsos.app.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf

interface Haptics {
    fun play(event: HapticEvent)
    fun hasVibrator(): Boolean
}

class AndroidHaptics(
    private val context: Context,
    private val isEnabled: () -> Boolean = { true }
) : Haptics {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastContactTimestamp = 0L

    override fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() == true
    }

    override fun play(event: HapticEvent) {
        if (!isEnabled()) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val now = System.currentTimeMillis()
        if (event == HapticEvent.Contact) {
            if (now - lastContactTimestamp < 10_000L) {
                return
            }
            lastContactTimestamp = now
        }

        when (event) {
            HapticEvent.Tap -> vibrateOneShot(v, 10)
            HapticEvent.OpenCompose -> vibrateOneShot(v, 25)
            HapticEvent.SendSos -> vibratePattern(v, longArrayOf(0, 60, 50, 60, 50, 160))
            HapticEvent.CarrierPickup -> vibrateOneShot(v, 20)
            HapticEvent.Contact -> vibratePattern(v, longArrayOf(0, 15, 60, 15))
            HapticEvent.Confirm -> vibratePattern(v, longArrayOf(0, 30, 40, 30))
            HapticEvent.Warning -> vibratePattern(v, longArrayOf(0, 50, 40, 50))
            HapticEvent.HapticsEnabled -> vibrateOneShot(v, 40)
        }
    }

    private fun vibrateOneShot(vibrator: Vibrator, durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun vibratePattern(vibrator: Vibrator, timings: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}

class NoOpHaptics : Haptics {
    override fun play(event: HapticEvent) = Unit
    override fun hasVibrator(): Boolean = false
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { NoOpHaptics() }

