package dtnsos.app.ui.screens.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.*
import dtnsos.app.ui.data.LocalMeshUiRepository
import dtnsos.app.ui.model.Category
import dtnsos.app.ui.model.LocationUi
import dtnsos.app.ui.model.Severity
import dtnsos.app.ui.model.SosDraft
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnType
import kotlinx.coroutines.launch

@Composable
fun ComposeSosSheet(
    onDismiss: () -> Unit,
    onSosSent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = LocalMeshUiRepository.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val locationState by repo.currentLocation().collectAsState(initial = LocationUi.Acquiring)

    var selectedCategory by remember { mutableStateOf(Category.TRAPPED) }
    var peopleCount by remember { mutableIntStateOf(1) }
    var selectedSeverity by remember { mutableStateOf(Severity.HIGH) }
    var messageText by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf("26.32") }
    var manualLon by remember { mutableStateOf("89.45") }
    var showManualLocationDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val usedBytes = remember(messageText) { Utf8Utils.byteCount(messageText) }

    val hasCoordinates = when (locationState) {
        is LocationUi.Ready, is LocationUi.Approximate -> true
        LocationUi.Acquiring, LocationUi.Unavailable -> manualLat.toDoubleOrNull() != null && manualLon.toDoubleOrNull() != null
    }

    DtnBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.compose_title),
            style = DtnType.title,
            color = DtnColors.TextPrimary
        )

        // 1. Category ChipRow
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.compose_category_label),
                style = DtnType.caption,
                color = DtnColors.TextSecondary
            )
            ChipRow(
                options = listOf(
                    ChipOption(Category.TRAPPED, "Trapped"),
                    ChipOption(Category.MEDICAL, "Medical"),
                    ChipOption(Category.FIRE, "Fire"),
                    ChipOption(Category.FLOOD, "Flood")
                ),
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )
        }

        // 2. People Stepper
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.compose_people_label),
                style = DtnType.caption,
                color = DtnColors.TextSecondary
            )
            Stepper(
                value = peopleCount,
                onValueChange = { peopleCount = it }
            )
        }

        // 3. Severity SegmentedControl
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.compose_severity_label),
                style = DtnType.caption,
                color = DtnColors.TextSecondary
            )
            SegmentedControl(
                options = listOf(
                    SegmentOption(Severity.LOW, "Low"),
                    SegmentOption(Severity.MEDIUM, "Medium"),
                    SegmentOption(Severity.HIGH, "High")
                ),
                selected = selectedSeverity,
                onSelect = { selectedSeverity = it }
            )
        }

        // 4. Message DtnTextField with ByteCounter (max 51 bytes)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DtnTextField(
                value = messageText,
                onValueChange = { input ->
                    messageText = Utf8Utils.trimOrReject(input, maxBytes = 51, currentText = messageText)
                },
                placeholder = stringResource(R.string.compose_message_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            ByteCounter(
                usedBytes = usedBytes,
                maxBytes = 51,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Location line
        LocationInfoSection(
            locationState = locationState,
            onManualClick = { showManualLocationDialog = true }
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = DtnType.caption,
                color = DtnColors.Negative
            )
        }

        Spacer(Modifier.height(8.dp))

        // 6. DangerButton "Broadcast SOS"
        DangerButton(
            text = stringResource(R.string.action_broadcast_sos),
            onClick = {
                val (lat, lon) = when (val loc = locationState) {
                    is LocationUi.Ready -> loc.lat to loc.lon
                    is LocationUi.Approximate -> loc.lat to loc.lon
                    LocationUi.Acquiring, LocationUi.Unavailable -> {
                        val parsedLat = manualLat.toDoubleOrNull() ?: 26.32
                        val parsedLon = manualLon.toDoubleOrNull() ?: 89.45
                        parsedLat to parsedLon
                    }
                }

                val draft = SosDraft(
                    category = selectedCategory,
                    people = peopleCount,
                    severity = selectedSeverity,
                    message = messageText,
                    lat = lat,
                    lon = lon
                )

                isSending = true
                scope.launch {
                    val result = repo.sendSos(draft)
                    isSending = false
                    if (result.isSuccess) {
                        onSosSent()
                        onDismiss()
                    } else {
                        errorMessage = "Couldn’t start broadcasting. Check Bluetooth and try again."
                    }
                }
            },
            enabled = hasCoordinates && !isSending,
            height = 56.dp
        )

        // 7. GhostButton "Cancel"
        GhostButton(
            text = stringResource(R.string.action_cancel),
            onClick = onDismiss
        )
    }

    if (showManualLocationDialog) {
        ManualLocationDialog(
            initialLat = manualLat,
            initialLon = manualLon,
            onDismiss = { showManualLocationDialog = false },
            onSave = { lat, lon ->
                manualLat = lat
                manualLon = lon
                showManualLocationDialog = false
            }
        )
    }
}

@Composable
private fun LocationInfoSection(
    locationState: LocationUi,
    onManualClick: () -> Unit
) {
    when (locationState) {
        is LocationUi.Acquiring -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = DtnColors.TextSecondary
                )
                Text(
                    text = stringResource(R.string.compose_location_acquiring),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
            }
        }
        is LocationUi.Ready -> {
            val coords = "${"%.2f".format(locationState.lat)}°N, ${"%.2f".format(locationState.lon)}°E"
            Text(
                text = stringResource(R.string.compose_location_ready, coords),
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
        }
        is LocationUi.Approximate -> {
            Text(
                text = stringResource(R.string.compose_location_approximate, "${locationState.ageMin} min"),
                style = DtnType.label,
                color = DtnColors.Warning
            )
        }
        is LocationUi.Unavailable -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.compose_location_unavailable),
                    style = DtnType.label,
                    color = DtnColors.TextSecondary
                )
                Text(
                    text = stringResource(R.string.action_enter_manually),
                    style = DtnType.label.copy(color = DtnColors.Cream),
                    modifier = Modifier.clickable { onManualClick() }
                )
            }
        }
    }
}

@Composable
private fun ManualLocationDialog(
    initialLat: String,
    initialLon: String,
    onDismiss: () -> Unit,
    onSave: (lat: String, lon: String) -> Unit
) {
    var latText by remember { mutableStateOf(initialLat) }
    var lonText by remember { mutableStateOf(initialLon) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DtnColors.Surface1,
        title = {
            Text(
                text = stringResource(R.string.manual_location_title),
                style = DtnType.title,
                color = DtnColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DtnTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    placeholder = stringResource(R.string.label_latitude),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                DtnTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    placeholder = stringResource(R.string.label_longitude),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (error) {
                    Text(
                        text = "Latitude must be -90..90 and Longitude -180..180",
                        style = DtnType.caption,
                        color = DtnColors.Negative
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.action_save_location),
                onClick = {
                    val latVal = latText.toDoubleOrNull()
                    val lonVal = lonText.toDoubleOrNull()
                    if (latVal != null && lonVal != null && latVal in -90.0..90.0 && lonVal in -180.0..180.0) {
                        onSave(latText, lonText)
                    } else {
                        error = true
                    }
                },
                height = 44.dp
            )
        },
        dismissButton = {
            GhostButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                height = 44.dp
            )
        }
    )
}

