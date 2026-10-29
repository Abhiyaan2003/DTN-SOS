package dtnsos.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.location.LocationBuffer
import dtnsos.core.TriageLevel

@Composable
fun VictimScreen(
    onBroadcastSos: (triage: TriageLevel, headCount: UByte, freeText: String, lat: Double, lon: Double) -> Unit,
    heldCount: Int,
    lastEvent: String
) {
    val context = LocalContext.current
    val locationBuffer = remember { LocationBuffer(context) }
    val initialCached = remember { locationBuffer.getLastKnownLocation() }

    var selectedTriage by remember { mutableStateOf(TriageLevel.CRITICAL) }
    var headCount by remember { mutableIntStateOf(1) }
    var freeText by remember { mutableStateOf("EMERGENCY HELP") }
    var latitude by remember { mutableDoubleStateOf(initialCached.latitude) }
    var longitude by remember { mutableDoubleStateOf(initialCached.longitude) }
    var locationSource by remember { mutableStateOf(initialCached.source) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Victim SOS Broadcast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Broadcasts connectionless BLE packets without cellular towers or internet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Last Known Location Buffer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Last Known Location Buffer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Active fallback: $locationSource\nCoordinates: ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Triage Level Selection
        Text("Triage Priority", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TriageLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedTriage == level,
                    onClick = { selectedTriage = level },
                    label = { Text(level.name) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Headcount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("People in need: $headCount", style = MaterialTheme.typography.titleMedium)
            Row {
                FilledTonalButton(
                    onClick = { if (headCount > 1) headCount-- },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("-", fontSize = 20.sp) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { if (headCount < 50) headCount++ },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) { Text("+", fontSize = 20.sp) }
            }
        }

        // Message
        OutlinedTextField(
            value = freeText,
            onValueChange = { if (it.length <= 25) freeText = it },
            label = { Text("Message (max 25 chars)") },
            supportingText = { Text("${freeText.length} / 25") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Coordinates Input + Buffer Update
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latitude.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { v ->
                        latitude = v
                        locationSource = "User Input / Manual Fix"
                        locationBuffer.saveLocation(latitude, longitude, locationSource)
                    }
                },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = longitude.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { v ->
                        longitude = v
                        locationSource = "User Input / Manual Fix"
                        locationBuffer.saveLocation(latitude, longitude, locationSource)
                    }
                },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f)
            )
        }

        // Presets quick-bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    latitude = 22.5726
                    longitude = 88.3639
                    locationSource = "Disaster Zone (Kolkata Preset)"
                    locationBuffer.saveLocation(latitude, longitude, locationSource)
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Kolkata Preset", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    latitude = 28.6139
                    longitude = 77.2090
                    locationSource = "Disaster Zone (Delhi Preset)"
                    locationBuffer.saveLocation(latitude, longitude, locationSource)
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Delhi Preset", fontSize = 12.sp)
            }
        }

        // BIG RED SOS BUTTON
        Button(
            onClick = {
                // Ensure current location is saved in buffer before broadcasting
                locationBuffer.saveLocation(latitude, longitude, locationSource)
                onBroadcastSos(selectedTriage, headCount.toUByte(), freeText, latitude, longitude)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text("BROADCAST EMERGENCY SOS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Broadcast Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Packets in local buffer: $heldCount")
                Text("Last event: $lastEvent")
            }
        }
    }
}
