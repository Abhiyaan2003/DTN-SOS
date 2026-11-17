package dtnsos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.service.ServiceUiState
import dtnsos.app.ui.theme.DarkGlass
import dtnsos.core.NodeRole
import dtnsos.core.TriageLevel

// ─── Nav pill shape constant ──────────────────────────────────────────────────
private val PillShape = RoundedCornerShape(999.dp)

enum class AppTab(val title: String) {
    VICTIM("Victim"),
    MULE("Mule"),
    GATEWAY("Gateway"),
    SETUP("Setup")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    serviceState: ServiceUiState,
    onRoleChanged: (NodeRole) -> Unit,
    onBroadcastSos: (TriageLevel, UByte, String, Double, Double) -> Unit,
    onClearBuffer: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.VICTIM) }

    Scaffold(
        containerColor = DarkGlass.BgBase,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DTN-SOS — ${serviceState.role.name}",
                        color = DarkGlass.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    Button(
                        onClick = {
                            if (serviceState.isRunning) onStopService() else onStartService()
                        },
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkGlass.Cream,
                            contentColor   = DarkGlass.OnCream,
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text       = if (serviceState.isRunning) "Stop" else "Start",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            // Floating glass nav pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(PillShape)
                        .background(Color(0x8C1C1C1F))
                        .border(1.dp, DarkGlass.BorderStrong, PillShape),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    NavPillItem(
                        icon      = Icons.Default.Warning,
                        label     = "Victim",
                        isActive  = selectedTab == AppTab.VICTIM,
                        onClick   = {
                            selectedTab = AppTab.VICTIM
                            onRoleChanged(NodeRole.VICTIM)
                        },
                    )
                    NavPillItem(
                        icon      = Icons.Default.Refresh,
                        label     = "Mule",
                        isActive  = selectedTab == AppTab.MULE,
                        onClick   = {
                            selectedTab = AppTab.MULE
                            onRoleChanged(NodeRole.MULE)
                        },
                    )
                    NavPillItem(
                        icon      = Icons.Default.Share,
                        label     = "Gateway",
                        isActive  = selectedTab == AppTab.GATEWAY,
                        onClick   = {
                            selectedTab = AppTab.GATEWAY
                            onRoleChanged(NodeRole.GATEWAY)
                        },
                    )
                    NavPillItem(
                        icon     = Icons.Default.Settings,
                        label    = "Setup",
                        isActive = selectedTab == AppTab.SETUP,
                        onClick  = { selectedTab = AppTab.SETUP },
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                AppTab.VICTIM  -> VictimScreen(
                    onBroadcastSos = onBroadcastSos,
                    heldCount      = serviceState.heldPacketsCount,
                    lastEvent      = serviceState.lastEventMessage,
                )
                AppTab.MULE    -> MuleScreen(
                    heldCount    = serviceState.heldPacketsCount,
                    heldPackets  = serviceState.heldPackets,
                    lastEvent    = serviceState.lastEventMessage,
                    advStatus    = serviceState.advStatus,
                    scanStatus   = serviceState.scanStatus,
                    onClearBuffer = onClearBuffer,
                )
                AppTab.GATEWAY -> GatewayScreen(
                    lastEvent  = serviceState.lastEventMessage,
                    advStatus  = serviceState.advStatus,
                    scanStatus = serviceState.scanStatus,
                )
                AppTab.SETUP   -> SetupScreen(
                    onProceed = { _ ->
                        selectedTab = AppTab.MULE
                    }
                )
            }
        }
    }
}

// ─── Single nav pill item ─────────────────────────────────────────────────────

@Composable
private fun RowScope.NavPillItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val iconColor  = if (isActive) DarkGlass.TextPrimary else DarkGlass.TextSecondary
    val labelColor = if (isActive) DarkGlass.TextPrimary else DarkGlass.TextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(PillShape)
            .then(
                if (isActive) Modifier.background(DarkGlass.Surface3) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = iconColor,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text      = label,
            color     = labelColor,
            fontSize  = 11.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
