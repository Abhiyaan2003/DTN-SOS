package dtnsos.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnSpacing

@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(DtnShapes.radiusLg)
            .border(1.dp, DtnColors.BorderSubtle, DtnShapes.radiusLg)
            .drawWithCache {
                val cardWidth = size.width
                val cardHeight = size.height

                // Radial glow anchored at bottom-right
                val glowBrush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to DtnColors.Accent.copy(alpha = 0.28f),
                        0.4f to DtnColors.AccentDeep.copy(alpha = 0.12f),
                        0.7f to Color.Transparent
                    ),
                    center = Offset(cardWidth, cardHeight),
                    radius = cardWidth * 1.2f
                )

                onDrawBehind {
                    // Surface 1 base
                    drawRect(color = DtnColors.Surface1)
                    // Glow
                    drawRect(brush = glowBrush)

                    // Dot grid texture: 6dp spacing, 1dp dot, 5% white
                    val spacingPx = 6.dp.toPx()
                    val dotRadius = 0.5.dp.toPx()
                    val dotColor = Color.White.copy(alpha = 0.05f)

                    var x = spacingPx / 2
                    while (x < cardWidth) {
                        var y = spacingPx / 2
                        while (y < cardHeight) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            y += spacingPx
                        }
                        x += spacingPx
                    }
                }
            }
            .padding(DtnSpacing.heroPadding),
        content = content
    )
}

