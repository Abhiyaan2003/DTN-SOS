package dtnsos.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import dtnsos.app.service.BleForegroundService
import dtnsos.app.service.ServiceUiState
import dtnsos.app.ui.screens.MainScreen
import dtnsos.app.ui.theme.DtnSosTheme
import dtnsos.app.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private var bleService: BleForegroundService? = null
    private var isBound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? BleForegroundService.LocalBinder
            bleService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep screen on per PRD NFR-2 & AGENTS.md
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val serviceIntent = Intent(this, BleForegroundService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        val permLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            if (granted.values.all { it }) {
                // Permissions granted
            }
        }

        if (!PermissionHelper.hasAllPermissions(this)) {
            permLauncher.launch(PermissionHelper.getRequiredPermissions())
        }

        setContent {
            DtnSosTheme {
                val serviceState by (bleService?.uiState?.collectAsState() ?: remember {
                    mutableStateOf(ServiceUiState())
                })

                MainScreen(
                    serviceState = serviceState,
                    onRoleChanged = { newRole ->
                        bleService?.setRole(newRole)
                    },
                    onBroadcastSos = { triage, headCount, text, lat, lon ->
                        bleService?.createSos(triage, headCount, text, lat, lon)
                    },
                    onClearBuffer = {
                        bleService?.clearBuffer()
                    },
                    onStartService = {
                        val intent = Intent(this@MainActivity, BleForegroundService::class.java).apply {
                            action = BleForegroundService.ACTION_START
                        }
                        ContextCompat.startForegroundService(this@MainActivity, intent)
                    },
                    onStopService = {
                        val intent = Intent(this@MainActivity, BleForegroundService::class.java).apply {
                            action = BleForegroundService.ACTION_STOP
                        }
                        startService(intent)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
