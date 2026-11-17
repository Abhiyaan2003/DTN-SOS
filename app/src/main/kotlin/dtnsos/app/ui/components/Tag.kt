package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

enum class TagVariant {
    DEFAULT,
    WARNING,
    ALLOWED
}

@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    variant: TagVariant = TagVariant.DEFAULT,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 0.dp
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    val (textColor, borderColor, bgColor) = when (variant) {
        TagVariant.DEFAULT -> Triple(DtnColors.TextSecondary, DtnColors.BorderStrong, Color.Transparent)
        TagVariant.WARNING -> Triple(DtnColors.Warning, DtnColors.Warning, Color.Transparent)
        TagVariant.ALLOWED -> Triple(DtnColors.OnCream, DtnColors.Cream, DtnColors.Cream)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(DtnShapes.radiusFull)
            .background(bgColor)
            .border(1.dp, borderColor, DtnShapes.radiusFull)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptics.play(HapticEvent.Tap)
                                onClick()
                            }
                        )
                        .pressScale(interactionSource)
                } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = if (minHeight > 0.dp) 8.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = DtnType.captionTag,
            color = textColor
        )
    }
}

