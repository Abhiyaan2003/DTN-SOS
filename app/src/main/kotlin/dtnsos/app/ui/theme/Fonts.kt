package dtnsos.app.ui.theme

import android.annotation.SuppressLint
import androidx.annotation.FontRes
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dtnsos.app.R
import java.util.Locale

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, w: Int): Font = Font(
    resId = res,
    weight = FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w))
)

/**
 * Creates a variable font family for weights 400, 500, 600.
 * If font loading or resource resolution fails, falls back gracefully to [fallback].
 */
@OptIn(ExperimentalTextApi::class)
@SuppressLint("ResourceType")
fun createVariableFontFamily(
    @FontRes resId: Int,
    fallback: FontFamily = FontFamily.SansSerif
): FontFamily {
    return try {
        if (resId <= 0) return fallback
        FontFamily(
            variable(resId, 400),
            variable(resId, 500),
            variable(resId, 600)
        )
    } catch (_: Throwable) {
        fallback
    }
}

/**
 * Toggle for testing / simulation: when true, all families fall back to [FontFamily.SansSerif].
 */
@Volatile
var forceSansSerifFallback: Boolean = false

private val rawJakartaFamily: FontFamily by lazy {
    createVariableFontFamily(R.font.plus_jakarta_sans)
}

private val rawBengaliFamily: FontFamily by lazy {
    createVariableFontFamily(R.font.noto_sans_bengali)
}

private val rawDevanagariFamily: FontFamily by lazy {
    createVariableFontFamily(R.font.noto_sans_devanagari)
}

val JakartaFamily: FontFamily
    get() = if (forceSansSerifFallback) FontFamily.SansSerif else rawJakartaFamily

val BengaliFamily: FontFamily
    get() = if (forceSansSerifFallback) FontFamily.SansSerif else rawBengaliFamily

val DevanagariFamily: FontFamily
    get() = if (forceSansSerifFallback) FontFamily.SansSerif else rawDevanagariFamily

val WordmarkFamily: FontFamily
    get() = JakartaFamily

/**
 * Selects the appropriate [FontFamily] for the given [locale].
 * Bengali ("bn") -> [BengaliFamily]
 * Hindi ("hi"), Marathi ("mr"), Nepali ("ne") -> [DevanagariFamily]
 * Else -> [JakartaFamily]
 */
fun bodyFamilyFor(locale: Locale): FontFamily = when (locale.language.lowercase()) {
    "bn" -> BengaliFamily
    "hi", "mr", "ne" -> DevanagariFamily
    else -> JakartaFamily
}
