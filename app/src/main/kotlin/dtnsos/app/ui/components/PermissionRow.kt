package dtnsos.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.model.PermissionStatus
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun PermissionRow(
    title: String,
    reason: String,
    status: PermissionStatus,
    isRequired: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (buttonText, variant) = when (status) {
        PermissionStatus.NOT_ASKED -> "Allow" to TagVariant.DEFAULT
        PermissionStatus.GRANTED -> "Allowed" to TagVariant.ALLOWED
        PermissionStatus.DENIED -> "Allow" to TagVariant.DEFAULT
        PermissionStatus.PERMANENTLY_DENIED -> "Open settings" to TagVariant.WARNING
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DtnType.bodyStrong,
                color = DtnColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = reason,
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
            if (isRequired && (status == PermissionStatus.DENIED || status == PermissionStatus.PERMANENTLY_DENIED)) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Needed for the network to work.",
                    style = DtnType.caption,
                    color = DtnColors.Warning
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Tag(
            text = buttonText,
            variant = variant,
            minHeight = 44.dp,
            onClick = if (status != PermissionStatus.GRANTED) onActionClick else null
        )
    }
}

