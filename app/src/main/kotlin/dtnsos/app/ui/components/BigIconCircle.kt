package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes

@Composable
fun BigIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = DtnColors.TextSecondary
) {
    Box(
        modifier = modifier
            .padding(top = 24.dp)
            .size(96.dp)
            .clip(DtnShapes.radiusFull)
            .background(DtnColors.Surface2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(44.dp)
        )
    }
}

