package dtnsos.app.ui.screens.interrupts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun CancelSosSheet(
    onKeepBroadcasting: () -> Unit,
    onCancelSosConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHaptics.current

    DtnBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.cancel_sos_title),
            style = DtnType.title,
            color = DtnColors.TextPrimary
        )

        Text(
            text = stringResource(R.string.cancel_sos_body),
            style = DtnType.body,
            color = DtnColors.TextSecondary
        )

        Spacer(Modifier.height(8.dp))

        // Safe choice is highlighted button
        PrimaryButton(
            text = stringResource(R.string.action_keep_broadcasting),
            onClick = onKeepBroadcasting
        )

        GhostButton(
            text = stringResource(R.string.action_cancel_sos),
            onClick = {
                haptics.play(HapticEvent.Confirm)
                onCancelSosConfirm()
            }
        )
    }
}

