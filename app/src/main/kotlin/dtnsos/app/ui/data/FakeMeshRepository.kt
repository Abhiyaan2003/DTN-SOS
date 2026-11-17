package dtnsos.app.ui.data

import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

class FakeMeshRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : MeshUiRepository {

    private val samplePackets = listOf(
        PacketUi(
            msgIdHex = "a41f9c02",
            category = Category.TRAPPED,
            people = 3,
            severity = Severity.HIGH,
            message = "Basement, hearing voices above",
            ageText = "38 min",
            distanceText = "1.2 km",
            tokens = 4,
            state = PacketState.SPRAY,
            lifetimeLeftHours = 23
        ),
        PacketUi(
            msgIdHex = "7be2d310",
            category = Category.MEDICAL,
            people = 1,
            severity = Severity.HIGH,
            message = "Leg injury, cannot walk",
            ageText = "2 h",
            distanceText = "3.4 km",
            tokens = 1,
            state = PacketState.WAIT,
            lifetimeLeftHours = 22
        ),
        PacketUi(
            msgIdHex = "c09a77e4",
            category = Category.FLOOD,
            people = 6,
            severity = Severity.MEDIUM,
            message = "Roof, water rising slowly",
            ageText = "5 h",
            distanceText = "0.8 km",
            tokens = 2,
            state = PacketState.SPRAY,
            lifetimeLeftHours = 19
        ),
        PacketUi(
            msgIdHex = "12dd5b8f",
            category = Category.FIRE,
            people = 2,
            severity = Severity.LOW,
            message = "Smoke on 2nd floor, exit blocked",
            ageText = "9 h",
            distanceText = "5.1 km",
            tokens = 1,
            state = PacketState.WAIT,
            lifetimeLeftHours = 15
        )
    )

    // Clean initial state: 0 packets carried, no mock metrics
    private val _mesh = MutableStateFlow(
        MeshUiState(
            role = Role.MULE,
            capability = Capability.FULL,
            bluetooth = BtState.ON,
            listening = true,
            carriedCount = 0,
            nearbyCount = null,
            lastContactSecondsAgo = null,
            batteryDropPerHour = null,
            batteryPercent = 85,
            isCharging = false,
            sos = null,
            gateway = null
        )
    )
    override val mesh: StateFlow<MeshUiState> = _mesh.asStateFlow()

    private val _packets = MutableStateFlow<List<PacketUi>>(emptyList())
    override val packets: StateFlow<List<PacketUi>> = _packets.asStateFlow()

    private val _settings = MutableStateFlow(
        SettingsUi(
            role = Role.MULE,
            scan = ScanIntensity.BALANCED,
            ttlHours = 24,
            haptics = true,
            bufferLimit = 1000,
            keyIdShort = "3f9a…c21b",
            extendedAdvertising = true
        )
    )
    override val settings: StateFlow<SettingsUi> = _settings.asStateFlow()

