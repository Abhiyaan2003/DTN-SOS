package dtnsos.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnMotion
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.rememberIsReducedMotion

@Composable
fun PulsingRing(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isReducedMotion = rememberIsReducedMotion()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseLoop")
    val expansionDp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DtnMotion.DurationPulseLoop, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseExpansion"
    )
    val alphaVal by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = DtnMotion.DurationPulseLoop, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        if (!isReducedMotion) {
            Box(
                modifier = Modifier
                    .size((150 + expansionDp).dp)
                    .clip(DtnShapes.radiusFull)
                    .border(2.dp, DtnColors.Sos.copy(alpha = alphaVal), DtnShapes.radiusFull)
            )
        }

        // Main inner ring
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(DtnShapes.radiusFull)
                .border(2.dp, DtnColors.Sos, DtnShapes.radiusFull),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

