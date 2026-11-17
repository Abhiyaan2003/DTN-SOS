package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.model.Capability
import dtnsos.app.ui.model.Role
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnType

@Composable
fun StatusPill(
    role: Role,
    listening: Boolean,
    hasActiveSos: Boolean,
    capability: Capability,
    modifier: Modifier = Modifier
) {
    // Status text priority: Gateway -> Broadcasting -> Paused -> Carry only -> Listening
    val (label, dotColor) = when {
        role == Role.GATEWAY -> "Gateway" to DtnColors.Info
        hasActiveSos -> "Broadcasting" to DtnColors.Sos
        !listening -> "Paused" to DtnColors.TextTertiary
        capability == Capability.CARRY_ONLY -> "Carry only" to DtnColors.Warning
        else -> "Listening" to DtnColors.Positive
    }

    Row(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clip(DtnShapes.radiusFull)
            .border(1.dp, DtnColors.BorderStrong, DtnShapes.radiusFull)
            .background(Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(DtnShapes.radiusFull)
                .background(dotColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = DtnType.label,
            color = DtnColors.TextSecondary
        )
    }
}

