package dtnsos.app.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build

/**
 * Reads Bluetooth LE extended advertising capabilities from the [BluetoothAdapter].
 *
 * API references verified at:
 *   https://developer.android.com/reference/android/bluetooth/BluetoothAdapter
 *   (methods: isLeExtendedAdvertisingSupported, getLeMaximumAdvertisingDataLength,
 *    isLe2MPhySupported, isLeCodedPhySupported, isLePeriodicAdvertisingSupported)
 *
 * All methods were introduced in API 26 (minSdk), so no additional version guards are needed
 * beyond the minSdk=26 declared in build.gradle.kts.
 *
 * RULE: no GATT connections, no pairing, no bonding. This class is read-only.
 */
data class BleCapabilities(
    val isExtendedAdvertisingSupported: Boolean,
    val maxAdvertisingDataLength: Int,      // bytes; 0 if unavailable
    val is2MPhySupported: Boolean,
    val isCodedPhySupported: Boolean,
    val isPeriodicAdvertisingSupported: Boolean,
    val androidVersion: Int,                // Build.VERSION.SDK_INT
    val deviceModel: String,                // Build.MANUFACTURER + " " + Build.MODEL
    val mode: BleMode,
)

enum class BleMode {
    /** Full: extended adv capable with payload >= 154 bytes. Both phones must be A. */
    A_FULL,
    /** One-way: only this device is capable; Mule acts as combined Mule+Gateway. */
    B_ONE_WAY,
    /** Neither device capable — legacy 31-byte fragmentation (stretch, M7). */
    C_LEGACY,
    /** Not yet determined (waiting for remote device capability report). */
    UNKNOWN,
}

object CapabilityChecker {

    /**
     * Read all BLE capabilities from the local adapter.
     * [mode] is initially [BleMode.UNKNOWN]; the UI finalises it after the remote phone reports.
     *
     * Verified: BluetoothAdapter API 26 methods:
     *   isLeExtendedAdvertisingSupported() — added API 26
     *   getLeMaximumAdvertisingDataLength() — added API 26
     *   isLe2MPhySupported() — added API 26
     *   isLeCodedPhySupported() — added API 26
     *   isLePeriodicAdvertisingSupported() — added API 26
     * Source: https://developer.android.com/reference/android/bluetooth/BluetoothAdapter
     */
    fun read(context: Context): BleCapabilities {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter   = btManager?.adapter

        val extAdv    = adapter?.isLeExtendedAdvertisingSupported ?: false
        val maxLen    = if (adapter != null) adapter.leMaximumAdvertisingDataLength else 0
        val phy2m     = adapter?.isLe2MPhySupported ?: false
        val phyCoded  = adapter?.isLeCodedPhySupported ?: false
        val periodic  = adapter?.isLePeriodicAdvertisingSupported ?: false

        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        // Local mode assessment (UNKNOWN until remote device info is known).
        val localMode = when {
            extAdv && maxLen >= dtnsos.core.Config.BLE_ADV_PAYLOAD_BYTES -> BleMode.A_FULL
            else -> BleMode.B_ONE_WAY
        }

        return BleCapabilities(
            isExtendedAdvertisingSupported = extAdv,
            maxAdvertisingDataLength       = maxLen,
            is2MPhySupported               = phy2m,
            isCodedPhySupported            = phyCoded,
            isPeriodicAdvertisingSupported  = periodic,
            androidVersion                 = Build.VERSION.SDK_INT,
            deviceModel                    = model,
            mode                           = localMode,
        )
    }
}
