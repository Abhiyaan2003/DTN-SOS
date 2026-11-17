package dtnsos.app.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.*
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.model.Role
import dtnsos.app.ui.model.ScanIntensity
import dtnsos.app.ui.model.SettingsUi
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun SettingsScreen(
    settings: SettingsUi,
    hasActiveSos: Boolean,
    onRoleChange: (Role) -> Unit,
    onScanChange: (ScanIntensity) -> Unit,
    onTtlChange: (Int) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onNavigateToGatewaySetup: () -> Unit,
    onNavigateToBackgroundHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current

    ScreenScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary
            )
        }

        // 1. Role Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.role_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.role_helper),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                SegmentedControl(
                    options = listOf(
                        SegmentOption(Role.MULE, stringResource(R.string.role_mule)),
                        SegmentOption(Role.GATEWAY, stringResource(R.string.role_gateway))
                    ),
                    selected = settings.role,
                    onSelect = { newRole ->
                        if (hasActiveSos) {
                            Toast.makeText(context, context.getString(R.string.toast_cancel_sos_first), Toast.LENGTH_SHORT).show()
                        } else {
                            if (newRole == Role.GATEWAY) {
                                onNavigateToGatewaySetup()
                            } else {
                                onRoleChange(Role.MULE)
                            }
                        }
                    }
                )
            }
        }

        // 2. Scan Intensity Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.scan_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.scan_helper),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                SegmentedControl(
                    options = listOf(
                        SegmentOption(ScanIntensity.SAVER, stringResource(R.string.scan_saver)),
                        SegmentOption(ScanIntensity.BALANCED, stringResource(R.string.scan_balanced)),
                        SegmentOption(ScanIntensity.FAST, stringResource(R.string.scan_fast))
                    ),
                    selected = settings.scan,
                    onSelect = onScanChange
                )
            }
        }

        // 3. Default Lifetime Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.ttl_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.ttl_helper),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                SegmentedControl(
                    options = listOf(
                        SegmentOption(24, stringResource(R.string.ttl_24)),
                        SegmentOption(48, stringResource(R.string.ttl_48))
                    ),
                    selected = settings.ttlHours,
                    onSelect = onTtlChange
                )
            }
        }

        // 4. Haptic Feedback Card (only shown if device has vibrator)
        if (haptics.hasVibrator()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.haptics_title),
                        style = DtnType.title,
                        color = DtnColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.haptics_helper),
                        style = DtnType.label,
                        color = DtnColors.TextSecondary
                    )
                    SegmentedControl(
                        options = listOf(
                            SegmentOption(true, stringResource(R.string.haptics_on)),
                            SegmentOption(false, stringResource(R.string.haptics_off))
                        ),
                        selected = settings.haptics,
                        onSelect = onHapticsChange
                    )
                }
            }
        }

        // 5. Info Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = stringResource(R.string.info_card_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                KeyValueRow(
                    label = stringResource(R.string.info_buffer),
                    value = "${settings.bufferLimit} packets",
                    showDivider = true
                )
                KeyValueRow(
                    label = stringResource(R.string.info_signing_key),
                    value = "Ed25519 · ID ${settings.keyIdShort}",
                    showDivider = true
                )
                KeyValueRow(
                    label = stringResource(R.string.info_bt5_broadcast),
                    value = if (settings.extendedAdvertising) {
                        stringResource(R.string.value_supported)
                    } else {
                        stringResource(R.string.value_not_supported)
                    },
                    showDivider = false
                )
            }
        }

        // 6. Background Help LinkCard
        LinkCard(
            title = stringResource(R.string.link_battery_help_title),
            subtitle = stringResource(R.string.link_battery_help_sub),
            onClick = onNavigateToBackgroundHelp
        )
    }
}

