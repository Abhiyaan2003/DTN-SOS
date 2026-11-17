package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 1,
    maxValue: Int = 255,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current

    val labelText = if (value == 1) "1 person" else "$value people"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus button
        val minusInteractionSource = remember { MutableInteractionSource() }
        val canDecrement = value > minValue
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(DtnShapes.radiusFull)
                .background(if (canDecrement) DtnColors.Surface2 else DtnColors.Surface2.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = minusInteractionSource,
                    indication = null,
                    enabled = canDecrement,
                    onClick = {
                        haptics.play(HapticEvent.Tap)
                        onValueChange(value - 1)
                    }
                )
                .pressScale(minusInteractionSource)
                .semantics { contentDescription = "Fewer people" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                style = DtnType.title.copy(
                    fontSize = 22.sp,
                    color = if (canDecrement) DtnColors.TextPrimary else DtnColors.TextTertiary
                )
            )
        }

        Spacer(Modifier.width(16.dp))

        // Center value
        Box(
            modifier = Modifier.widthIn(min = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                style = DtnType.title.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W600,
                    color = DtnColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(Modifier.width(16.dp))

        // Plus button
        val plusInteractionSource = remember { MutableInteractionSource() }
        val canIncrement = value < maxValue
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(DtnShapes.radiusFull)
                .background(if (canIncrement) DtnColors.Surface2 else DtnColors.Surface2.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = plusInteractionSource,
                    indication = null,
                    enabled = canIncrement,
                    onClick = {
                        haptics.play(HapticEvent.Tap)
                        onValueChange(value + 1)
                    }
                )
                .pressScale(plusInteractionSource)
                .semantics { contentDescription = "More people" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                style = DtnType.title.copy(
                    fontSize = 22.sp,
                    color = if (canIncrement) DtnColors.TextPrimary else DtnColors.TextTertiary
                )
            )
        }
    }
}