    private val _onboarding = MutableStateFlow(
        OnboardingUi(
            step = 0,
            permissions = mapOf(
                PermissionKey.NEARBY_DEVICES to PermissionStatus.NOT_ASKED,
                PermissionKey.LOCATION to PermissionStatus.NOT_ASKED,
                PermissionKey.BACKGROUND to PermissionStatus.NOT_ASKED,
                PermissionKey.NOTIFICATIONS to PermissionStatus.NOT_ASKED
            ),
            keyRegistered = null,
            readiness = listOf(
                ReadinessItem("Bluetooth 5 supported", true),
                ReadinessItem("Extended advertising available", true),
                ReadinessItem("Background service allowed", true),
                ReadinessItem("Signing key created · ID 3f9a…c21b", true)
            )
        )
    )
    override val onboarding: StateFlow<OnboardingUi> = _onboarding.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(true)
    override val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 10)
    override val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents.asSharedFlow()

    private val _currentLocation = MutableStateFlow<LocationUi>(
        LocationUi.Ready(lat = 26.32, lon = 89.45)
    )

    init {
        startSimulationTicker()
    }

    private fun startSimulationTicker() {
        scope.launch {
            var tickerSeconds = 0
            while (isActive) {
                delay(1000L)
                tickerSeconds++

                val current = _mesh.value
                val sos = current.sos
                if (sos != null) {
                    val nextSent = sos.sentSecondsAgo + 1
                    var newCarriers = sos.carriersSeen
                    if (tickerSeconds % 3 == 0 && newCarriers < sos.maxCopies) {
                        newCarriers++
                        _hapticEvents.emit(HapticEvent.CarrierPickup)
                    }
                    _mesh.update {
                        it.copy(
                            sos = sos.copy(
                                sentSecondsAgo = nextSent,
                                carriersSeen = newCarriers
                            )
                        )
                    }
                }
            }
        }
    }

    override suspend fun setListening(on: Boolean) {
        _mesh.update {
            if (on) {
                it.copy(
                    listening = true,
                    nearbyCount = null,
                    lastContactSecondsAgo = null,
                    batteryDropPerHour = null
                )
            } else {
                it.copy(
                    listening = false,
                    nearbyCount = null,
                    lastContactSecondsAgo = null,
                    batteryDropPerHour = 0.0f
                )
            }
        }
    }

    override suspend fun sendSos(draft: SosDraft): Result<Unit> {
        val session = SosSessionUi(
            category = draft.category,
            people = draft.people,
            severity = draft.severity,
            message = draft.message,
            sentSecondsAgo = 0,
            carriersSeen = 1,
            maxCopies = 8,
            lifetimeLeftHours = _settings.value.ttlHours,
            locationText = "${"%.2f".format(draft.lat)}°N, ${"%.2f".format(draft.lon)}°E",
            locationApproximate = false
        )
        _mesh.update { it.copy(sos = session) }
        _hapticEvents.emit(HapticEvent.SendSos)
        return Result.success(Unit)
    }

    override suspend fun cancelSos() {
        _mesh.update { it.copy(sos = null) }
        _hapticEvents.emit(HapticEvent.Confirm)
    }

    override suspend fun setRole(role: Role) {
        _settings.update { it.copy(role = role) }
        _mesh.update {
            if (role == Role.GATEWAY) {
                it.copy(
                    role = Role.GATEWAY,
                    gateway = GatewayStatsUi(
                        received = _packets.value.size,
                        verified = _packets.value.size,
                        unverified = 0,
                        duplicatesDropped = 0,
                        mqtt = MqttState.CONNECTED,
                        queued = 0,
                        lastSyncSecondsAgo = 0
                    )
                )
            } else {
                it.copy(role = Role.MULE, gateway = null)
            }
        }
    }

    override suspend fun setScan(i: ScanIntensity) {
        _settings.update { it.copy(scan = i) }
    }

    override suspend fun setTtl(hours: Int) {
        _settings.update { it.copy(ttlHours = hours) }
    }

    override suspend fun setHaptics(on: Boolean) {
        _settings.update { it.copy(haptics = on) }
        if (on) {
            _hapticEvents.emit(HapticEvent.HapticsEnabled)
        }
    }

    override suspend fun setCarryOnly() {
        _mesh.update { it.copy(capability = Capability.CARRY_ONLY) }
    }

    override suspend fun requestPermission(key: PermissionKey) {
        _onboarding.update { state ->
            val updated = state.permissions.toMutableMap()
            updated[key] = PermissionStatus.GRANTED
            state.copy(permissions = updated)
        }
    }

    override suspend fun submitProfile(name: String, phone: String, notes: String) {
        delay(600)
        _onboarding.update { it.copy(keyRegistered = true) }
    }

    override suspend fun completeOnboarding() {
        _onboardingComplete.value = true
    }

    override suspend fun requestEnableBluetooth() {
        _mesh.update { it.copy(bluetooth = BtState.ON) }
    }

    override fun currentLocation(): Flow<LocationUi> = _currentLocation.asStateFlow()

    // ─── Debug knobs for StateGalleryScreen ──────────────────────────────────
    fun setDebugOnboardingComplete(complete: Boolean) {
        _onboardingComplete.value = complete
    }

    fun setDebugOnboardingStep(step: Int) {
        _onboarding.update { it.copy(step = step) }
    }

    fun setDebugBluetooth(bt: BtState) {
        _mesh.update { it.copy(bluetooth = bt) }
    }

    fun setDebugCapability(cap: Capability) {
        _mesh.update { it.copy(capability = cap) }
    }

    fun setDebugBattery(percent: Int) {
        _mesh.update { it.copy(batteryPercent = percent) }
    }

    fun setDebugMqtt(state: MqttState) {
        _mesh.update { current ->
            current.copy(
                gateway = current.gateway?.copy(mqtt = state)
            )
        }
    }

    fun setDebugLocationState(state: LocationUi) {
        _currentLocation.value = state
    }

    fun setDebugEmptyPackets(empty: Boolean) {
        if (empty) {
            _packets.value = emptyList()
            _mesh.update { it.copy(carriedCount = 0) }
        } else {
            _packets.value = samplePackets
            _mesh.update { it.copy(carriedCount = samplePackets.size) }
        }
    }

    fun setDebugActiveSos(active: Boolean) {
        if (active) {
            val session = SosSessionUi(
                category = Category.TRAPPED,
                people = 3,
                severity = Severity.HIGH,
                message = "Basement, hearing voices above",
                sentSecondsAgo = 45,
                carriersSeen = 3,
                maxCopies = 8,
                lifetimeLeftHours = 24,
                locationText = "26.32°N, 89.45°E",
                locationApproximate = false
            )
            _mesh.update { it.copy(sos = session) }
        } else {
            _mesh.update { it.copy(sos = null) }
        }
    }
}
