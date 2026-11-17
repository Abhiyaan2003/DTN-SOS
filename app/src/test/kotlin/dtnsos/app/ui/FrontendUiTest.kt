package dtnsos.app.ui

import dtnsos.app.ui.components.Utf8Utils
import dtnsos.app.ui.data.FakeMeshRepository
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.Haptics
import dtnsos.app.ui.model.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FrontendUiTest {

    // ─── 1. UTF-8 Byte Counter & Character Boundary Tests ────────────────────
    @Test
    fun `byteCounter accurately measures UTF-8 bytes for multilingual text`() {
        // English (1 byte per char)
        assertEquals(4, Utf8Utils.byteCount("Help"))

        // Hindi (3 bytes per char in Devanagari)
        val hindiText = "मदद"
        assertEquals(9, Utf8Utils.byteCount(hindiText))

        // Bengali (3 bytes per char in Bengali script)
        val bengaliText = "বাঁচাও"
        assertEquals(18, Utf8Utils.byteCount(bengaliText))
    }

    @Test
    fun `trimOrReject rejects text exceeding 51 bytes without splitting characters`() {
        val baseText = "1234567890123456789012345678901234567890123456789" // 49 bytes
        assertEquals(49, Utf8Utils.byteCount(baseText))

        // Adding 2 ASCII chars -> 51 bytes (allowed)
        val fits51 = Utf8Utils.trimOrReject(baseText + "AB", 51, baseText)
        assertEquals(51, Utf8Utils.byteCount(fits51))
        assertEquals(baseText + "AB", fits51)

        // Adding a 3-byte character to 49 bytes would be 52 bytes -> rejected
        val rejected = Utf8Utils.trimOrReject(baseText + "म", 51, baseText)
        assertEquals(baseText, rejected)
        assertEquals(49, Utf8Utils.byteCount(rejected))
    }

    // ─── 2. FakeMeshRepository State Machine & UI Contract Tests ───────────────
    @Test
    fun `initial state starts clean with zero carried packets and listening active`() = runBlocking {
        val repo = FakeMeshRepository()
        val mesh = repo.mesh.value
        val packets = repo.packets.value

        assertTrue(mesh.listening)
        assertEquals(Role.MULE, mesh.role)
        assertEquals(Capability.FULL, mesh.capability)
        assertEquals(BtState.ON, mesh.bluetooth)
        assertEquals(0, mesh.carriedCount)
        assertEquals(0, packets.size)
        assertNull(mesh.nearbyCount)
        assertNull(mesh.lastContactSecondsAgo)
    }

    @Test
    fun `stop listening freezes stats and sets nullable fields to null`() = runBlocking {
        val repo = FakeMeshRepository()

        repo.setListening(false)
        val paused = repo.mesh.value

        assertFalse(paused.listening)
        assertNull(paused.nearbyCount)
        assertNull(paused.lastContactSecondsAgo)
        assertEquals(0.0f, paused.batteryDropPerHour)
        assertEquals(0, repo.packets.value.size)

        repo.setListening(true)
        val resumed = repo.mesh.value

        assertTrue(resumed.listening)
    }

    @Test
    fun `debug gallery can populate and clear sample packets`() = runBlocking {
        val repo = FakeMeshRepository()
        assertEquals(0, repo.packets.value.size)

        repo.setDebugEmptyPackets(false)
        assertEquals(4, repo.packets.value.size)
        assertEquals(4, repo.mesh.value.carriedCount)

        repo.setDebugEmptyPackets(true)
        assertEquals(0, repo.packets.value.size)
        assertEquals(0, repo.mesh.value.carriedCount)
    }

    @Test
    fun `send SOS creates active session and cancel SOS terminates it`() = runBlocking {
        val repo = FakeMeshRepository()

        assertNull(repo.mesh.value.sos)

        val draft = SosDraft(
            category = Category.TRAPPED,
            people = 3,
            severity = Severity.HIGH,
            message = "Trapped on roof",
            lat = 26.32,
            lon = 89.45
        )

        val result = repo.sendSos(draft)
        assertTrue(result.isSuccess)

        val active = repo.mesh.value.sos
        assertNotNull(active)
        assertEquals(Category.TRAPPED, active?.category)
        assertEquals(3, active?.people)
        assertEquals(Severity.HIGH, active?.severity)
        assertEquals(8, active?.maxCopies)
        assertEquals(1, active?.carriersSeen)

        // Cancel SOS
        repo.cancelSos()
        assertNull(repo.mesh.value.sos)
    }

    @Test
    fun `role switching updates state and gateway stats`() = runBlocking {
        val repo = FakeMeshRepository()

        repo.setRole(Role.GATEWAY)
        assertEquals(Role.GATEWAY, repo.mesh.value.role)
        assertNotNull(repo.mesh.value.gateway)
        assertEquals(MqttState.CONNECTED, repo.mesh.value.gateway?.mqtt)

        repo.setRole(Role.MULE)
        assertEquals(Role.MULE, repo.mesh.value.role)
        assertNull(repo.mesh.value.gateway)
    }

    // ─── 3. Haptics Toggle & Rate Limiting Tests ─────────────────────────────
    @Test
    fun `fake haptics respects toggle and rate limits contact events`() {
        var hapticsEnabled = true
        var playedEventsCount = 0
        var lastContactTimestamp = 0L

        val testHaptics = object : Haptics {
            override fun play(event: HapticEvent) {
                if (!hapticsEnabled) return
                if (event == HapticEvent.Contact) {
                    val now = System.currentTimeMillis()
                    if (now - lastContactTimestamp < 10_000L) return
                    lastContactTimestamp = now
                }
                playedEventsCount++
            }

            override fun hasVibrator(): Boolean = true
        }

        // Test normal play
        testHaptics.play(HapticEvent.Tap)
        assertEquals(1, playedEventsCount)

        // Test toggle off
        hapticsEnabled = false
        testHaptics.play(HapticEvent.Tap)
        assertEquals(1, playedEventsCount) // unchanged

        // Re-enable and test contact rate limit
        hapticsEnabled = true
        testHaptics.play(HapticEvent.Contact)
        assertEquals(2, playedEventsCount)

        // Immediate second contact call is dropped
        testHaptics.play(HapticEvent.Contact)
        assertEquals(2, playedEventsCount)
    }
}

