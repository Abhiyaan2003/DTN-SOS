package dtnsos.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import dtnsos.app.ui.theme.DarkGlass

// ─── Shape constants ──────────────────────────────────────────────────────────
private val CardShapeStandard = RoundedCornerShape(20.dp)
private val PillShape         = RoundedCornerShape(999.dp)

// ─── Shared text field colors ─────────────────────────────────────────────────
@Composable
private fun setupTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = DarkGlass.Accent,
    unfocusedBorderColor    = DarkGlass.BorderSubtle,
    cursorColor             = DarkGlass.Accent,
    focusedLabelColor       = DarkGlass.TextSecondary,
    unfocusedLabelColor     = DarkGlass.TextTertiary,
    focusedTextColor        = DarkGlass.TextPrimary,
    unfocusedTextColor      = DarkGlass.TextPrimary,
    focusedContainerColor   = DarkGlass.Surface2,
    unfocusedContainerColor = DarkGlass.Surface2,
)

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

    val tfColors = setupTextFieldColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkGlass.BgBase)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text       = "DTN-SOS setup",
            style      = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color      = DarkGlass.TextPrimary,
                fontSize   = 28.sp,
            ),
        )
        Text(
            text  = "Connectionless BLE delay-tolerant network",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = DarkGlass.TextSecondary,
            ),
        )

        HorizontalDivider(color = DarkGlass.BorderSubtle)

        // ── Mode Banner ───────────────────────────────────────────────────────
        ModeBanner(mode = uiState.mode)

        // ── Capability Table ──────────────────────────────────────────────────
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color    = DarkGlass.Accent,
            )
        } else {
            uiState.capabilities?.let { caps ->
                CapabilityTable(caps = caps)
            }
        }

        HorizontalDivider(color = DarkGlass.BorderSubtle)

        // ── Location Input ────────────────────────────────────────────────────
        Text(
            text  = "Location",
            style = MaterialTheme.typography.titleMedium.copy(
                color      = DarkGlass.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text  = "GPS is unreliable indoors. Enter preset or manual coordinates for the demo.",
            style = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.TextSecondary),
        )

        // Preset buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.setPresetLocation(22.5726, 88.3639) },
                shape   = PillShape,
                border  = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.BorderStrong),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.TextSecondary),
            ) {
                Text("Kolkata (preset)", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.setPresetLocation(28.6139, 77.2090) },
                shape   = PillShape,
                border  = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.BorderStrong),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.TextSecondary),
            ) {
                Text("Delhi (preset)", fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value          = latText,
                onValueChange  = { latText = it },
                label          = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier       = Modifier.weight(1f),
                singleLine     = true,
                colors         = tfColors,
            )
            OutlinedTextField(
                value          = lonText,
                onValueChange  = { lonText = it },
                label          = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier       = Modifier.weight(1f),
                singleLine     = true,
                colors         = tfColors,
            )
        }

        OutlinedButton(
            onClick = {
                val lat = latText.toDoubleOrNull()
                val lon = lonText.toDoubleOrNull()
                if (lat != null && lon != null) viewModel.setManualLocation(lat, lon)
            },
            shape  = PillShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.BorderStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.TextSecondary),
        ) { Text("Set manual location") }

        // Location status chip
        uiState.locationInput.lat?.let { lat ->
            val lon = uiState.locationInput.lon!!
            val src = uiState.locationInput.source.name
            SuggestionChip(
                onClick = {},
                label   = { Text("$src: %.5f, %.5f".format(lat, lon), fontSize = 11.sp) },
                icon    = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape  = PillShape,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = DarkGlass.Surface2,
                    labelColor     = DarkGlass.TextSecondary,
                    iconContentColor = DarkGlass.TextTertiary,
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled             = true,
                    borderColor         = DarkGlass.BorderSubtle,
                    disabledBorderColor = DarkGlass.BorderSubtle,
                ),
            )
        }

        HorizontalDivider(color = DarkGlass.BorderSubtle)

        // ── Device guidance ───────────────────────────────────────────────────
        when (uiState.deviceGuidance) {
            DeviceGuidance.XIAOMI  -> XiaomiGuidanceCard()
            DeviceGuidance.NOTHING -> NothingGuidanceCard()
            DeviceGuidance.NONE    -> Unit
        }

        // ── Permissions warning ───────────────────────────────────────────────
        if (!uiState.permissionsGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkGlass.AccentDeep)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = DarkGlass.Negative,
                    )
                    Text(
                        text  = "Some permissions were denied. BLE will not work.",
                        style = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.Negative),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Proceed button ────────────────────────────────────────────────────
        Button(
            onClick  = { onProceed(uiState.mode) },
            enabled  = !uiState.isLoading && uiState.permissionsGranted && uiState.locationInput.lat != null,
            modifier = Modifier.fillMaxWidth(),
            shape    = PillShape,
            colors   = ButtonDefaults.buttonColors(
                containerColor         = DarkGlass.Cream,
                contentColor           = DarkGlass.OnCream,
                disabledContainerColor = DarkGlass.Surface3,
                disabledContentColor   = DarkGlass.TextTertiary,
            ),
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Proceed to role selection", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Mode Banner ──────────────────────────────────────────────────────────────

@Composable
private fun ModeBanner(mode: BleMode) {
    val (label, modeColor, icon) = when (mode) {
        BleMode.A_FULL    -> Triple("Mode A — Full extended advertising",   DarkGlass.Positive, Icons.Default.Bluetooth)
        BleMode.B_ONE_WAY -> Triple("Mode B — One-way extended advertising", DarkGlass.Warning,  Icons.Default.BluetoothSearching)
        BleMode.C_LEGACY  -> Triple("Mode C — Legacy fragmented (stretch)", DarkGlass.Negative,  Icons.Default.BluetoothDisabled)
        BleMode.UNKNOWN   -> Triple("Determining mode…",                    DarkGlass.TextTertiary, Icons.Default.HourglassEmpty)
    }

    // Surface-1 card with 4dp left accent border
    val modeColorValue = modeColor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkGlass.Surface1)
            .border(1.dp, DarkGlass.BorderSubtle, RoundedCornerShape(12.dp))
            .drawBehind {
                // Draw 4dp left accent strip
                drawRect(
                    color  = modeColorValue,
                    topLeft = Offset.Zero,
                    size   = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                )
            }
    ) {
        Row(
            modifier              = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = modeColor, modifier = Modifier.size(20.dp))
            Text(
                text  = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    color      = DarkGlass.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

// ─── Capability Table ─────────────────────────────────────────────────────────

@Composable
private fun CapabilityTable(caps: BleCapabilities) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShapeStandard)
            .background(DarkGlass.Surface2)
            .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text  = "BLE capabilities",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color      = DarkGlass.TextSecondary,
                ),
            )
            CapRow("Device",               caps.deviceModel)
            CapRow("Android API",          caps.androidVersion.toString())
            CapBoolRow("Extended advertising", caps.isExtendedAdvertisingSupported)
            CapRow("Max adv data length",  "${caps.maxAdvertisingDataLength} bytes (need ≥ 154)")
            CapBoolRow("2M PHY",           caps.is2MPhySupported)
            CapBoolRow("Coded PHY",        caps.isCodedPhySupported)
            CapBoolRow("Periodic adv",     caps.isPeriodicAdvertisingSupported)
        }
    }
}

@Composable
private fun CapRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.TextSecondary))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                color      = DarkGlass.TextPrimary,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

@Composable
private fun CapBoolRow(label: String, value: Boolean) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.TextSecondary))
        Icon(
            imageVector        = if (value) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (value) "Supported" else "Not supported",
            tint               = if (value) DarkGlass.Positive else DarkGlass.Negative,
            modifier           = Modifier.size(18.dp),
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
            text     = "Keep screen on during the demo. Background BLE scanning is unreliable on HyperOS.",
            style    = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.Warning),
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShapeStandard)
            .background(DarkGlass.Surface2)
            .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint     = DarkGlass.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color      = DarkGlass.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = DarkGlass.TextSecondary,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically(),
            ) {
                Column(
                    modifier            = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
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
        style = MaterialTheme.typography.bodySmall.copy(color = DarkGlass.TextSecondary),
    )
}
