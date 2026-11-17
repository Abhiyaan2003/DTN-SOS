package dtnsos.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object DtnMotion {
    val EaseOut = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    const val DurationSegmentSlide = 200
    const val DurationPressScale   = 120
    const val DurationNavSlide     = 220
    const val DurationSheetEnter   = 300
    const val DurationHeroFade     = 250
    const val DurationNumberTick   = 400
    const val DurationPulseLoop    = 1800
    const val DurationReducedFade  = 100

    fun isReducedMotion(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun rememberIsReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { DtnMotion.isReducedMotion(context) }
}

