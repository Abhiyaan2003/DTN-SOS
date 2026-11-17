package dtnsos.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnMotion
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CARRYING("Carrying", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Default.Tune)
}

@Composable
fun GlassNavBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    showSosFab: Boolean,
    onSosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val tabs = NavTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DtnSpacing.gutter, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frosted Glass Nav Pill
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = DtnShapes.radiusFull,
                    ambientColor = Color.Black.copy(alpha = 0.65f),
                    spotColor = Color.Black.copy(alpha = 0.65f)
                )
                .clip(DtnShapes.radiusFull)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB326262B), // translucent frosted top (~70%)
                            Color(0x8C141416)  // translucent frosted base (~55%)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f), // crisp top specular highlight
                            Color.White.copy(alpha = 0.08f)  // subtle bottom border
                        )
                    ),
                    shape = DtnShapes.radiusFull
                )
                .padding(6.dp)
        ) {
            val availableWidth = maxWidth
            val tabWidth = availableWidth / tabs.size
            val activePillOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = tween(
                    durationMillis = DtnMotion.DurationNavSlide,
                    easing = DtnMotion.EaseOut
                ),
                label = "navSlide"
            )

            // Active background pill slider with frosted inner fill & subtle border
            Box(
                modifier = Modifier
                    .offset(x = activePillOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(DtnShapes.radiusFull)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xE6323238),
                                Color(0xCC242428)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), DtnShapes.radiusFull)
            )

            // Tab items row perfectly mapped to widths
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == currentTab
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(DtnShapes.radiusFull)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptics.play(HapticEvent.Tap)
                                        onTabSelected(tab)
                                    }
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) DtnColors.TextPrimary else DtnColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = DtnType.caption.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.W600 else FontWeight.W400,
                                color = if (isSelected) DtnColors.TextPrimary else DtnColors.TextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Detached SOS FAB
        if (showSosFab) {
            SosFab(onClick = onSosClick)
        }
    }
}

