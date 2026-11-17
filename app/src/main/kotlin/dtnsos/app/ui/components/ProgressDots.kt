package dtnsos.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnMotion
import dtnsos.app.ui.theme.DtnShapes

@Composable
fun ProgressDots(
    currentStep: Int,
    totalSteps: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Step ${currentStep + 1} of $totalSteps" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isCurrent = i == currentStep
            val isCompleted = i < currentStep

            val widthDp by animateDpAsState(
                targetValue = if (isCurrent) 24.dp else 6.dp,
                animationSpec = tween(DtnMotion.DurationSegmentSlide),
                label = "dotWidth"
            )

            val color = if (isCurrent || isCompleted) DtnColors.Cream else DtnColors.Surface3

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(widthDp)
                    .clip(DtnShapes.radiusFull)
                    .background(color)
            )

            if (i < totalSteps - 1) {
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

