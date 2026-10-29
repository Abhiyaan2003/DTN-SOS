package dtnsos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Palette ──────────────────────────────────────────────────────────────────
// Triage-inspired colour system: safe blues, warning ambers, critical reds.
private val PrimaryBlue   = Color(0xFF1565C0)
private val OnPrimary     = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF1E3A5F)
private val Secondary     = Color(0xFFFFB300)   // amber — warning / triage
private val Tertiary      = Color(0xFFD32F2F)   // red — critical triage
private val Background    = Color(0xFF0D1B2A)   // deep navy — dark mode
private val Surface       = Color(0xFF142035)
private val OnBackground  = Color(0xFFE8EDF2)
private val OnSurface     = Color(0xFFCFD8DC)
private val SurfaceVariant = Color(0xFF1C2D40)

private val DarkColors = darkColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary        = Secondary,
    tertiary         = Tertiary,
    background       = Background,
    surface          = Surface,
    onBackground     = OnBackground,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceVariant,
)

@Composable
fun DtnSosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content,
    )
}
