package dtnsos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = {
            haptics.play(HapticEvent.Tap)
            onClick()
        },
        enabled = enabled,
        shape = DtnShapes.radiusFull,
        colors = ButtonDefaults.buttonColors(
            containerColor = DtnColors.Cream,
            contentColor = DtnColors.OnCream,
            disabledContainerColor = DtnColors.Cream.copy(alpha = 0.4f),
            disabledContentColor = DtnColors.OnCream.copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interactionSource)
    ) {
        Text(
            text = text,
            style = DtnType.button,
            color = if (enabled) DtnColors.OnCream else DtnColors.OnCream.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = {
            haptics.play(HapticEvent.Tap)
            onClick()
        },
        enabled = enabled,
        shape = DtnShapes.radiusFull,
        colors = ButtonDefaults.buttonColors(
            containerColor = DtnColors.Sos,
            contentColor = Color.White,
            disabledContainerColor = DtnColors.Sos.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.4f)
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interactionSource)
    ) {
        Text(
            text = text,
            style = DtnType.button,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    OutlinedButton(
        onClick = {
            haptics.play(HapticEvent.Tap)
            onClick()
        },
        enabled = enabled,
        shape = DtnShapes.radiusFull,
        border = BorderStroke(1.dp, if (enabled) DtnColors.BorderStrong else DtnColors.BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = DtnColors.TextPrimary,
            disabledContentColor = DtnColors.TextTertiary
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pressScale(interactionSource)
    ) {
        Text(
            text = text,
            style = DtnType.button,
            color = if (enabled) DtnColors.TextPrimary else DtnColors.TextTertiary
        )
    }
}

