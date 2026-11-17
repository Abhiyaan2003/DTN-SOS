package dtnsos.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Locale

val DtnFontFamily: FontFamily get() = JakartaFamily

/**
 * Typography container holding all 13 styles specified in §3.2.3.
 */
data class DtnTypography(
    val display: TextStyle,
    val displayMd: TextStyle,
    val headlineLg: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val button: TextStyle,
    val label: TextStyle,
    val captionTag: TextStyle,
    val caption: TextStyle,
    val footerValue: TextStyle,
    val ids: TextStyle,
    // Backwards compatibility aliases
    val bodyMedium: TextStyle = bodyStrong,
    val labelMedium: TextStyle = label.copy(fontWeight = FontWeight.W500)
)

fun isIndicLocale(locale: Locale): Boolean = when (locale.language.lowercase()) {
    "bn", "hi", "mr", "ne" -> true
    else -> false
}

/**
 * Creates [DtnTypography] dynamically configured for the specified [locale].
 * When an Indic locale is detected ("bn", "hi", "mr", "ne"), script overrides from §3.2.4
 * are applied:
 *  - letterSpacing = 0.em
 *  - lineHeight = 1.25 * Latin value
 *  - size: label, captionTag, caption + 1.sp
 *  - keeps Latin digits (0-9)
 */
fun createDtnTypography(
    locale: Locale = Locale.ENGLISH,
    customBodyFamily: FontFamily? = null
): DtnTypography {
    val isIndic = isIndicLocale(locale)
    val bodyFamily = customBodyFamily ?: bodyFamilyFor(locale)
    val lhMultiplier = if (isIndic) 1.25f else 1.0f
    val sizeBonus = if (isIndic) 1 else 0

    fun makeStyle(
        weight: Int,
        baseSize: Int,
        baseLh: Float,
        baseTracking: Float,
        isHeading: Boolean,
        applySizeBonus: Boolean = false,
        fontFamilyOverride: FontFamily? = null
    ): TextStyle {
        val size = if (applySizeBonus) (baseSize + sizeBonus).sp else baseSize.sp
        val lh = (baseLh * lhMultiplier).sp
        val letterSpacing = if (isIndic) 0.em else baseTracking.em
        val family = fontFamilyOverride ?: bodyFamily

        return TextStyle(
            fontFamily = family,
            fontWeight = FontWeight(weight),
            fontSize = size,
            lineHeight = lh,
            letterSpacing = letterSpacing,
            fontFeatureSettings = "tnum",
            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            ),
            lineBreak = if (isHeading) LineBreak.Heading else LineBreak.Paragraph,
            hyphens = Hyphens.None
        )
    }

    return DtnTypography(
        display = makeStyle(
            weight = 600,
            baseSize = 64,
            baseLh = 70f,
            baseTracking = -0.03f,
            isHeading = true
        ),
        displayMd = makeStyle(
            weight = 600,
            baseSize = 52,
            baseLh = 58f,
            baseTracking = -0.03f,
            isHeading = true
        ),
        headlineLg = makeStyle(
            weight = 600,
            baseSize = 32,
            baseLh = 37f,
            baseTracking = -0.02f,
            isHeading = true
        ),
        headline = makeStyle(
            weight = 600,
            baseSize = 28,
            baseLh = 34f,
            baseTracking = -0.02f,
            isHeading = true
        ),
        title = makeStyle(
            weight = 500,
            baseSize = 20,
            baseLh = 26f,
            baseTracking = -0.01f,
            isHeading = true
        ),
        body = makeStyle(
            weight = 400,
            baseSize = 16,
            baseLh = 24f,
            baseTracking = 0f,
            isHeading = false
        ),
        bodyStrong = makeStyle(
            weight = 500,
            baseSize = 16,
            baseLh = 24f,
            baseTracking = 0f,
            isHeading = false
        ),
        button = makeStyle(
            weight = 600,
            baseSize = 16,
            baseLh = 24f,
            baseTracking = 0f,
            isHeading = false
        ),
        label = makeStyle(
            weight = 400,
            baseSize = 14,
            baseLh = 20f,
            baseTracking = 0f,
            isHeading = false,
            applySizeBonus = true
        ),
        captionTag = makeStyle(
            weight = 400,
            baseSize = 12,
            baseLh = 16f,
            baseTracking = 0.01f,
            isHeading = false,
            applySizeBonus = true
        ),
        caption = makeStyle(
            weight = 400,
            baseSize = 12,
            baseLh = 16f,
            baseTracking = 0.01f,
            isHeading = false,
            applySizeBonus = true
        ),
        footerValue = makeStyle(
            weight = 600,
            baseSize = 16,
            baseLh = 20f,
            baseTracking = 0f,
            isHeading = false
        ),
        ids = makeStyle(
            weight = 400,
            baseSize = 14,
            baseLh = 20f,
            baseTracking = 0f,
            isHeading = false,
            fontFamilyOverride = FontFamily.Monospace
        )
    )
}

fun DtnTypography.toMaterial3Typography(): androidx.compose.material3.Typography {
    return androidx.compose.material3.Typography(
        displayLarge = display,
        displayMedium = displayMd,
        displaySmall = headlineLg,
        headlineLarge = headlineLg,
        headlineMedium = headline,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = title,
        titleSmall = bodyStrong,
        bodyLarge = bodyStrong,
        bodyMedium = body,
        bodySmall = label,
        labelLarge = button,
        labelMedium = label,
        labelSmall = caption
    )
}

val LocalDtnTypography = staticCompositionLocalOf {
    createDtnTypography(Locale.ENGLISH)
}

/**
 * Default static typography object (Latin/en) for direct usage in Compose components.
 */
object DtnType {
    private val defaultTypography = createDtnTypography(Locale.ENGLISH)

    val display: TextStyle get() = defaultTypography.display
    val displayMd: TextStyle get() = defaultTypography.displayMd
    val headlineLg: TextStyle get() = defaultTypography.headlineLg
    val headline: TextStyle get() = defaultTypography.headline
    val title: TextStyle get() = defaultTypography.title
    val body: TextStyle get() = defaultTypography.body
    val bodyStrong: TextStyle get() = defaultTypography.bodyStrong
    val button: TextStyle get() = defaultTypography.button
    val label: TextStyle get() = defaultTypography.label
    val captionTag: TextStyle get() = defaultTypography.captionTag
    val caption: TextStyle get() = defaultTypography.caption
    val footerValue: TextStyle get() = defaultTypography.footerValue
    val ids: TextStyle get() = defaultTypography.ids

    // Backwards compatibility aliases
    val bodyMedium: TextStyle get() = defaultTypography.bodyMedium
    val labelMedium: TextStyle get() = defaultTypography.labelMedium

    /**
     * Retrieves the current locale-aware [DtnTypography] from the Compose tree.
     */
    val current: DtnTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalDtnTypography.current
}
