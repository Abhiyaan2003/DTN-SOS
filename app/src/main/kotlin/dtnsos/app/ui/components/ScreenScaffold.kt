package dtnsos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnSpacing

@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtnColors.BgBase)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .widthIn(max = DtnSpacing.maxContentWidth)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = DtnSpacing.gutter)
                .padding(bottom = DtnSpacing.bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(DtnSpacing.cardGap),
            content = content
        )
    }
}

