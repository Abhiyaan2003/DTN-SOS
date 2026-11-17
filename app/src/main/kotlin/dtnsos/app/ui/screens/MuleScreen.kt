package dtnsos.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.theme.DarkGlass
import dtnsos.app.ui.theme.DtnType
import dtnsos.core.BufferedPacket
import dtnsos.core.TriageLevel

// ─── Shape constants ──────────────────────────────────────────────────────────
private val CardShapeHero     = RoundedCornerShape(28.dp)
private val CardShapeStandard = RoundedCornerShape(20.dp)
private val PillShape         = RoundedCornerShape(999.dp)

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
    var showClearConfirm         by remember { mutableStateOf(false) }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        // ── Header (hero) card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeHero)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeHero)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint     = DarkGlass.TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text       = "Mule relay node",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = DarkGlass.TextPrimary,
                        )
                    }
                    // Clear buffer button — ghost/outline with Negative border
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        shape   = PillShape,
                        border  = androidx.compose.foundation.BorderStroke(1.dp, DarkGlass.Negative),
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = DarkGlass.Negative),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear buffer", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "Passively scans for SOS packets, adopts tokens via spray-and-wait, and rotates advertisements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkGlass.TextSecondary,
                )
            }
        }

        // ── Buffer stats card (3-column) ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeStandard)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                StatColumn(label = "Held packets", value = "$heldCount / 500")
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(DarkGlass.BorderSubtle)
                )
                StatColumn(label = "Scanner", value = scanStatus)
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(DarkGlass.BorderSubtle)
                )
                StatColumn(label = "Advertiser", value = advStatus)
            }
        }

        // ── Live event pill ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(DarkGlass.Surface2)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Info dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(PillShape)
                    .background(DarkGlass.Info)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text     = lastEvent,
                style    = MaterialTheme.typography.bodySmall,
                color    = DarkGlass.TextSecondary,
                maxLines = 1,
            )
        }

        // ── Section header ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Held SOS packets (${heldPackets.size})",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = DarkGlass.TextPrimary,
            )
            Text(
                text  = "Tap to inspect",
                style = MaterialTheme.typography.bodySmall,
                color = DarkGlass.TextTertiary,
            )
        }

        // ── Empty state / packet list ─────────────────────────────────────────
        if (heldPackets.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "No packets in buffer.\nWaiting for nearby SOS broadcasts…",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = DarkGlass.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(heldPackets) { item ->
                    PacketItemCard(
                        item    = item,
                        onClick = { selectedPacketForDetails = item },
                    )
                }
            }
        }
    }

    // ── Packet inspection dialog ──────────────────────────────────────────────
    selectedPacketForDetails?.let { item ->
        val p      = item.packet
        val triage = TriageLevel.fromCode(p.triage)

        AlertDialog(
            onDismissRequest = { selectedPacketForDetails = null },
            containerColor   = DarkGlass.Surface2,
            icon             = {
                Icon(Icons.Default.Email, contentDescription = null, tint = DarkGlass.TextPrimary)
            },
            title = {
                Text("SOS message inspection", color = DarkGlass.TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkGlass.Surface3)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text       = "Decoded message",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color      = DarkGlass.TextSecondary,
                            )
                            Text(
                                text       = p.messageText().ifEmpty { "(No text content)" },
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = DarkGlass.TextPrimary,
                            )
                        }
                    }
                    DialogRow("Packet ID:", p.packetIdHex(), mono = true)
                    DialogRow("Triage priority:", "${triage.name} (${triage.code})")
                    DialogRow("Headcount:", "${p.headCount} people")
                    DialogRow("Remaining tokens:", "${item.tokens} tokens")
                    DialogRow("Coordinates:", "${"%.5f".format(p.latitudeDegrees)}, ${"%.5f".format(p.longitudeDegrees)}")
                    DialogRow("TTL:", "${p.ttlHours} hours")
                    DialogRow("State:", item.state.name)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPacketForDetails = null }) {
                    Text("Close", color = DarkGlass.TextPrimary)
                }
            },
        )
    }

    // ── Clear buffer confirmation dialog ──────────────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor   = DarkGlass.Surface2,
            title = {
                Text("Clear all packets?", color = DarkGlass.TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    text  = "This will purge all held packets from the buffer, clear the duplicate cache, and reset the broadcast queue.",
                    color = DarkGlass.TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBuffer()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGlass.Negative,
                        contentColor   = DarkGlass.OnCream,
                    ),
                ) {
                    Text("Clear everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = DarkGlass.TextSecondary)
                }
            },
        )
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = DarkGlass.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = DarkGlass.TextPrimary,
        )
    }
}

@Composable
private fun DialogRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = DarkGlass.TextSecondary, fontSize = 13.sp)
        Text(
            text  = value,
            style = if (mono) DtnType.ids else DtnType.body,
            color = DarkGlass.TextPrimary,
        )
    }
}

@Composable
fun PacketItemCard(
    item: BufferedPacket,
    onClick: () -> Unit
) {
    val p      = item.packet
    val triage = TriageLevel.fromCode(p.triage)

    val triageColor: Color = when (triage) {
        TriageLevel.CRITICAL -> DarkGlass.Negative            // #F26D6D
        TriageLevel.TRAPPED  -> Color(0xFFE65100)             // deep orange
        TriageLevel.INJURED  -> DarkGlass.Warning             // #F5B84A
        TriageLevel.SUPPLIES -> DarkGlass.Positive            // #4ADE80
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkGlass.Surface1)
            .border(1.dp, DarkGlass.BorderSubtle, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Triage badge pill
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(triageColor.copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = triage.name,
                        color      = triageColor,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row {
                    Text("Tokens: ", style = MaterialTheme.typography.labelMedium, color = DarkGlass.TextSecondary)
                    Text("${item.tokens}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DarkGlass.TextPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text       = p.messageText().ifEmpty { "(No text message)" },
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color      = DarkGlass.TextPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = "ID: ${p.packetIdHex()}",
                    style = DtnType.ids,
                    color = DarkGlass.TextTertiary,
                )
                Text(
                    text     = "${"%.4f".format(p.latitudeDegrees)}, ${"%.4f".format(p.longitudeDegrees)}",
                    fontSize = 11.sp,
                    color    = DarkGlass.TextTertiary,
                )
            }
        }
    }
}
