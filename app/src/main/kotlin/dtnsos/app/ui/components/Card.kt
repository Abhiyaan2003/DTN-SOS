package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnSpacing

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(DtnShapes.radiusLg)
            .background(DtnColors.Surface1)
            .border(1.dp, DtnColors.BorderSubtle, DtnShapes.radiusLg)
            .padding(DtnSpacing.cardPadding),
        content = content
    )
}

