package dtnsos.app.service

import android.app.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dtnsos.app.ble.BleAdvertiser
import dtnsos.app.ble.BleScanner
import dtnsos.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServiceUiState(
    val role: NodeRole = NodeRole.MULE,
    val isRunning: Boolean = false,
    val heldPacketsCount: Int = 0,
    val heldPackets: List<BufferedPacket> = emptyList(),
    val lastEventMessage: String = "Idle",
    val advStatus: String = "Stopped",
    val scanStatus: String = "Stopped"
)

/**
 * Foreground Service running the BLE advertiser, scanner, and DTN Router.
 * Reference: ARCHITECTURE.md §3, §8.
 */
class BleForegroundService : Service() {

    companion object {
        private const val TAG = "BleService"
        private const val CHANNEL_ID = "dtnsos_ble_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "dtnsos.app.action.START"
        const val ACTION_STOP = "dtnsos.app.action.STOP"
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadContext = Dispatchers.Default.limitedParallelism(1)

    private val _uiState = MutableStateFlow(ServiceUiState())
    val uiState: StateFlow<ServiceUiState> = _uiState.asStateFlow()

    private var advertiser: BleAdvertiser? = null
    private var scanner: BleScanner? = null
    private var tickerJob: Job? = null

    // Pure Router instance
    lateinit var router: Router
        private set

    val nodeId = ByteArray(4) { (0x10 + it).toByte() }
    val signer = Ed25519Signer.generate(SystemRng)

    inner class LocalBinder : Binder() {
        fun getService(): BleForegroundService = this@BleForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        router = Router(
            role = NodeRole.MULE,
            nodeId = nodeId,
            clock = SystemClock,
            rng = SystemRng
        )

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter

        if (adapter != null) {
            advertiser = BleAdvertiser(adapter) { status ->
                _uiState.value = _uiState.value.copy(advStatus = status)
            }
            scanner = BleScanner(
                adapter = adapter,
                onPacketScanned = { raw, rssi ->
                    serviceScope.launch(singleThreadContext) {
                        handleScannedPacket(raw, rssi)
                    }
                },
                onStatusChanged = { status ->
                    _uiState.value = _uiState.value.copy(scanStatus = status)
                }
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopDtn()
            else -> startDtn()
        }
        return START_STICKY
    }

    fun setRole(newRole: NodeRole) {
        serviceScope.launch(singleThreadContext) {
            router = Router(
                role = newRole,
                nodeId = nodeId,
                clock = SystemClock,
                rng = SystemRng,
                buffer = router.buffer,
                bloomFilter = router.bloomFilter
            )
            _uiState.value = _uiState.value.copy(role = newRole)
        }
    }

    fun createSos(triage: TriageLevel, headCount: UByte, freeText: String, lat: Double, lon: Double) {
        serviceScope.launch(singleThreadContext) {
            val nowMs = System.currentTimeMillis()
            val actions = router.process(
                RouterEvent.UserCreatedSos(
                    triage = triage,
                    headCount = headCount,
                    freeText = freeText,
                    latitude = lat,
                    longitude = lon,
                    signer = signer,
                    nowMs = nowMs
                )
            )
            handleRouterActions(actions)
        }
    }

    fun clearBuffer() {
        serviceScope.launch(singleThreadContext) {
            val actions = router.process(RouterEvent.ClearBuffer)
            handleRouterActions(actions)
            advertiser?.updateQueue(emptyList())
        }
    }

    private fun startDtn() {
        startForegroundNotification()
        scanner?.start()
        advertiser?.start()

        tickerJob?.cancel()
        tickerJob = serviceScope.launch(singleThreadContext) {
            while (isActive) {
                delay(Config.ROTATION_MS)
                val actions = router.process(RouterEvent.Tick(System.currentTimeMillis()))
                handleRouterActions(actions)
                advertiser?.rotate()
            }
        }

        _uiState.value = _uiState.value.copy(isRunning = true)
    }

    private fun stopDtn() {
        tickerJob?.cancel()
        advertiser?.stop()
        scanner?.stop()
        _uiState.value = _uiState.value.copy(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleScannedPacket(raw: ByteArray, rssi: Int) {
        val nowMs = System.currentTimeMillis()
        val actions = router.process(RouterEvent.Scanned(raw, rssi, nowMs))
        handleRouterActions(actions)
    }

    private fun handleRouterActions(actions: List<RouterAction>) {
        for (action in actions) {
            when (action) {
                is RouterAction.SetAdvertiseQueue -> {
                    advertiser?.updateQueue(action.items)
                }
                is RouterAction.EmitAckBeacon -> {
                    val encoded = BeaconCodec.encodeAck(action.beacon)
                    advertiser?.updateQueue(listOf(encoded))
                }
                is RouterAction.EmitGatewayBeacon -> {
                    val encoded = BeaconCodec.encodeGateway(action.beacon)
                    advertiser?.updateQueue(listOf(encoded))
                }
                is RouterAction.UiEvent -> {
                    _uiState.value = _uiState.value.copy(
                        lastEventMessage = "[${action.kind}] ${action.message}"
                    )
                }
                is RouterAction.Store -> {
                    Log.i(TAG, "Stored SOS packet: ${action.packet.packetIdHex()}")
                }
                is RouterAction.PublishToMqtt -> {
                    Log.i(TAG, "Gateway delivered SOS: ${action.packet.packetIdHex()}")
                }
                is RouterAction.Drop -> {
                    Log.d(TAG, "Dropped packet: ${action.reason}")
                }
            }
        }
        _uiState.value = _uiState.value.copy(
            heldPacketsCount = router.buffer.getHeldPackets().size,
            heldPackets = router.buffer.getHeldPackets()
        )
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DTN-SOS Active")
            .setContentText("Connectionless BLE SOS relay is running")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DTN-SOS BLE Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps connectionless BLE scanner and advertiser running in foreground"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        advertiser?.stop()
        scanner?.stop()
    }
}
