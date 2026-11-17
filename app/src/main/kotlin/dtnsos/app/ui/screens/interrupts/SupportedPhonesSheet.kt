package dtnsos.app.ui.screens.interrupts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dtnsos.app.R
import dtnsos.app.ui.components.DtnBottomSheet
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun SupportedPhonesSheet(
    onDismiss: () -> Unit
) {
    DtnBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.supported_phones_title),
            style = DtnType.title,
            color = DtnColors.TextPrimary
        )

        Text(
            text = stringResource(R.string.supported_phones_body),
            style = DtnType.body,
            color = DtnColors.TextSecondary
        )

        PrimaryButton(
            text = stringResource(R.string.action_done),
            onClick = onDismiss
        )
    }
}

