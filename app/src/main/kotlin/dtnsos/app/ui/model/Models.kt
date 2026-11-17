package dtnsos.app.ui.model

enum class Role { MULE, GATEWAY }
enum class Severity { LOW, MEDIUM, HIGH }
enum class Category { TRAPPED, MEDICAL, FIRE, FLOOD }
enum class ScanIntensity { SAVER, BALANCED, FAST }
enum class Capability { FULL, CARRY_ONLY, UNSUPPORTED }
enum class BtState { ON, OFF }
enum class PacketState { SPRAY, WAIT, DELIVERED }
enum class PermissionKey { NEARBY_DEVICES, LOCATION, BACKGROUND, NOTIFICATIONS }
enum class PermissionStatus { NOT_ASKED, GRANTED, DENIED, PERMANENTLY_DENIED }
enum class MqttState { CONNECTED, CONNECTING, OFFLINE }

sealed interface LocationUi {
    data object Acquiring : LocationUi
    data class Ready(val lat: Double, val lon: Double) : LocationUi
    data class Approximate(val lat: Double, val lon: Double, val ageMin: Int) : LocationUi
    data object Unavailable : LocationUi
}

data class ReadinessItem(
    val label: String,
    val ok: Boolean,
    val reason: String? = null
)

data class MeshUiState(
    val role: Role = Role.MULE,
    val capability: Capability = Capability.FULL,
    val bluetooth: BtState = BtState.ON,
    val listening: Boolean = true,
    val carriedCount: Int = 0,
    val nearbyCount: Int? = null,
    val lastContactSecondsAgo: Int? = null,
    val batteryDropPerHour: Float? = null,
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val sos: SosSessionUi? = null,
    val gateway: GatewayStatsUi? = null
)

data class SosSessionUi(
    val category: Category,
    val people: Int,
    val severity: Severity,
    val message: String,
    val sentSecondsAgo: Int,
    val carriersSeen: Int,
    val maxCopies: Int = 8,
    val lifetimeLeftHours: Int,
    val locationText: String,
    val locationApproximate: Boolean
)

data class GatewayStatsUi(
    val received: Int,
    val verified: Int,
    val unverified: Int,
    val duplicatesDropped: Int,
    val mqtt: MqttState,
    val queued: Int,
    val lastSyncSecondsAgo: Int?
)

data class PacketUi(
    val msgIdHex: String,
    val category: Category,
    val people: Int,
    val severity: Severity,
    val message: String,
    val ageText: String,
    val distanceText: String?,
    val tokens: Int,
    val state: PacketState,
    val lifetimeLeftHours: Int
)

data class SettingsUi(
    val role: Role = Role.MULE,
    val scan: ScanIntensity = ScanIntensity.BALANCED,
    val ttlHours: Int = 24,
    val haptics: Boolean = true,
    val bufferLimit: Int = 1000,
    val keyIdShort: String = "3f9a…c21b",
    val extendedAdvertising: Boolean = true
)

data class OnboardingUi(
    val step: Int = 0,
    val permissions: Map<PermissionKey, PermissionStatus> = mapOf(
        PermissionKey.NEARBY_DEVICES to PermissionStatus.NOT_ASKED,
        PermissionKey.LOCATION to PermissionStatus.NOT_ASKED,
        PermissionKey.BACKGROUND to PermissionStatus.NOT_ASKED,
        PermissionKey.NOTIFICATIONS to PermissionStatus.NOT_ASKED
    ),
    val keyRegistered: Boolean? = null,
    val readiness: List<ReadinessItem> = emptyList()
)

data class SosDraft(
    val category: Category,
    val people: Int,
    val severity: Severity,
    val message: String,
    val lat: Double,
    val lon: Double
)

