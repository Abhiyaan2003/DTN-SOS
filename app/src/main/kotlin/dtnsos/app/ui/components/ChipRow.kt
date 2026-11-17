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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

data class ChipOption<T>(
    val value: T,
    val label: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipRow(
    options: List<ChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(DtnShapes.radiusFull)
                    .background(if (isSelected) DtnColors.Cream else DtnColors.Surface2)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (!isSelected) {
                                haptics.play(HapticEvent.Tap)
                                onSelect(option.value)
                            }
                        }
                    )
                    .pressScale(interactionSource)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = DtnType.label.copy(
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                        color = if (isSelected) DtnColors.OnCream else DtnColors.TextSecondary
                    )
                )
            }
        }
    }
}

