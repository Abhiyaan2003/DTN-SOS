package dtnsos.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.model.Capability
import dtnsos.app.ui.model.Role
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType
import dtnsos.app.ui.theme.WordmarkFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBar(
    role: Role,
    listening: Boolean,
    hasActiveSos: Boolean,
    capability: Capability,
    screenTitle: String? = null,
    onDebugLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screenTitle != null) {
            Text(
                text = screenTitle,
                style = DtnType.headline,
                color = DtnColors.TextPrimary
            )
        } else {
            // Wordmark: dtn + . in AccentDot (always Plus Jakarta Sans 600 per §3.2.1)
            val wordmark = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = DtnColors.TextPrimary
                    )
                ) {
                    append("dtn")
                }
                withStyle(
                    SpanStyle(
                        color = DtnColors.AccentDot
                    )
                ) {
                    append(".")
                }
            }

            Text(
                text = wordmark,
                style = DtnType.headline.copy(fontFamily = WordmarkFamily),
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onDebugLongClick
                )
            )
        }

        StatusPill(
            role = role,
            listening = listening,
            hasActiveSos = hasActiveSos,
            capability = capability
        )
    }
}

