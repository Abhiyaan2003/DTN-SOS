package dtnsos.app.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.theme.BengaliFamily
import dtnsos.app.ui.theme.DevanagariFamily
import dtnsos.app.ui.theme.DtnType
import dtnsos.app.ui.theme.DtnTypography
import dtnsos.app.ui.theme.JakartaFamily
import dtnsos.app.ui.theme.WordmarkFamily
import dtnsos.app.ui.theme.bodyFamilyFor
import dtnsos.app.ui.theme.createDtnTypography
import dtnsos.app.ui.theme.createVariableFontFamily
import dtnsos.app.ui.theme.forceSansSerifFallback
import dtnsos.app.ui.theme.isIndicLocale
import dtnsos.app.ui.theme.toMaterial3Typography
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class TypographyTest {

    @AfterEach
    fun tearDown() {
        forceSansSerifFallback = false
    }

    // ─── 1. Locale-based Body Font Family Routing ─────────────────────────────

    @Test
    fun `bodyFamilyFor selects correct font family across locales`() {
        // English -> JakartaFamily
        assertSame(JakartaFamily, bodyFamilyFor(Locale.ENGLISH))
        assertSame(JakartaFamily, bodyFamilyFor(Locale("en")))
        assertSame(JakartaFamily, bodyFamilyFor(Locale("en", "US")))
        assertSame(JakartaFamily, bodyFamilyFor(Locale("en", "GB")))

        // Bengali ("bn") -> BengaliFamily
        assertSame(BengaliFamily, bodyFamilyFor(Locale("bn")))
        assertSame(BengaliFamily, bodyFamilyFor(Locale("bn", "BD")))
        assertSame(BengaliFamily, bodyFamilyFor(Locale("bn", "IN")))
        assertSame(BengaliFamily, bodyFamilyFor(Locale("BN")))

        // Hindi ("hi") -> DevanagariFamily
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("hi")))
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("hi", "IN")))
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("HI")))

        // Marathi ("mr") -> DevanagariFamily
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("mr")))
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("mr", "IN")))

        // Nepali ("ne") -> DevanagariFamily
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("ne")))
        assertSame(DevanagariFamily, bodyFamilyFor(Locale("ne", "NP")))

        // Default fallbacks -> JakartaFamily
        assertSame(JakartaFamily, bodyFamilyFor(Locale.FRENCH))
        assertSame(JakartaFamily, bodyFamilyFor(Locale.GERMAN))
        assertSame(JakartaFamily, bodyFamilyFor(Locale.JAPANESE))
        assertSame(JakartaFamily, bodyFamilyFor(Locale.ROOT))
        assertSame(JakartaFamily, bodyFamilyFor(Locale("es")))
    }

    @Test
    fun `wordmark font family is always JakartaFamily`() {
        assertSame(JakartaFamily, WordmarkFamily)
    }

    @Test
    fun `isIndicLocale correctly identifies supported Indic languages`() {
        assertTrue(isIndicLocale(Locale("bn")))
        assertTrue(isIndicLocale(Locale("hi")))
        assertTrue(isIndicLocale(Locale("mr")))
        assertTrue(isIndicLocale(Locale("ne")))

        assertFalse(isIndicLocale(Locale.ENGLISH))
        assertFalse(isIndicLocale(Locale("en")))
        assertFalse(isIndicLocale(Locale.FRENCH))
        assertFalse(isIndicLocale(Locale.ROOT))
    }

    // ─── 2. All 13 Typography Styles Verification ─────────────────────────────

    private fun extractStyles(typo: DtnTypography): List<Pair<String, TextStyle>> = listOf(
        "display" to typo.display,
        "displayMd" to typo.displayMd,
        "headlineLg" to typo.headlineLg,
        "headline" to typo.headline,
        "title" to typo.title,
        "body" to typo.body,
        "bodyStrong" to typo.bodyStrong,
        "button" to typo.button,
        "label" to typo.label,
        "captionTag" to typo.captionTag,
        "caption" to typo.caption,
        "footerValue" to typo.footerValue,
        "ids" to typo.ids
    )

    @Test
    fun `Latin typography defines all 13 styles with correct base attributes`() {
        val latin = createDtnTypography(Locale.ENGLISH)
        val styles = extractStyles(latin)
        assertEquals(13, styles.size)

        // display: 64sp, lh 70sp, -0.03em tracking, w600
        assertEquals(64.sp, latin.display.fontSize)
        assertEquals(70.sp, latin.display.lineHeight)
        assertEquals((-0.03f).em, latin.display.letterSpacing)
        assertEquals(FontWeight.W600, latin.display.fontWeight)

        // displayMd: 52sp, lh 58sp, -0.03em tracking, w600
        assertEquals(52.sp, latin.displayMd.fontSize)
        assertEquals(58.sp, latin.displayMd.lineHeight)
        assertEquals((-0.03f).em, latin.displayMd.letterSpacing)
        assertEquals(FontWeight.W600, latin.displayMd.fontWeight)

        // headlineLg: 32sp, lh 37sp, -0.02em tracking, w600
        assertEquals(32.sp, latin.headlineLg.fontSize)
        assertEquals(37.sp, latin.headlineLg.lineHeight)
        assertEquals((-0.02f).em, latin.headlineLg.letterSpacing)
        assertEquals(FontWeight.W600, latin.headlineLg.fontWeight)

        // headline: 28sp, lh 34sp, -0.02em tracking, w600
        assertEquals(28.sp, latin.headline.fontSize)
        assertEquals(34.sp, latin.headline.lineHeight)
        assertEquals((-0.02f).em, latin.headline.letterSpacing)
        assertEquals(FontWeight.W600, latin.headline.fontWeight)

        // title: 20sp, lh 26sp, -0.01em tracking, w500
        assertEquals(20.sp, latin.title.fontSize)
        assertEquals(26.sp, latin.title.lineHeight)
        assertEquals((-0.01f).em, latin.title.letterSpacing)
        assertEquals(FontWeight.W500, latin.title.fontWeight)

        // body: 16sp, lh 24sp, 0em tracking, w400
        assertEquals(16.sp, latin.body.fontSize)
        assertEquals(24.sp, latin.body.lineHeight)
        assertEquals(0.em, latin.body.letterSpacing)
        assertEquals(FontWeight.W400, latin.body.fontWeight)

        // bodyStrong: 16sp, lh 24sp, 0em tracking, w500
        assertEquals(16.sp, latin.bodyStrong.fontSize)
        assertEquals(24.sp, latin.bodyStrong.lineHeight)
        assertEquals(0.em, latin.bodyStrong.letterSpacing)
        assertEquals(FontWeight.W500, latin.bodyStrong.fontWeight)

        // button: 16sp, lh 24sp, 0em tracking, w600
        assertEquals(16.sp, latin.button.fontSize)
        assertEquals(24.sp, latin.button.lineHeight)
        assertEquals(0.em, latin.button.letterSpacing)
        assertEquals(FontWeight.W600, latin.button.fontWeight)

        // label: 14sp, lh 20sp, 0em tracking, w400
        assertEquals(14.sp, latin.label.fontSize)
        assertEquals(20.sp, latin.label.lineHeight)
        assertEquals(0.em, latin.label.letterSpacing)
        assertEquals(FontWeight.W400, latin.label.fontWeight)

        // captionTag: 12sp, lh 16sp, 0.01em tracking, w400
        assertEquals(12.sp, latin.captionTag.fontSize)
        assertEquals(16.sp, latin.captionTag.lineHeight)
        assertEquals(0.01f.em, latin.captionTag.letterSpacing)
        assertEquals(FontWeight.W400, latin.captionTag.fontWeight)

        // caption: 12sp, lh 16sp, 0.01em tracking, w400
        assertEquals(12.sp, latin.caption.fontSize)
        assertEquals(16.sp, latin.caption.lineHeight)
        assertEquals(0.01f.em, latin.caption.letterSpacing)
        assertEquals(FontWeight.W400, latin.caption.fontWeight)

        // footerValue: 16sp, lh 20sp, 0em tracking, w600
        assertEquals(16.sp, latin.footerValue.fontSize)
        assertEquals(20.sp, latin.footerValue.lineHeight)
        assertEquals(0.em, latin.footerValue.letterSpacing)
        assertEquals(FontWeight.W600, latin.footerValue.fontWeight)

        // ids: 14sp, lh 20sp, 0em tracking, w400, Monospace
        assertEquals(14.sp, latin.ids.fontSize)
        assertEquals(20.sp, latin.ids.lineHeight)
        assertEquals(0.em, latin.ids.letterSpacing)
        assertEquals(FontWeight.W400, latin.ids.fontWeight)
        assertSame(FontFamily.Monospace, latin.ids.fontFamily)
    }

    // ─── 3. Indic-Script Overrides (§3.2.4) ───────────────────────────────────

    @Test
    fun `Indic script typography applies 1_25x line height multiplier to all styles`() {
        val latin = createDtnTypography(Locale.ENGLISH)
        val indicLocales = listOf(Locale("bn"), Locale("hi"), Locale("mr"), Locale("ne"))

        for (loc in indicLocales) {
            val indic = createDtnTypography(loc)
            val latinStyles = extractStyles(latin)
            val indicStyles = extractStyles(indic)

            for (i in latinStyles.indices) {
                val (name, latinStyle) = latinStyles[i]
                val (_, indicStyle) = indicStyles[i]

                val expectedLh = (latinStyle.lineHeight.value * 1.25f).sp
                assertEquals(
                    expectedLh.value,
                    indicStyle.lineHeight.value,
                    0.01f,
                    "Style $name in locale ${loc.language} must have 1.25x line height"
                )
            }
        }
    }

    @Test
    fun `Indic script typography enforces 0em letter spacing across all styles`() {
        val indicLocales = listOf(Locale("bn"), Locale("hi"), Locale("mr"), Locale("ne"))

        for (loc in indicLocales) {
            val indic = createDtnTypography(loc)
            val styles = extractStyles(indic)

            for ((name, style) in styles) {
                assertEquals(
                    0.em,
                    style.letterSpacing,
                    "Style $name in locale ${loc.language} must have letterSpacing 0.em"
                )
            }
        }
    }

    @Test
    fun `Indic script typography adds +1sp bonus exclusively to label, captionTag, and caption`() {
        val latin = createDtnTypography(Locale.ENGLISH)
        val indicLocales = listOf(Locale("bn"), Locale("hi"), Locale("mr"), Locale("ne"))

        for (loc in indicLocales) {
            val indic = createDtnTypography(loc)

            // Small text elements get +1sp
            assertEquals(15.sp, indic.label.fontSize, "label in ${loc.language} must be 15sp (+1sp)")
            assertEquals(13.sp, indic.captionTag.fontSize, "captionTag in ${loc.language} must be 13sp (+1sp)")
            assertEquals(13.sp, indic.caption.fontSize, "caption in ${loc.language} must be 13sp (+1sp)")

            // All other styles maintain their exact Latin base font size
            assertEquals(latin.display.fontSize, indic.display.fontSize)
            assertEquals(latin.displayMd.fontSize, indic.displayMd.fontSize)
            assertEquals(latin.headlineLg.fontSize, indic.headlineLg.fontSize)
            assertEquals(latin.headline.fontSize, indic.headline.fontSize)
            assertEquals(latin.title.fontSize, indic.title.fontSize)
            assertEquals(latin.body.fontSize, indic.body.fontSize)
            assertEquals(latin.bodyStrong.fontSize, indic.bodyStrong.fontSize)
            assertEquals(latin.button.fontSize, indic.button.fontSize)
            assertEquals(latin.footerValue.fontSize, indic.footerValue.fontSize)
            assertEquals(latin.ids.fontSize, indic.ids.fontSize)
        }
    }

    @Test
    fun `ids style preserves Monospace font family across all Indic locales`() {
        val indicLocales = listOf(Locale("bn"), Locale("hi"), Locale("mr"), Locale("ne"))
        for (loc in indicLocales) {
            val indic = createDtnTypography(loc)
            assertSame(
                FontFamily.Monospace,
                indic.ids.fontFamily,
                "ids style in locale ${loc.language} must use FontFamily.Monospace"
            )
        }
    }

    // ─── 4. Tabular Figures ('tnum') Across All Styles (§3.2.5) ───────────────

    @Test
    fun `tabular figures fontFeatureSettings is set to tnum across all 13 styles in Latin`() {
        val latin = createDtnTypography(Locale.ENGLISH)
        for ((name, style) in extractStyles(latin)) {
            assertEquals("tnum", style.fontFeatureSettings, "Latin style $name must have fontFeatureSettings == 'tnum'")
        }
    }

    @Test
    fun `tabular figures fontFeatureSettings is set to tnum across all 13 styles in Indic locales`() {
        for (loc in listOf(Locale("bn"), Locale("hi"), Locale("mr"), Locale("ne"))) {
            val indic = createDtnTypography(loc)
            for ((name, style) in extractStyles(indic)) {
                assertEquals(
                    "tnum",
                    style.fontFeatureSettings,
                    "Indic style $name ($loc) must have fontFeatureSettings == 'tnum'"
                )
            }
        }
    }

    @Test
    fun `DtnType default singleton exposes tnum on all styles`() {
        assertEquals("tnum", DtnType.display.fontFeatureSettings)
        assertEquals("tnum", DtnType.displayMd.fontFeatureSettings)
        assertEquals("tnum", DtnType.headlineLg.fontFeatureSettings)
        assertEquals("tnum", DtnType.headline.fontFeatureSettings)
        assertEquals("tnum", DtnType.title.fontFeatureSettings)
        assertEquals("tnum", DtnType.body.fontFeatureSettings)
        assertEquals("tnum", DtnType.bodyStrong.fontFeatureSettings)
        assertEquals("tnum", DtnType.button.fontFeatureSettings)
        assertEquals("tnum", DtnType.label.fontFeatureSettings)
        assertEquals("tnum", DtnType.captionTag.fontFeatureSettings)
        assertEquals("tnum", DtnType.caption.fontFeatureSettings)
        assertEquals("tnum", DtnType.footerValue.fontFeatureSettings)
        assertEquals("tnum", DtnType.ids.fontFeatureSettings)
    }

    // ─── 5. Text Rendering Attributes (§3.2.6) ────────────────────────────────

    @Test
    fun `rendering settings enforce includeFontPadding false and centered LineHeightStyle`() {
        val typo = createDtnTypography(Locale.ENGLISH)
        for ((name, style) in extractStyles(typo)) {
            @Suppress("DEPRECATION")
            val platformStyle = style.platformStyle
            assertNotNull(platformStyle, "Style $name must have platformStyle")
            assertEquals(
                false,
                platformStyle?.paragraphStyle?.includeFontPadding,
                "Style $name must set includeFontPadding = false"
            )

            val lhStyle = style.lineHeightStyle
            assertNotNull(lhStyle, "Style $name must have lineHeightStyle")
            assertEquals(
                LineHeightStyle.Alignment.Center,
                lhStyle?.alignment,
                "Style $name must use centered line height alignment"
            )
            assertEquals(
                LineHeightStyle.Trim.None,
                lhStyle?.trim,
                "Style $name must use Trim.None"
            )

            assertEquals(Hyphens.None, style.hyphens, "Style $name must disable hyphens")
        }
    }

    @Test
    fun `lineBreak policy applies Heading to titles and Paragraph to text`() {
        val typo = createDtnTypography(Locale.ENGLISH)

        // Headings: display, displayMd, headlineLg, headline, title
        assertEquals(LineBreak.Heading, typo.display.lineBreak)
        assertEquals(LineBreak.Heading, typo.displayMd.lineBreak)
        assertEquals(LineBreak.Heading, typo.headlineLg.lineBreak)
        assertEquals(LineBreak.Heading, typo.headline.lineBreak)
        assertEquals(LineBreak.Heading, typo.title.lineBreak)

        // Paragraph: body, bodyStrong, button, label, captionTag, caption, footerValue, ids
        assertEquals(LineBreak.Paragraph, typo.body.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.bodyStrong.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.button.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.label.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.captionTag.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.caption.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.footerValue.lineBreak)
        assertEquals(LineBreak.Paragraph, typo.ids.lineBreak)
    }

    // ─── 6. Graceful Font Resource Fallback ────────────────────────────────────

    @Test
    fun `createVariableFontFamily returns fallback when resource id is non-positive`() {
        val fallbackCustom = FontFamily.Cursive
        val resultZero = createVariableFontFamily(0, fallback = fallbackCustom)
        assertSame(fallbackCustom, resultZero)

        val resultNegative = createVariableFontFamily(-123, fallback = fallbackCustom)
        assertSame(fallbackCustom, resultNegative)

        val resultDefault = createVariableFontFamily(0)
        assertSame(FontFamily.SansSerif, resultDefault)
    }

    @Test
    fun `forceSansSerifFallback redirects all font families to SansSerif`() {
        try {
            forceSansSerifFallback = true

            assertSame(FontFamily.SansSerif, JakartaFamily)
            assertSame(FontFamily.SansSerif, BengaliFamily)
            assertSame(FontFamily.SansSerif, DevanagariFamily)
            assertSame(FontFamily.SansSerif, WordmarkFamily)

            // bodyFamilyFor also resolves to SansSerif
            assertSame(FontFamily.SansSerif, bodyFamilyFor(Locale.ENGLISH))
            assertSame(FontFamily.SansSerif, bodyFamilyFor(Locale("bn")))
            assertSame(FontFamily.SansSerif, bodyFamilyFor(Locale("hi")))
        } finally {
            forceSansSerifFallback = false
        }
    }

    @Test
    fun `Material3 typography conversion preserves all key type styles`() {
        val dtnTypo = createDtnTypography(Locale.ENGLISH)
        val m3Typo = dtnTypo.toMaterial3Typography()

        assertEquals(dtnTypo.display, m3Typo.displayLarge)
        assertEquals(dtnTypo.displayMd, m3Typo.displayMedium)
        assertEquals(dtnTypo.headlineLg, m3Typo.headlineLarge)
        assertEquals(dtnTypo.headline, m3Typo.headlineMedium)
        assertEquals(dtnTypo.title, m3Typo.titleLarge)
        assertEquals(dtnTypo.bodyStrong, m3Typo.bodyLarge)
        assertEquals(dtnTypo.body, m3Typo.bodyMedium)
        assertEquals(dtnTypo.button, m3Typo.labelLarge)
        assertEquals(dtnTypo.label, m3Typo.bodySmall)
        assertEquals(dtnTypo.caption, m3Typo.labelSmall)
    }
}
