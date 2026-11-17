package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

@Composable
fun SosFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(
                elevation = 16.dp,
                shape = DtnShapes.radiusFull,
                ambientColor = Color.Black.copy(alpha = 0.65f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(DtnShapes.radiusFull)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF5C4D),
                        DtnColors.Sos
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = DtnShapes.radiusFull
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.play(HapticEvent.OpenCompose)
                    onClick()
                }
            )
            .pressScale(interactionSource)
            .semantics { contentDescription = "Send SOS" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SOS",
            style = DtnType.title.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = Color.White
            )
        )
    }
}

