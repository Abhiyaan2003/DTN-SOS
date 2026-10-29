package dtnsos.app.ble

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.ScanFilter
import dtnsos.core.Config

/**
 * Packs and unpacks DTN-SOS BLE advertisements per ARCHITECTURE.md §4 & §8.
 *
 * Payload is manufacturer data with company ID 0xFFFF (experimental/test).
 * Envelope: [Company ID 0xFFFF (2B)][Payload (150B SOS or Beacon)]
 */
object BleEnvelope {
    const val COMPANY_ID = Config.BLE_COMPANY_ID // 0xFFFF

    /**
     * Build non-connectable, non-scannable AdvertiseData containing the 150-byte packet
     * under manufacturer ID 0xFFFF.
     */
    fun buildAdvertiseData(payload: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            .addManufacturerData(COMPANY_ID, payload)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }

    /**
     * Build ScanFilter filtering for manufacturer data with Company ID 0xFFFF.
     * Mask on the version nibble (version=1, bits 7-4 = 0x10).
     */
    fun buildScanFilter(): ScanFilter {
        val filterData = byteArrayOf(0x10.toByte())
        val filterMask = byteArrayOf(0xF0.toByte())

        return ScanFilter.Builder()
            .setManufacturerData(COMPANY_ID, filterData, filterMask)
            .build()
    }
}
