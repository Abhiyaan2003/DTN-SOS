package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType

@Composable
fun LinkCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DtnShapes.radiusLg)
            .background(DtnColors.Surface1)
            .border(1.dp, DtnColors.BorderSubtle, DtnShapes.radiusLg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.play(HapticEvent.Tap)
                    onClick()
                }
            )
            .pressScale(interactionSource)
            .padding(DtnSpacing.cardPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DtnColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

