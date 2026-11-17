package dtnsos.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dtnsos.app.ui.components.GlassNavBar
import dtnsos.app.ui.components.NavTab
import dtnsos.app.ui.data.LocalMeshUiRepository
import dtnsos.app.ui.debug.StateGalleryScreen
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.model.*
import dtnsos.app.ui.screens.carrying.CarryingScreen
import dtnsos.app.ui.screens.carrying.PacketDetailSheet
import dtnsos.app.ui.screens.compose.ComposeSosSheet
import dtnsos.app.ui.screens.home.HomeScreen
import dtnsos.app.ui.screens.interrupts.*
import dtnsos.app.ui.screens.onboarding.OnboardingScreen
import dtnsos.app.ui.screens.settings.BackgroundHelpScreen
import dtnsos.app.ui.screens.settings.GatewaySetupScreen
import dtnsos.app.ui.screens.settings.SettingsScreen
import dtnsos.app.ui.theme.DtnColors
import kotlinx.coroutines.launch

@Composable
fun AppGate(
    modifier: Modifier = Modifier
) {
    val repo = LocalMeshUiRepository.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()

    val meshState by repo.mesh.collectAsState()
    val packets by repo.packets.collectAsState()
    val settingsState by repo.settings.collectAsState()
    val onboardingComplete by repo.onboardingComplete.collectAsState()

    // Collect background haptic events
    LaunchedEffect(Unit) {
        repo.hapticEvents.collect { event ->
            haptics.play(event)
        }
    }

    // Low battery trigger state
    var lowBatteryTriggeredThisEpisode by remember { mutableStateOf(false) }

    LaunchedEffect(meshState.batteryPercent, meshState.isCharging) {
        if (meshState.isCharging || meshState.batteryPercent >= 20) {
            lowBatteryTriggeredThisEpisode = false
        }
    }

    // Active navigation and overlay sheets
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
    var activeSheet by remember { mutableStateOf<ActiveSheet>(ActiveSheet.None) }
    var inspectingPacket by remember { mutableStateOf<PacketUi?>(null) }

    // If bluetooth turns off, close open sheets per §6.1
    LaunchedEffect(meshState.bluetooth) {
        if (meshState.bluetooth == BtState.OFF) {
            activeSheet = ActiveSheet.None
            inspectingPacket = null
        }
    }

    // Trigger low-battery sheet if applicable
    LaunchedEffect(meshState.batteryPercent, meshState.isCharging, onboardingComplete, activeSheet) {
        if (onboardingComplete &&
            meshState.batteryPercent <= 15 &&
            !meshState.isCharging &&
            !lowBatteryTriggeredThisEpisode &&
            activeSheet != ActiveSheet.ComposeSos
        ) {
            lowBatteryTriggeredThisEpisode = true
            activeSheet = ActiveSheet.LowBattery
        }
    }

    // ─── GATE RULES ──────────────────────────────────────────────────────────
    // 1. Onboarding not complete
    if (!onboardingComplete) {
        OnboardingScreen(onComplete = {
            scope.launch { repo.completeOnboarding() }
        })
        return
    }

    // 2. Unsupported phone (and not carry-only)
    if (meshState.capability == Capability.UNSUPPORTED) {
        UnsupportedPhoneScreen(
            onContinueCarryOnly = {
                scope.launch { repo.setCarryOnly() }
            },
            onSeeSupportedPhones = {
                activeSheet = ActiveSheet.SupportedPhones
            }
        )

        if (activeSheet == ActiveSheet.SupportedPhones) {
            SupportedPhonesSheet(onDismiss = { activeSheet = ActiveSheet.None })
        }
        return
    }

    // 3. Bluetooth is OFF
    if (meshState.bluetooth == BtState.OFF) {
        BluetoothOffScreen(
            onTurnOnBluetooth = {
                scope.launch { repo.requestEnableBluetooth() }
            }
        )
        return
    }

    // ─── MAIN SCREENS (Tabs & SubScreens) ────────────────────────────────────

    // Back handling
    BackHandler(enabled = currentSubScreen != SubScreen.None || currentTab != NavTab.HOME) {
        if (currentSubScreen != SubScreen.None) {
            currentSubScreen = SubScreen.None
        } else if (currentTab != NavTab.HOME) {
            currentTab = NavTab.HOME
        }
    }

    when (currentSubScreen) {
        SubScreen.GatewaySetup -> {
            GatewaySetupScreen(
                onSaveAndBecomeGateway = {
                    scope.launch {
                        repo.setRole(Role.GATEWAY)
                        currentSubScreen = SubScreen.None
                        currentTab = NavTab.HOME
                    }
                },
                onBack = { currentSubScreen = SubScreen.None }
            )
            return
        }
        SubScreen.BackgroundHelp -> {
            BackgroundHelpScreen(onBack = { currentSubScreen = SubScreen.None })
            return
        }
        SubScreen.StateGallery -> {
            StateGalleryScreen(onDismiss = { currentSubScreen = SubScreen.None })
            return
        }
        SubScreen.None -> Unit
    }

    val showSosFab = meshState.role == Role.MULE &&
            meshState.capability == Capability.FULL &&
            meshState.sos == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DtnColors.BgBase)
    ) {
        // Tab content
        when (currentTab) {
            NavTab.HOME -> {
                HomeScreen(
                    state = meshState,
                    onStartListening = {
                        scope.launch { repo.setListening(true) }
                    },
                    onRequestStopListening = {
                        activeSheet = ActiveSheet.StopListening
                    },
                    onNavigateToCarrying = {
                        currentTab = NavTab.CARRYING
                    },
                    onRequestCancelSos = {
                        activeSheet = ActiveSheet.CancelSos
                    },
                    onDebugLongClick = {
                        currentSubScreen = SubScreen.StateGallery
                    }
                )
            }
            NavTab.CARRYING -> {
                CarryingScreen(
                    packets = packets,
                    isListening = meshState.listening,
                    onPacketClick = { packet ->
                        inspectingPacket = packet
                    },
                    onStartListening = {
                        scope.launch { repo.setListening(true) }
                    }
                )
            }
            NavTab.SETTINGS -> {
                SettingsScreen(
                    settings = settingsState,
                    hasActiveSos = meshState.sos != null,
                    onRoleChange = { role ->
                        scope.launch { repo.setRole(role) }
                    },
                    onScanChange = { scan ->
                        scope.launch { repo.setScan(scan) }
                    },
                    onTtlChange = { ttl ->
                        scope.launch { repo.setTtl(ttl) }
                    },
                    onHapticsChange = { hapticsEnabled ->
                        scope.launch { repo.setHaptics(hapticsEnabled) }
                    },
                    onNavigateToGatewaySetup = {
                        currentSubScreen = SubScreen.GatewaySetup
                    },
                    onNavigateToBackgroundHelp = {
                        currentSubScreen = SubScreen.BackgroundHelp
                    }
                )
            }
        }

        // Soft bottom gradient scrim behind the floating navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DtnColors.BgBase.copy(alpha = 0.70f),
                            DtnColors.BgBase.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Floating Glass Nav Bar
        GlassNavBar(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            showSosFab = showSosFab,
            onSosClick = {
                activeSheet = ActiveSheet.ComposeSos
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ─── OVERLAY SHEETS ──────────────────────────────────────────────────────
    when (activeSheet) {
        ActiveSheet.ComposeSos -> {
            ComposeSosSheet(
                onDismiss = { activeSheet = ActiveSheet.None },
                onSosSent = {
                    currentTab = NavTab.HOME
                }
            )
        }
        ActiveSheet.StopListening -> {
            StopListeningSheet(
                onKeepListening = { activeSheet = ActiveSheet.None },
                onStopListeningConfirm = {
                    scope.launch { repo.setListening(false) }
                    activeSheet = ActiveSheet.None
                },
                onDismiss = { activeSheet = ActiveSheet.None }
            )
        }
        ActiveSheet.CancelSos -> {
            CancelSosSheet(
                onKeepBroadcasting = { activeSheet = ActiveSheet.None },
                onCancelSosConfirm = {
                    scope.launch { repo.cancelSos() }
                    activeSheet = ActiveSheet.None
                },
                onDismiss = { activeSheet = ActiveSheet.None }
            )
        }
        ActiveSheet.LowBattery -> {
            LowBatterySheet(
                batteryPercent = meshState.batteryPercent,
                onSwitchToSaver = {
                    scope.launch { repo.setScan(ScanIntensity.SAVER) }
                    activeSheet = ActiveSheet.None
                },
                onKeepCurrent = { activeSheet = ActiveSheet.None },
                onDismiss = { activeSheet = ActiveSheet.None }
            )
        }
        ActiveSheet.SupportedPhones -> {
            SupportedPhonesSheet(onDismiss = { activeSheet = ActiveSheet.None })
        }
        ActiveSheet.None -> Unit
    }

    // Packet detail sheet
    inspectingPacket?.let { packet ->
        PacketDetailSheet(
            packet = packet,
            onDismiss = { inspectingPacket = null }
        )
    }
}

