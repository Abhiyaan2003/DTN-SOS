package dtnsos.app.ui.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dtnsos.app.ble.BleMode
import dtnsos.app.ble.CapabilityChecker
import dtnsos.app.location.LocationBuffer
import dtnsos.app.service.BleForegroundService
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.model.*
import dtnsos.core.BufferedPacket
import dtnsos.core.NodeRole
import dtnsos.core.PacketState as CorePacketState
import dtnsos.core.TriageLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BleMeshUiRepository(
    private val context: Context,
    private val getService: () -> BleForegroundService?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) : MeshUiRepository {

    private val locationBuffer = LocationBuffer(context)

    private val _mesh = MutableStateFlow(
        MeshUiState(
            role = Role.MULE,
            capability = readCapability(),
            bluetooth = readBluetoothState(),
            listening = false,
            carriedCount = 0,
            nearbyCount = null,
            lastContactSecondsAgo = null,
            batteryDropPerHour = null,
            batteryPercent = readBatteryPercent(),
            isCharging = readIsCharging(),
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
            bufferLimit = 500,
            keyIdShort = "3f9a…c21b",
            extendedAdvertising = readCapability() == Capability.FULL
        )
    )
    override val settings: StateFlow<SettingsUi> = _settings.asStateFlow()

    private val _onboarding = MutableStateFlow(
        OnboardingUi(
            step = 0,
            permissions = mapOf(
                PermissionKey.NEARBY_DEVICES to PermissionStatus.GRANTED,
                PermissionKey.LOCATION to PermissionStatus.GRANTED,
                PermissionKey.BACKGROUND to PermissionStatus.GRANTED,
                PermissionKey.NOTIFICATIONS to PermissionStatus.GRANTED
            ),
            keyRegistered = true,
            readiness = listOf(
                ReadinessItem("Bluetooth 5 supported", readCapability() == Capability.FULL),
                ReadinessItem("Extended advertising available", readCapability() == Capability.FULL),
                ReadinessItem("Background service allowed", true),
                ReadinessItem("Signing key created · ID 3f9a…c21b", true)
            )
        )
    )
    override val onboarding: StateFlow<OnboardingUi> = _onboarding.asStateFlow()

    private val prefs = context.getSharedPreferences("dtnsos_ui_prefs", Context.MODE_PRIVATE)
    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean("onboarding_complete", true))
    override val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<HapticEvent>(extraBufferCapacity = 10)
    override val hapticEvents: SharedFlow<HapticEvent> = _hapticEvents.asSharedFlow()

    private val _currentLocation = MutableStateFlow<LocationUi>(readInitialLocation())

    init {
        // Register receiver for Bluetooth state changes
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val bt = readBluetoothState()
                        _mesh.update { it.copy(bluetooth = bt) }
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 85
                        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        _mesh.update { it.copy(batteryPercent = pct, isCharging = charging) }
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)
    }

    fun syncFromService(service: BleForegroundService) {
        val sState = service.uiState.value
        val role = if (sState.role == NodeRole.GATEWAY) Role.GATEWAY else Role.MULE
        val isListening = sState.isRunning

        // Map real BufferedPackets to PacketUi
        val realPackets = sState.heldPackets.map { mapBufferedPacket(it) }

        _packets.value = realPackets
        _mesh.update {
            it.copy(
                role = role,
                listening = isListening,
                carriedCount = realPackets.size,
                gateway = if (role == Role.GATEWAY) {
                    GatewayStatsUi(
                        received = realPackets.size,
                        verified = realPackets.size,
                        unverified = 0,
                        duplicatesDropped = 0,
                        mqtt = MqttState.CONNECTED,
                        queued = 0,
                        lastSyncSecondsAgo = 0
                    )
                } else null
            )
        }
    }

    private fun mapBufferedPacket(bp: BufferedPacket): PacketUi {
        val p = bp.packet
        val triage = TriageLevel.fromCode(p.triage)

        val category = when (triage) {
            TriageLevel.TRAPPED -> Category.TRAPPED
            TriageLevel.CRITICAL -> Category.MEDICAL
            TriageLevel.INJURED -> Category.MEDICAL
            TriageLevel.SUPPLIES -> Category.FLOOD
        }

        val severity = when (triage) {
            TriageLevel.CRITICAL -> Severity.HIGH
            TriageLevel.TRAPPED -> Severity.HIGH
            TriageLevel.INJURED -> Severity.MEDIUM
            TriageLevel.SUPPLIES -> Severity.LOW
        }

        val nowSec = System.currentTimeMillis() / 1000
        val ageSec = (nowSec - p.timestamp).coerceAtLeast(0)
        val ageText = when {
            ageSec < 60 -> "${ageSec} s"
            ageSec < 3600 -> "${ageSec / 60} min"
            else -> "${ageSec / 3600} h"
        }

        val state = when (bp.state) {
            CorePacketState.DELIVERED -> PacketState.DELIVERED
            CorePacketState.HELD -> if (bp.tokens > 1) PacketState.SPRAY else PacketState.WAIT
            else -> PacketState.WAIT
        }

        return PacketUi(
            msgIdHex = p.packetIdHex(),
            category = category,
            people = p.headCount.toInt(),
            severity = severity,
            message = p.messageText(),
            ageText = ageText,
            distanceText = null,
            tokens = bp.tokens,
            state = state,
            lifetimeLeftHours = p.ttlHours.toInt()
        )
    }

    private fun readCapability(): Capability {
        val caps = CapabilityChecker.read(context)
        return when (caps.mode) {
            BleMode.A_FULL -> Capability.FULL
            BleMode.B_ONE_WAY -> Capability.CARRY_ONLY
            BleMode.C_LEGACY -> Capability.UNSUPPORTED
            BleMode.UNKNOWN -> if (caps.isExtendedAdvertisingSupported) Capability.FULL else Capability.UNSUPPORTED
        }
    }

    private fun readBluetoothState(): BtState {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        return if (adapter?.isEnabled == true) BtState.ON else BtState.OFF
    }

    private fun readBatteryPercent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        return pct.coerceIn(0, 100)
    }

    private fun readIsCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun readInitialLocation(): LocationUi {
        val cached = locationBuffer.getLastKnownLocation()
        return LocationUi.Ready(cached.latitude, cached.longitude)
    }

    override suspend fun setListening(on: Boolean) {
        if (on) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = BleForegroundService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            _mesh.update {
                it.copy(
                    listening = true,
                    nearbyCount = null,
                    lastContactSecondsAgo = null,
                    batteryDropPerHour = null
                )
            }
        } else {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = BleForegroundService.ACTION_STOP
            }
            context.startService(intent)
            _mesh.update {
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
        val triage = when (draft.category) {
            Category.TRAPPED -> TriageLevel.TRAPPED
            Category.MEDICAL -> if (draft.severity == Severity.HIGH) TriageLevel.CRITICAL else TriageLevel.INJURED
            Category.FIRE -> if (draft.severity == Severity.HIGH) TriageLevel.CRITICAL else TriageLevel.TRAPPED
            Category.FLOOD -> if (draft.severity == Severity.HIGH) TriageLevel.TRAPPED else TriageLevel.SUPPLIES
        }

        // Save location to cache buffer
        locationBuffer.saveLocation(draft.lat, draft.lon, "Emergency SOS")

        // Ensure service is running and broadcast packet
        val service = getService()
        if (service != null) {
            service.createSos(
                triage = triage,
                headCount = draft.people.coerceIn(1, 255).toUByte(),
                freeText = draft.message,
                lat = draft.lat,
                lon = draft.lon
            )
        } else {
            // Start service first then create
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = BleForegroundService.ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
            getService()?.createSos(
                triage = triage,
                headCount = draft.people.coerceIn(1, 255).toUByte(),
                freeText = draft.message,
                lat = draft.lat,
                lon = draft.lon
            )
        }

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
        _mesh.update { it.copy(sos = session, listening = true) }
        _hapticEvents.emit(HapticEvent.SendSos)

        return Result.success(Unit)
    }

    override suspend fun cancelSos() {
        _mesh.update { it.copy(sos = null) }
        _hapticEvents.emit(HapticEvent.Confirm)
    }

    override suspend fun setRole(role: Role) {
        _settings.update { it.copy(role = role) }
        val nodeRole = if (role == Role.GATEWAY) NodeRole.GATEWAY else NodeRole.MULE
        getService()?.setRole(nodeRole)
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
        _onboarding.update { it.copy(keyRegistered = true) }
    }

    override suspend fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        _onboardingComplete.value = true
    }

    @SuppressLint("MissingPermission")
    override suspend fun requestEnableBluetooth() {
        try {
            val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            @Suppress("DEPRECATION")
            adapter?.enable()
        } catch (_: SecurityException) {
        }
        _mesh.update { it.copy(bluetooth = BtState.ON) }
    }

    override fun currentLocation(): Flow<LocationUi> = _currentLocation.asStateFlow()
}
