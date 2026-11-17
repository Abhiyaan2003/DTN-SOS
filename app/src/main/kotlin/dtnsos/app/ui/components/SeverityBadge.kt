package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.model.Severity
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes

@Composable
fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val baseColor = when (severity) {
        Severity.HIGH -> DtnColors.Sos
        Severity.MEDIUM -> DtnColors.Warning
        Severity.LOW -> DtnColors.Info
    }

    val gradient = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to baseColor.copy(alpha = 0.45f),
            0.6f to baseColor.copy(alpha = 0.12f),
            1.0f to Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(DtnShapes.radiusFull)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = DtnColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

