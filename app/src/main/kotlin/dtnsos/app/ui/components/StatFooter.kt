package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

data class StatItem(
    val label: String,
    val value: String
)

@Composable
fun StatFooter(
    stats: List<StatItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        // Top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DtnColors.BorderSubtle)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(DtnColors.BorderSubtle)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (index == 0) 0.dp else 10.dp, end = 10.dp)
                ) {
                    Text(
                        text = item.label,
                        style = DtnType.captionTag,
                        color = DtnColors.TextSecondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.value,
                        style = DtnType.footerValue.copy(
                            color = DtnColors.TextPrimary
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

