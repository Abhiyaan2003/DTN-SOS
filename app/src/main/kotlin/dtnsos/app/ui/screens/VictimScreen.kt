package dtnsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.location.LocationBuffer
import dtnsos.app.ui.theme.DarkGlass
import dtnsos.core.TriageLevel

// ─── Shape constants ──────────────────────────────────────────────────────────
private val CardShapeHero     = RoundedCornerShape(28.dp)
private val CardShapeStandard = RoundedCornerShape(20.dp)
private val PillShape         = RoundedCornerShape(999.dp)

@Composable
fun VictimScreen(
    onBroadcastSos: (triage: TriageLevel, headCount: UByte, freeText: String, lat: Double, lon: Double) -> Unit,
    heldCount: Int,
    lastEvent: String
) {
    val context         = LocalContext.current
    val locationBuffer  = remember { LocationBuffer(context) }
    val initialCached   = remember { locationBuffer.getLastKnownLocation() }

    var selectedTriage  by remember { mutableStateOf(TriageLevel.CRITICAL) }
    var headCount       by remember { mutableIntStateOf(1) }
    var freeText        by remember { mutableStateOf("EMERGENCY HELP") }
    var latitude        by remember { mutableDoubleStateOf(initialCached.latitude) }
    var longitude       by remember { mutableDoubleStateOf(initialCached.longitude) }
    var locationSource  by remember { mutableStateOf(initialCached.source) }

    // TextField color preset reused on all fields
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = DarkGlass.Accent,
        unfocusedBorderColor = DarkGlass.BorderSubtle,
        cursorColor          = DarkGlass.Accent,
        focusedLabelColor    = DarkGlass.TextSecondary,
        unfocusedLabelColor  = DarkGlass.TextTertiary,
        focusedTextColor     = DarkGlass.TextPrimary,
        unfocusedTextColor   = DarkGlass.TextPrimary,
        focusedContainerColor   = DarkGlass.Surface2,
        unfocusedContainerColor = DarkGlass.Surface2,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Hero SOS card ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeHero)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeHero)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = DarkGlass.Accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "Victim SOS broadcast",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = DarkGlass.TextPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "Broadcasts connectionless BLE packets without cellular towers or internet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkGlass.TextSecondary,
                )
            }
        }

        // ── Location buffer card ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeStandard)
                .background(DarkGlass.Surface2)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint     = DarkGlass.Info,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = "Last known location buffer",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = DarkGlass.TextPrimary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "Active fallback: $locationSource",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkGlass.TextSecondary,
                )
                Text(
                    text  = "Coordinates: ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkGlass.TextSecondary,
                )
            }
        }

        // ── Triage selector ───────────────────────────────────────────────────
        Text(
            text       = "Triage priority",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = DarkGlass.TextPrimary,
            modifier   = Modifier.align(Alignment.Start),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(DarkGlass.Surface2)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TriageLevel.entries.forEach { level ->
                val isSelected = selectedTriage == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(if (isSelected) DarkGlass.Cream else Color.Transparent)
                        .clickable { selectedTriage = level }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = level.name,
                        color      = if (isSelected) DarkGlass.OnCream else DarkGlass.TextSecondary,
                        fontSize   = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        // ── Headcount ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(DarkGlass.Surface2)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "People in need: $headCount",
                style = MaterialTheme.typography.titleMedium,
                color = DarkGlass.TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Minus button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(PillShape)
                        .background(DarkGlass.Surface3)
                        .clickable { if (headCount > 1) headCount-- },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "−", color = DarkGlass.TextPrimary, fontSize = 20.sp)
                }
                // Plus button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(PillShape)
                        .background(DarkGlass.Surface3)
                        .clickable { if (headCount < 50) headCount++ },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "+", color = DarkGlass.TextPrimary, fontSize = 20.sp)
                }
            }
        }

        // ── Message field ─────────────────────────────────────────────────────
        OutlinedTextField(
            value         = freeText,
            onValueChange = { if (it.length <= 25) freeText = it },
            label         = { Text("Message (max 25 chars)") },
            supportingText = { Text("${freeText.length} / 25", color = DarkGlass.TextTertiary) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            colors        = textFieldColors,
        )

        // ── Coordinate fields ─────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value         = latitude.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { v ->
                        latitude       = v
                        locationSource = "User Input / Manual Fix"
                        locationBuffer.saveLocation(latitude, longitude, locationSource)
                    }
                },
                label    = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                colors   = textFieldColors,
            )
            OutlinedTextField(
                value         = longitude.toString(),
                onValueChange = {
                    it.toDoubleOrNull()?.let { v ->
                        longitude      = v
                        locationSource = "User Input / Manual Fix"
                        locationBuffer.saveLocation(latitude, longitude, locationSource)
                    }
                },
                label    = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                colors   = textFieldColors,
            )
        }

        // ── Preset buttons ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    latitude       = 22.5726
                    longitude      = 88.3639
                    locationSource = "Disaster zone (Kolkata preset)"
                    locationBuffer.saveLocation(latitude, longitude, locationSource)
                },
                modifier     = Modifier.weight(1f),
                shape        = PillShape,
                border       = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.BorderStrong),
                colors       = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.TextSecondary),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Text("Kolkata preset", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    latitude       = 28.6139
                    longitude      = 77.2090
                    locationSource = "Disaster zone (Delhi preset)"
                    locationBuffer.saveLocation(latitude, longitude, locationSource)
                },
                modifier     = Modifier.weight(1f),
                shape        = PillShape,
                border       = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.BorderStrong),
                colors       = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.TextSecondary),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Text("Delhi preset", fontSize = 12.sp)
            }
        }

        // ── SOS broadcast button ──────────────────────────────────────────────
        Button(
            onClick = {
                locationBuffer.saveLocation(latitude, longitude, locationSource)
                onBroadcastSos(selectedTriage, headCount.toUByte(), freeText, latitude, longitude)
            },
            colors  = ButtonDefaults.buttonColors(containerColor = DarkGlass.Accent),
            shape   = CardShapeHero,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Text(
                text       = "Broadcast emergency SOS",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = DarkGlass.TextPrimary,
            )
        }

        // ── Broadcast status card ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeStandard)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text  = "Broadcast status",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkGlass.TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Packets in buffer:", color = DarkGlass.TextSecondary, fontSize = 13.sp)
                    Text("$heldCount", color = DarkGlass.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Last event:", color = DarkGlass.TextSecondary, fontSize = 13.sp)
                    Text(lastEvent, color = DarkGlass.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
