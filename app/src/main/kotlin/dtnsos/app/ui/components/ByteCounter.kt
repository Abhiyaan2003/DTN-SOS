package dtnsos.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

object Utf8Utils {
    fun byteCount(text: String): Int {
        return text.toByteArray(Charsets.UTF_8).size
    }

    /**
     * Rejects new input if it exceeds maxBytes, ensuring characters are never split.
     */
    fun trimOrReject(newText: String, maxBytes: Int, currentText: String): String {
        return if (byteCount(newText) <= maxBytes) {
            newText
        } else {
            currentText
        }
    }
}

@Composable
fun ByteCounter(
    usedBytes: Int,
    maxBytes: Int = 51,
    modifier: Modifier = Modifier
) {
    val isNearLimit = usedBytes >= 48
    val color = if (isNearLimit) DtnColors.Warning else DtnColors.TextSecondary

    Text(
        text = "$usedBytes/$maxBytes",
        style = DtnType.label,
        color = color,
        textAlign = TextAlign.End,
        modifier = modifier
    )
}

