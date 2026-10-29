package dtnsos.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dtnsos.app.service.ServiceUiState
import dtnsos.core.NodeRole
import dtnsos.core.TriageLevel

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
        topBar = {
            TopAppBar(
                title = { Text("DTN-SOS — ${serviceState.role.name}") },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            if (serviceState.isRunning) onStopService() else onStartService()
                        }
                    ) {
                        Text(if (serviceState.isRunning) "STOP" else "START")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.VICTIM,
                    onClick = {
                        selectedTab = AppTab.VICTIM
                        onRoleChanged(NodeRole.VICTIM)
                    },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                    label = { Text("Victim") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.MULE,
                    onClick = {
                        selectedTab = AppTab.MULE
                        onRoleChanged(NodeRole.MULE)
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("Mule") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.GATEWAY,
                    onClick = {
                        selectedTab = AppTab.GATEWAY
                        onRoleChanged(NodeRole.GATEWAY)
                    },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    label = { Text("Gateway") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETUP,
                    onClick = { selectedTab = AppTab.SETUP },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Setup") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                AppTab.VICTIM -> VictimScreen(
                    onBroadcastSos = onBroadcastSos,
                    heldCount = serviceState.heldPacketsCount,
                    lastEvent = serviceState.lastEventMessage
                )
                AppTab.MULE -> MuleScreen(
                    heldCount = serviceState.heldPacketsCount,
                    heldPackets = serviceState.heldPackets,
                    lastEvent = serviceState.lastEventMessage,
                    advStatus = serviceState.advStatus,
                    scanStatus = serviceState.scanStatus,
                    onClearBuffer = onClearBuffer
                )
                AppTab.GATEWAY -> GatewayScreen(
                    lastEvent = serviceState.lastEventMessage,
                    advStatus = serviceState.advStatus,
                    scanStatus = serviceState.scanStatus
                )
                AppTab.SETUP -> SetupScreen(
                    onProceed = { mode ->
                        selectedTab = AppTab.MULE
                    }
                )
            }
        }
    }
}
