package dtnsos.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.Card
import dtnsos.app.ui.components.GhostButton
import dtnsos.app.ui.components.ScreenScaffold
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType

@Composable
fun BackgroundHelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manufacturer = Build.MANUFACTURER.lowercase()
    val isXiaomi = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    val isNothing = manufacturer.contains("nothing")

    ScreenScaffold(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DtnColors.TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bg_help_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary
            )
        }

        Text(
            text = stringResource(R.string.bg_help_intro),
            style = DtnType.body,
            color = DtnColors.TextSecondary
        )

        // Step 1: Notifications
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.bg_step_notifications),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.bg_step_notifications_desc),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                GhostButton(
                    text = stringResource(R.string.bg_action_open),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    height = 40.dp
                )
            }
        }

        // Step 2: Battery Optimization
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.bg_step_battery),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.bg_step_battery_desc),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                GhostButton(
                    text = stringResource(R.string.bg_action_open),
                    onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    height = 40.dp
                )
            }
        }

        // Step 3: Manufacturer Specific
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.bg_step_oem_title),
                    style = DtnType.title,
                    color = DtnColors.TextPrimary
                )
                val oemText = when {
                    isXiaomi -> "Xiaomi / HyperOS steps:\n1. Settings → Apps → DTN-SOS → Battery → No restrictions\n2. Security app → Autostart → Enable DTN-SOS\n3. Recents → Long-press DTN-SOS tile → Lock in recents"
                    isNothing -> "Nothing OS steps:\n1. Settings → Battery → Battery optimisation → DTN-SOS → Don't optimise\n2. Keep screen on during active operations"
                    else -> "Android battery savers throttle BLE beacons when inactive. Add DTN-SOS to the unmonitored apps list in your device settings."
                }
                Text(
                    text = oemText,
                    style = DtnType.body,
                    color = DtnColors.TextSecondary
                )
            }
        }
    }
}

