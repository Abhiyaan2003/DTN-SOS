package dtnsos.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dtnsos.app.ble.BleCapabilities
import dtnsos.app.ble.BleMode
import dtnsos.app.ui.DeviceGuidance
import dtnsos.app.ui.LocationSource
import dtnsos.app.ui.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onProceed: (BleMode) -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // ─── Permission launcher ──────────────────────────────────────────────────
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionsResult(results.values.all { it })
    }

    LaunchedEffect(Unit) {
        viewModel.loadCapabilities()
        permLauncher.launch(permissions.toTypedArray())
    }

    // ─── Manual location state ────────────────────────────────────────────────
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text = "DTN-SOS Setup",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Text(
            text = "Connectionless BLE delay-tolerant network",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // ── Mode Banner ───────────────────────────────────────────────────────
        ModeBanner(mode = uiState.mode)

        // ── Capability Table ──────────────────────────────────────────────────
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            uiState.capabilities?.let { caps ->
                CapabilityTable(caps = caps)
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // ── Location Input ────────────────────────────────────────────────────
        Text(
            text = "Location",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = "GPS is unreliable indoors. Enter preset or manual coordinates for the demo.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        // Preset buttons (fake coordinates — NFR-4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.setPresetLocation(22.5726, 88.3639) }) {
                Text("Kolkata (preset)", fontSize = 12.sp)
            }
            OutlinedButton(onClick = { viewModel.setPresetLocation(28.6139, 77.2090) }) {
                Text("Delhi (preset)", fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        OutlinedButton(onClick = {
            val lat = latText.toDoubleOrNull()
            val lon = lonText.toDoubleOrNull()
            if (lat != null && lon != null) viewModel.setManualLocation(lat, lon)
        }) { Text("Set manual location") }

        // Location status chip
        uiState.locationInput.lat?.let { lat ->
            val lon = uiState.locationInput.lon!!
            val src = uiState.locationInput.source.name
            SuggestionChip(
                onClick = {},
                label = { Text("$src: %.5f, %.5f".format(lat, lon), fontSize = 11.sp) },
                icon  = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        // ── Device guidance ───────────────────────────────────────────────────
        when (uiState.deviceGuidance) {
            DeviceGuidance.XIAOMI  -> XiaomiGuidanceCard()
            DeviceGuidance.NOTHING -> NothingGuidanceCard()
            DeviceGuidance.NONE    -> Unit
        }

        // ── Permissions status ────────────────────────────────────────────────
        if (!uiState.permissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape  = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Some permissions were denied. BLE will not work.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Proceed button ────────────────────────────────────────────────────
        Button(
            onClick = { onProceed(uiState.mode) },
            enabled = !uiState.isLoading && uiState.permissionsGranted && uiState.locationInput.lat != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Proceed to Role Selection")
        }
    }
}

// ─── Mode Banner ──────────────────────────────────────────────────────────────

@Composable
private fun ModeBanner(mode: BleMode) {
    val (label, color, icon) = when (mode) {
        BleMode.A_FULL    -> Triple("Mode A — Full Extended Advertising",  Color(0xFF1B5E20), Icons.Default.Bluetooth)
        BleMode.B_ONE_WAY -> Triple("Mode B — One-Way Extended Advertising", Color(0xFFE65100), Icons.Default.BluetoothSearching)
        BleMode.C_LEGACY  -> Triple("Mode C — Legacy Fragmented (stretch)", Color(0xFFB71C1C), Icons.Default.BluetoothDisabled)
        BleMode.UNKNOWN   -> Triple("Determining mode…", Color(0xFF37474F), Icons.Default.HourglassEmpty)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(label, style = MaterialTheme.typography.titleSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
        }
    }
}

// ─── Capability Table ─────────────────────────────────────────────────────────

@Composable
private fun CapabilityTable(caps: BleCapabilities) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "BLE Capabilities",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            CapRow("Device",             caps.deviceModel)
            CapRow("Android API",        caps.androidVersion.toString())
            CapBoolRow("Extended advertising", caps.isExtendedAdvertisingSupported)
            CapRow("Max adv data length", "${caps.maxAdvertisingDataLength} bytes (need ≥ 154)")
            CapBoolRow("2M PHY",          caps.is2MPhySupported)
            CapBoolRow("Coded PHY",       caps.isCodedPhySupported)
            CapBoolRow("Periodic adv",    caps.isPeriodicAdvertisingSupported)
        }
    }
}

@Composable
private fun CapRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(
            color      = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace,
        ))
    }
}

@Composable
private fun CapBoolRow(label: String, value: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface))
        Icon(
            imageVector = if (value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (value) "Supported" else "Not supported",
            tint = if (value) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─── Device Guidance Cards ────────────────────────────────────────────────────

@Composable
private fun XiaomiGuidanceCard() {
    var expanded by remember { mutableStateOf(false) }
    GuidanceCard(
        title    = "Xiaomi / Redmi (HyperOS) guidance",
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        GuidanceStep("1. Settings → Apps → DTN-SOS → Battery → No restrictions")
        GuidanceStep("2. Security app → Battery → Power Saver → DTN-SOS → No restrictions")
        GuidanceStep("3. Recents → long-press DTN-SOS tile → Lock in recents")
        GuidanceStep("4. Security app → Autostart → enable DTN-SOS")
        Text(
            "Keep screen on during the demo. Background BLE scanning is unreliable on HyperOS.",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun NothingGuidanceCard() {
    var expanded by remember { mutableStateOf(false) }
    GuidanceCard(
        title    = "Nothing OS guidance",
        expanded = expanded,
        onToggle = { expanded = !expanded },
    ) {
        GuidanceStep("1. Settings → Battery → Battery optimisation → DTN-SOS → Don't optimise")
        GuidanceStep("2. Keep screen on during the demo.")
    }
}

@Composable
private fun GuidanceCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun GuidanceStep(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
    )
}
