package dtnsos.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import java.util.Locale

private val DarkColorScheme = darkColorScheme(
    primary                = DtnColors.Cream,
    onPrimary              = DtnColors.OnCream,
    primaryContainer       = DtnColors.Surface2,
    onPrimaryContainer     = DtnColors.TextPrimary,
    secondary              = DtnColors.Warning,
    onSecondary            = DtnColors.OnCream,
    secondaryContainer     = DtnColors.Surface2,
    onSecondaryContainer   = DtnColors.TextSecondary,
    tertiary               = DtnColors.Positive,
    onTertiary             = DtnColors.OnCream,
    tertiaryContainer      = DtnColors.Surface2,
    onTertiaryContainer    = DtnColors.Positive,
    error                  = DtnColors.Sos,
    onError                = DtnColors.TextPrimary,
    errorContainer         = DtnColors.Surface2,
    onErrorContainer       = DtnColors.Negative,
    background             = DtnColors.BgBase,
    onBackground           = DtnColors.TextPrimary,
    surface                = DtnColors.Surface1,
    onSurface              = DtnColors.TextPrimary,
    surfaceVariant         = DtnColors.Surface2,
    onSurfaceVariant       = DtnColors.TextSecondary,
    outline                = DtnColors.BorderStrong,
    outlineVariant         = DtnColors.BorderSubtle,
)

object DtnTheme {
    val typography: DtnTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalDtnTypography.current
}

@Composable
fun DtnTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DtnColors.BgBase.toArgb()
                window.navigationBarColor = DtnColors.BgBase.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    val configuration = LocalConfiguration.current
    val locale = try {
        configuration.locales[0] ?: Locale.getDefault()
    } catch (_: Throwable) {
        Locale.getDefault()
    }
    val typography = remember(locale) { createDtnTypography(locale) }

    CompositionLocalProvider(
        LocalDtnTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = typography.toMaterial3Typography()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DtnColors.BgBase
            ) {
                content()
            }
        }
    }
}
