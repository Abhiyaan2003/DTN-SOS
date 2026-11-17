package dtnsos.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnMotion
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

data class SegmentOption<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }

    val segmentWidthDp = if (containerWidthPx > 0 && options.isNotEmpty()) {
        with(density) { ((containerWidthPx.toDp() - 8.dp) / options.size) }
    } else 0.dp

    val pillOffsetDp by animateDpAsState(
        targetValue = segmentWidthDp * selectedIndex + 4.dp,
        animationSpec = tween(
            durationMillis = DtnMotion.DurationSegmentSlide,
            easing = DtnMotion.EaseOut
        ),
        label = "segmentSlide"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DtnShapes.radiusFull)
            .background(DtnColors.Surface2)
            .padding(4.dp)
            .onGloballyPositioned { containerWidthPx = it.size.width }
    ) {
        // Sliding indicator pill
        if (segmentWidthDp > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = pillOffsetDp)
                    .width(segmentWidthDp)
                    .height(40.dp)
                    .clip(DtnShapes.radiusFull)
                    .background(DtnColors.Cream)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(DtnShapes.radiusFull)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptics.play(HapticEvent.Tap)
                                    onSelect(option.value)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        style = DtnType.label.copy(
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W500,
                            color = if (isSelected) DtnColors.OnCream else DtnColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

