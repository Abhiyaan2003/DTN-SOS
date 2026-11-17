package dtnsos.app.ui.screens.interrupts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.BigIconCircle
import dtnsos.app.ui.components.GhostButton
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType

@Composable
fun UnsupportedPhoneScreen(
    onContinueCarryOnly: () -> Unit,
    onSeeSupportedPhones: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current

    LaunchedEffect(Unit) {
        haptics.play(HapticEvent.Warning)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtnColors.BgBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = DtnSpacing.gutter),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = DtnSpacing.maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BigIconCircle(
                icon = Icons.Default.Warning,
                tint = DtnColors.Warning
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.unsupported_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.unsupported_body),
                style = DtnType.body,
                color = DtnColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(R.string.action_continue_carry_only),
                onClick = onContinueCarryOnly
            )

            Spacer(Modifier.height(12.dp))

            GhostButton(
                text = stringResource(R.string.action_see_supported_phones),
                onClick = onSeeSupportedPhones
            )
        }
    }
}

