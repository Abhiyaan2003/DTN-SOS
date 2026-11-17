package dtnsos.app.ui.screens.interrupts

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
fun BluetoothOffScreen(
    onTurnOnBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                icon = Icons.Default.Bluetooth,
                tint = DtnColors.TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.bt_off_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.bt_off_body),
                style = DtnType.body,
                color = DtnColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(R.string.action_turn_on_bt),
                onClick = onTurnOnBluetooth
            )

            Spacer(Modifier.height(12.dp))

            GhostButton(
                text = stringResource(R.string.action_open_settings),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
            )
        }
    }
}

