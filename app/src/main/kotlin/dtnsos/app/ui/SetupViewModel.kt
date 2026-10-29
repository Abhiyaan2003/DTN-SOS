package dtnsos.app.ui

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dtnsos.app.ble.BleCapabilities
import dtnsos.app.ble.BleMode
import dtnsos.app.ble.CapabilityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SetupUiState(
    val capabilities: BleCapabilities? = null,
    val mode: BleMode = BleMode.UNKNOWN,
    val permissionsGranted: Boolean = false,
    val locationInput: LocationInput = LocationInput(),
    val isLoading: Boolean = true,
    val deviceGuidance: DeviceGuidance = DeviceGuidance.NONE,
)

data class LocationInput(
    /** Latitude in degrees. Null until set. */
    val lat: Double? = null,
    /** Longitude in degrees. Null until set. */
    val lon: Double? = null,
    /** Source of the location value. */
    val source: LocationSource = LocationSource.NONE,
)

enum class LocationSource { NONE, GPS, MANUAL, PRESET }

enum class DeviceGuidance { NONE, XIAOMI, NOTHING }

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun loadCapabilities() {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val caps = CapabilityChecker.read(ctx)
            val guidance = detectDeviceGuidance(caps.deviceModel)
            _uiState.value = _uiState.value.copy(
                capabilities = caps,
                mode = caps.mode,
                deviceGuidance = guidance,
                isLoading = false,
            )
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionsGranted = granted)
    }

    /** Called when the remote phone's capability is known (scanned from a beacon in M3). */
    fun onRemoteCapabilityKnown(remoteExtAdv: Boolean, remoteMaxLen: Int) {
        val local = _uiState.value.capabilities ?: return
        val localOk = local.isExtendedAdvertisingSupported &&
                local.maxAdvertisingDataLength >= dtnsos.core.Config.BLE_ADV_PAYLOAD_BYTES
        val remoteOk = remoteExtAdv && remoteMaxLen >= dtnsos.core.Config.BLE_ADV_PAYLOAD_BYTES
        val mode = when {
            localOk && remoteOk -> BleMode.A_FULL
            localOk || remoteOk -> BleMode.B_ONE_WAY
            else                -> BleMode.C_LEGACY
        }
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun setManualLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            locationInput = LocationInput(lat, lon, LocationSource.MANUAL)
        )
    }

    fun setGpsLocation(location: Location) {
        _uiState.value = _uiState.value.copy(
            locationInput = LocationInput(location.latitude, location.longitude, LocationSource.GPS)
        )
    }

    fun setPresetLocation(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(
            locationInput = LocationInput(lat, lon, LocationSource.PRESET)
        )
    }

    private fun detectDeviceGuidance(model: String): DeviceGuidance {
        val lower = model.lowercase()
        return when {
            lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("hyperos") -> DeviceGuidance.XIAOMI
            lower.contains("nothing") -> DeviceGuidance.NOTHING
            else -> DeviceGuidance.NONE
        }
    }
}
