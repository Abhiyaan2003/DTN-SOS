package dtnsos.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dtnsos.app.service.BleForegroundService
import dtnsos.app.ui.data.BleMeshUiRepository
import dtnsos.app.ui.data.LocalMeshUiRepository
import dtnsos.app.ui.haptics.AndroidHaptics
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.navigation.AppGate
import dtnsos.app.ui.theme.DtnTheme
import dtnsos.app.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var bleService: BleForegroundService? = null
    private var isBound = false

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
        // Ensure window background is solid black immediately
        window.decorView.setBackgroundColor(Color.parseColor("#050505"))
        enableEdgeToEdge()

        val serviceIntent = Intent(this, BleForegroundService::class.java)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        val permLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { /* permissions updated */ }

        if (!PermissionHelper.hasAllPermissions(this)) {
            permLauncher.launch(PermissionHelper.getRequiredPermissions())
        }

        setContent {
            val scope = rememberCoroutineScope()
            val repository = remember {
                BleMeshUiRepository(
                    context = applicationContext,
                    getService = { bleService }
                )
            }

            // Sync with real service when available
            LaunchedEffect(isBound, bleService) {
                bleService?.let { service ->
                    scope.launch {
                        service.uiState.collectLatest {
                            repository.syncFromService(service)
                        }
                    }
                }
            }

            val haptics = remember {
                AndroidHaptics(
                    context = applicationContext,
                    isEnabled = { repository.settings.value.haptics }
                )
            }

            CompositionLocalProvider(
                LocalMeshUiRepository provides repository,
                LocalHaptics provides haptics
            ) {
                DtnTheme {
                    AppGate()
                }
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
