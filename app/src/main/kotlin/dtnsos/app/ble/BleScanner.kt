package dtnsos.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log

/**
 * Passive connectionless BLE scanner per ARCHITECTURE.md §3, §6, §8.
 *
 * Scans with legacy off, targeting manufacturer ID 0xFFFF and passing raw payloads to the listener.
 */
class BleScanner(
    private val adapter: BluetoothAdapter,
    private val onPacketScanned: (raw: ByteArray, rssi: Int) -> Unit,
    private val onStatusChanged: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "BleScanner"
    }

    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val record = result.scanRecord ?: return
            val payload = record.getManufacturerSpecificData(BleEnvelope.COMPANY_ID) ?: return

            onPacketScanned(payload, result.rssi)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            isScanning = false
            onStatusChanged("Scan failed (error $errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isScanning) return
        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BluetoothLeScanner unavailable")
            onStatusChanged("Scanner unavailable")
            return
        }

        val filters = listOf(BleEnvelope.buildScanFilter())
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setLegacy(false)
            .setReportDelay(0)
            .build()

        try {
            scanner?.startScan(filters, settings, callback)
            isScanning = true
            onStatusChanged("Scanning active")
            Log.i(TAG, "BLE scan started with low-latency, legacy=false")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE scan", e)
            isScanning = false
            onStatusChanged("Start scan error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!isScanning) return
        try {
            scanner?.stopScan(callback)
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping scan", e)
        }
        isScanning = false
        onStatusChanged("Scan stopped")
    }
}
