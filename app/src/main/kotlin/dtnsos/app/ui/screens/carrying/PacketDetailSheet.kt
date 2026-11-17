package dtnsos.app.ui.screens.carrying

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.DtnBottomSheet
import dtnsos.app.ui.components.KeyValueRow
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.model.PacketState
import dtnsos.app.ui.model.PacketUi
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun PacketDetailSheet(
    packet: PacketUi,
    onDismiss: () -> Unit
) {
    DtnBottomSheet(onDismissRequest = onDismiss) {
        val categoryName = packet.category.name.lowercase().replaceFirstChar { it.uppercase() }
        val peopleText = if (packet.people == 1) "1 person" else "${packet.people} people"
        val titleText = "$categoryName · $peopleText"

        Column {
            Text(
                text = titleText,
                style = DtnType.title,
                color = DtnColors.TextPrimary
            )
            if (packet.message.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = packet.message,
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
            }
        }

        val copiesText = when (packet.state) {
            PacketState.SPRAY -> stringResource(R.string.copies_spraying, packet.tokens)
            PacketState.WAIT -> stringResource(R.string.copies_waiting)
            PacketState.DELIVERED -> stringResource(R.string.copies_delivered)
        }

        val severityName = packet.severity.name.lowercase().replaceFirstChar { it.uppercase() }

        Column {
            KeyValueRow(
                label = stringResource(R.string.key_message_id),
                value = packet.msgIdHex,
                showDivider = true,
                isMonospace = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_severity),
                value = severityName,
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_age),
                value = packet.ageText,
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_lifetime_left),
                value = "${packet.lifetimeLeftHours} h",
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_copies_can_pass),
                value = copiesText,
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_signature),
                value = stringResource(R.string.sig_not_checked),
                showDivider = false
            )
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = stringResource(R.string.action_done),
            onClick = onDismiss
        )
    }
}

