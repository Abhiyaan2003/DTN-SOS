package dtnsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.ui.theme.DarkGlass

// ─── Shape constants ──────────────────────────────────────────────────────────
private val CardShapeHero     = RoundedCornerShape(28.dp)
private val CardShapeStandard = RoundedCornerShape(20.dp)

@Composable
fun GatewayScreen(
    lastEvent: String,
    advStatus: String,
    scanStatus: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Hero card ─────────────────────────────────────────────────────────
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
                        Icons.Default.Share,
                        contentDescription = null,
                        tint     = DarkGlass.Info,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "Gateway node",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color      = DarkGlass.TextPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = "Receives SOS packets over BLE, verifies authenticity, emits gateway beacons to clear mule buffers, and forwards to MQTT.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkGlass.TextSecondary,
                )
            }
        }

        // ── Status card ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeStandard)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text  = "Gateway status",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkGlass.TextSecondary,
                )
                Spacer(Modifier.height(12.dp))

                GatewayStatusRow(
                    label = "MQTT broker status:",
                    value = "Ready (TLS 8883)",
                    valueColor = DarkGlass.Info,
                )
                Spacer(Modifier.height(8.dp))
                GatewayStatusRow(
                    label = "Scanner status:",
                    value = scanStatus,
                    valueColor = if (scanStatus.contains("running", ignoreCase = true)) DarkGlass.Positive else DarkGlass.Negative,
                )
                Spacer(Modifier.height(8.dp))
                GatewayStatusRow(
                    label = "Beacon advertiser:",
                    value = advStatus,
                    valueColor = if (advStatus.contains("running", ignoreCase = true)) DarkGlass.Positive else DarkGlass.Negative,
                )
            }
        }

        // ── Live feed card ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShapeStandard)
                .background(DarkGlass.Surface1)
                .border(1.dp, DarkGlass.BorderSubtle, CardShapeStandard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = DarkGlass.Positive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Recent deliveries and beacons",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = DarkGlass.TextPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text  = lastEvent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DarkGlass.TextSecondary,
                )
            }
        }
    }
}

// ─── Helper composable ────────────────────────────────────────────────────────

@Composable
private fun GatewayStatusRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = DarkGlass.TextPrimary,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            color    = DarkGlass.TextSecondary,
            fontSize = 13.sp,
        )
        Text(
            text       = value,
            color      = valueColor,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
        )
    }
}
