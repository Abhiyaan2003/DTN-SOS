package dtnsos.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.components.Card
import dtnsos.app.ui.components.GhostButton
import dtnsos.app.ui.components.PrimaryButton
import dtnsos.app.ui.data.FakeMeshRepository
import dtnsos.app.ui.data.LocalMeshUiRepository
import dtnsos.app.ui.model.*
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType
import kotlinx.coroutines.launch

@Composable
fun StateGalleryScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = LocalMeshUiRepository.current as? FakeMeshRepository ?: return
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtnColors.BgBase)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = DtnSpacing.gutter),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = DtnSpacing.maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DtnColors.TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Debug State Gallery",
                    style = DtnType.headline,
                    color = DtnColors.TextPrimary
                )
            }

            Text(
                text = "Tap any state to force FakeMeshRepository to that configuration for testing.",
                style = DtnType.body,
                color = DtnColors.TextSecondary
            )

            // Section 1: Core Navigation & Interrupts
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Interrupts & Hardware", style = DtnType.title, color = DtnColors.TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "BT Off",
                            onClick = { repo.setDebugBluetooth(BtState.OFF); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "BT On",
                            onClick = { repo.setDebugBluetooth(BtState.ON); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Unsupported",
                            onClick = { repo.setDebugCapability(Capability.UNSUPPORTED); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Full Capable",
                            onClick = { repo.setDebugCapability(Capability.FULL); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Low Battery (14%)",
                            onClick = { repo.setDebugBattery(14); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Normal Batt (85%)",
                            onClick = { repo.setDebugBattery(85); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }
                }
            }

            // Section 2: Home States
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Home States", style = DtnType.title, color = DtnColors.TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Mule (Listening)",
                            onClick = {
                                scope.launch {
                                    repo.setRole(Role.MULE)
                                    repo.setListening(true)
                                    repo.setDebugActiveSos(false)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Mule (Paused)",
                            onClick = {
                                scope.launch {
                                    repo.setRole(Role.MULE)
                                    repo.setListening(false)
                                    repo.setDebugActiveSos(false)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Carry-Only",
                            onClick = {
                                scope.launch {
                                    repo.setCarryOnly()
                                    repo.setDebugActiveSos(false)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "SOS Active",
                            onClick = {
                                repo.setDebugActiveSos(true)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Gateway (Online)",
                            onClick = {
                                scope.launch {
                                    repo.setRole(Role.GATEWAY)
                                    repo.setDebugMqtt(MqttState.CONNECTED)
                                    repo.setDebugActiveSos(false)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Gateway (Offline)",
                            onClick = {
                                scope.launch {
                                    repo.setRole(Role.GATEWAY)
                                    repo.setDebugMqtt(MqttState.OFFLINE)
                                    repo.setDebugActiveSos(false)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }
                }
            }

            // Section 3: Carrying List States
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Carrying List", style = DtnType.title, color = DtnColors.TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Filled (4 Packets)",
                            onClick = { repo.setDebugEmptyPackets(false); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Empty State",
                            onClick = { repo.setDebugEmptyPackets(true); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }
                }
            }

            // Section 4: Location States (for Compose SOS)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Compose Location States", style = DtnType.title, color = DtnColors.TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Acquiring",
                            onClick = { repo.setDebugLocationState(LocationUi.Acquiring); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Ready (26.32, 89.45)",
                            onClick = { repo.setDebugLocationState(LocationUi.Ready(26.32, 89.45)); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Approx (12 min ago)",
                            onClick = { repo.setDebugLocationState(LocationUi.Approximate(26.32, 89.45, 12)); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Unavailable",
                            onClick = { repo.setDebugLocationState(LocationUi.Unavailable); onDismiss() },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }
                }
            }

            // Section 5: Onboarding
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Onboarding Steps", style = DtnType.title, color = DtnColors.TextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Step 0 (Welcome)",
                            onClick = {
                                repo.setDebugOnboardingComplete(false)
                                repo.setDebugOnboardingStep(0)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Step 1 (Perms)",
                            onClick = {
                                repo.setDebugOnboardingComplete(false)
                                repo.setDebugOnboardingStep(1)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            text = "Step 2 (Profile)",
                            onClick = {
                                repo.setDebugOnboardingComplete(false)
                                repo.setDebugOnboardingStep(2)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                        GhostButton(
                            text = "Step 3 (Ready)",
                            onClick = {
                                repo.setDebugOnboardingComplete(false)
                                repo.setDebugOnboardingStep(3)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            height = 40.dp
                        )
                    }

                    GhostButton(
                        text = "Complete Onboarding",
                        onClick = { repo.setDebugOnboardingComplete(true); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        height = 40.dp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            PrimaryButton(
                text = "Close Gallery",
                onClick = onDismiss
            )
        }
    }
}

