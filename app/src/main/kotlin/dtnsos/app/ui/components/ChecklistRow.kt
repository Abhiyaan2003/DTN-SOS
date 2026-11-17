package dtnsos.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun ChecklistRow(
    text: String,
    isOk: Boolean,
    reason: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isOk) Icons.Default.Check else Icons.Default.Warning,
            contentDescription = if (isOk) "Ready" else "Warning",
            tint = if (isOk) DtnColors.Positive else DtnColors.Warning,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = DtnType.bodyStrong,
                color = DtnColors.TextPrimary
            )
            if (!isOk && !reason.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = reason,
                    style = DtnType.caption,
                    color = DtnColors.Warning
                )
            }
        }
    }
}

