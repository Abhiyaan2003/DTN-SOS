package dtnsos.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.util.Log

/**
 * Manages Extended Advertising via a single [AdvertisingSet].
 * Reference: ARCHITECTURE.md §3, §6, §8.
 *
 * Rules:
 * - Connectionless only: legacy off, connectable off, scannable off.
 * - Payload under manufacturer data ID 0xFFFF.
 * - Rotating data: periodic update of advertise data without tearing down the set.
 */
class BleAdvertiser(
    private val adapter: BluetoothAdapter,
    private val onStatusChanged: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "BleAdvertiser"
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var activeSet: AdvertisingSet? = null
    private var isStarting = false
    private var isAdvertising = false

    private var queue: List<ByteArray> = emptyList()
    private var queueIndex: Int = 0

    private val callback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == ADVERTISE_SUCCESS) {
                Log.i(TAG, "AdvertisingSet started successfully. txPower=$txPower")
                activeSet = advertisingSet
                isAdvertising = true
                isStarting = false
                onStatusChanged("Advertising active")
            } else {
                Log.e(TAG, "AdvertisingSet start failed with status: $status")
                activeSet = null
                isAdvertising = false
                isStarting = false
                onStatusChanged("Advertising failed (status $status)")
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
            Log.i(TAG, "AdvertisingSet stopped")
            activeSet = null
            isAdvertising = false
            onStatusChanged("Advertising stopped")
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet?, status: Int) {
            if (status != ADVERTISE_SUCCESS) {
                Log.w(TAG, "setAdvertisingData failed with status: $status")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun updateQueue(newQueue: List<ByteArray>) {
        queue = newQueue
        if (queue.isEmpty()) {
            stop()
            return
        }
        if (!isAdvertising && !isStarting) {
            start()
        } else {
            rotate()
        }
    }

    @SuppressLint("MissingPermission")
    fun rotate() {
        val currentSet = activeSet ?: return
        if (queue.isEmpty()) return

        val payload = queue[queueIndex % queue.size]
        queueIndex++
        val advData = BleEnvelope.buildAdvertiseData(payload)
        currentSet.setAdvertisingData(advData)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isAdvertising || isStarting) return
        if (queue.isEmpty()) return

        advertiser = adapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e(TAG, "BluetoothLeAdvertiser unavailable on this adapter")
            onStatusChanged("Advertiser unavailable")
            return
        }

        val parameters = AdvertisingSetParameters.Builder()
            .setConnectable(false)
            .setScannable(false)
            .setLegacyMode(false)
            .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_1M)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_1M)
            .build()

        val initialPayload = queue[queueIndex % queue.size]
        val advData = BleEnvelope.buildAdvertiseData(initialPayload)

        isStarting = true
        onStatusChanged("Starting advertising...")

        try {
            advertiser?.startAdvertisingSet(
                parameters,
                advData,
                null, // scanResponse
                null, // periodicParameters
                null, // periodicData
                callback
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting advertising set", e)
            isStarting = false
            onStatusChanged("Start error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!isAdvertising && !isStarting) return
        try {
            advertiser?.stopAdvertisingSet(callback)
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping advertising set", e)
        }
        activeSet = null
        isAdvertising = false
        isStarting = false
    }
}
