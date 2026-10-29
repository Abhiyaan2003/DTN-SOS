package dtnsos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.core.BufferedPacket
import dtnsos.core.TriageLevel

@Composable
fun MuleScreen(
    heldCount: Int,
    heldPackets: List<BufferedPacket>,
    lastEvent: String,
    advStatus: String,
    scanStatus: String,
    onClearBuffer: () -> Unit
) {
    var selectedPacketForDetails by remember { mutableStateOf<BufferedPacket?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mule Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Mule Relay Node",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Clear Buffer Button
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Buffer")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Passively scans for SOS packets, adopts tokens via spray-and-wait, and rotates advertisements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Buffer Stats Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Held Packets", style = MaterialTheme.typography.labelMedium)
                    Text("$heldCount / 500", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Scanner", style = MaterialTheme.typography.labelMedium)
                    Text(scanStatus, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Advertiser", style = MaterialTheme.typography.labelMedium)
                    Text(advStatus, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Live Activity Event Line
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    lastEvent,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }

        // Section Title: Held Packets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Held SOS Packets (${heldPackets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap to inspect message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }

        if (heldPackets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No packets in buffer.\nWaiting for nearby SOS broadcasts...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(heldPackets) { item ->
                    PacketItemCard(
                        item = item,
                        onClick = { selectedPacketForDetails = item }
                    )
                }
            }
        }
    }

    // Packet Inspection Dialog
    selectedPacketForDetails?.let { item ->
        val p = item.packet
        val triage = TriageLevel.fromCode(p.triage)

        AlertDialog(
            onDismissRequest = { selectedPacketForDetails = null },
            icon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("SOS Message Inspection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("DECODED MESSAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = p.messageText().ifEmpty { "(No text content)" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Packet ID:", fontWeight = FontWeight.SemiBold)
                        Text(p.packetIdHex(), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Triage Priority:", fontWeight = FontWeight.SemiBold)
                        Text("${triage.name} (${triage.code})", fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Headcount:", fontWeight = FontWeight.SemiBold)
                        Text("${p.headCount} people")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining Tokens:", fontWeight = FontWeight.SemiBold)
                        Text("${item.tokens} tokens")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coordinates:", fontWeight = FontWeight.SemiBold)
                        Text("${"%.5f".format(p.latitudeDegrees)}, ${"%.5f".format(p.longitudeDegrees)}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TTL:", fontWeight = FontWeight.SemiBold)
                        Text("${p.ttlHours} hours")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("State:", fontWeight = FontWeight.SemiBold)
                        Text(item.state.name, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPacketForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear Buffer Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Packets?") },
            text = { Text("This will purge all held packets from the buffer, clear the duplicate cache, and reset the broadcast queue.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBuffer()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PacketItemCard(
    item: BufferedPacket,
    onClick: () -> Unit
) {
    val p = item.packet
    val triage = TriageLevel.fromCode(p.triage)

    val triageColor = when (triage) {
        TriageLevel.CRITICAL -> MaterialTheme.colorScheme.error
        TriageLevel.TRAPPED -> Color(0xFFE65100) // Deep orange
        TriageLevel.INJURED -> Color(0xFFF57C00) // Orange
        TriageLevel.SUPPLIES -> Color(0xFF388E3C) // Green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Triage badge
                Surface(
                    color = triageColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        triage.name,
                        color = triageColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    "Tokens: ${item.tokens}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(6.dp))

            // Message text preview
            Text(
                text = p.messageText().ifEmpty { "(No text message)" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ID: ${p.packetIdHex()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    "${"%.4f".format(p.latitudeDegrees)}, ${"%.4f".format(p.longitudeDegrees)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
