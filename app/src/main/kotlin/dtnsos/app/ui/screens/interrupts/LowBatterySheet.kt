package dtnsos.app.ui.screens.interrupts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.DtnBottomSheet
import dtnsos.app.ui.components.GhostButton
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun LowBatterySheet(
    batteryPercent: Int,
    onSwitchToSaver: () -> Unit,
    onKeepCurrent: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHaptics.current

    LaunchedEffect(Unit) {
        haptics.play(HapticEvent.Warning)
    }

    DtnBottomSheet(onDismissRequest = onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryAlert,
                contentDescription = null,
                tint = DtnColors.Warning,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = stringResource(R.string.low_battery_title, batteryPercent),
                style = DtnType.title,
                color = DtnColors.TextPrimary
            )
        }

        Text(
            text = stringResource(R.string.low_battery_body),
            style = DtnType.body,
            color = DtnColors.TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = stringResource(R.string.action_switch_to_saver),
            onClick = onSwitchToSaver
        )

        GhostButton(
            text = stringResource(R.string.action_keep_current_settings),
            onClick = onKeepCurrent
        )
    }
}

