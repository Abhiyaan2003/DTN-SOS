package dtnsos.app.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MeshUiRepository {
    val mesh: StateFlow<MeshUiState>
    val packets: StateFlow<List<PacketUi>>
    val settings: StateFlow<SettingsUi>
    val onboarding: StateFlow<OnboardingUi>
    val onboardingComplete: StateFlow<Boolean>
    val hapticEvents: SharedFlow<HapticEvent>

    suspend fun setListening(on: Boolean)
    suspend fun sendSos(draft: SosDraft): Result<Unit>
    suspend fun cancelSos()
    suspend fun setRole(role: Role)
    suspend fun setScan(i: ScanIntensity)
    suspend fun setTtl(hours: Int)
    suspend fun setHaptics(on: Boolean)
    suspend fun setCarryOnly()
    suspend fun requestPermission(key: PermissionKey)
    suspend fun submitProfile(name: String, phone: String, notes: String)
    suspend fun completeOnboarding()
    suspend fun requestEnableBluetooth()
    fun currentLocation(): Flow<LocationUi>
}

val LocalMeshUiRepository = staticCompositionLocalOf<MeshUiRepository> {
    error("MeshUiRepository not provided")
}

