package dtnsos.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PacketBufferTest {

    private fun makePacket(
        idByte: Byte,
        triage: TriageLevel = TriageLevel.INJURED,
        ttlHours: UByte = 24u,
        timestamp: Long = 1_000_000L,
        priority: Int = 0
    ): SosPacket {
        val signer = Ed25519Signer.fromSeed(ByteArray(32) { idByte })
        return SosPacket.create(
            signer = signer,
            triage = triage,
            headCount = 1u,
            freeText = "BUFF TEST",
            latitude = 0.0,
            longitude = 0.0,
            tokens = 4u,
            ttlHours = ttlHours,
            seq = idByte.toLong(),
            timestamp = timestamp,
            priority = priority
        )
    }

    @Test
    fun `basic insertion retrieval and deduplication`() {
        val buffer = PacketBuffer(maxCapacity = 10)
        val p1 = makePacket(1)

        assertTrue(buffer.add(p1, nowMs = 1_000_000_000L))
        assertEquals(1, buffer.size)
        assertTrue(buffer.contains(p1.packetId))

        // Duplicate insertion rejected
        assertFalse(buffer.add(p1, nowMs = 1_000_000_000L))
        assertEquals(1, buffer.size)

        val retrieved = buffer.get(p1.packetId)
        assertNotNull(retrieved)
        assertEquals(p1, retrieved?.packet)
    }

    @Test
    fun `eviction prefers lower priority first when buffer is full`() {
        val buffer = PacketBuffer(maxCapacity = 2)

        val pLow = makePacket(1, priority = 0)
        val pHigh = makePacket(2, priority = 2)
        val pNewCritical = makePacket(3, priority = 3)

        assertTrue(buffer.add(pLow, nowMs = 1_000_000_000L))
        assertTrue(buffer.add(pHigh, nowMs = 1_000_000_000L))
        assertEquals(2, buffer.size)

        // Adding pNewCritical (priority 3) should evict pLow (priority 0)
        assertTrue(buffer.add(pNewCritical, nowMs = 1_000_000_000L))
        assertEquals(2, buffer.size)

        assertFalse(buffer.contains(pLow.packetId), "Low priority packet should have been evicted")
        assertTrue(buffer.contains(pHigh.packetId), "High priority packet must be kept")
        assertTrue(buffer.contains(pNewCritical.packetId), "New critical packet must be kept")
    }

    @Test
    fun `eviction prefers lower triage when priorities are equal`() {
        val buffer = PacketBuffer(maxCapacity = 2)

        val pSupplies = makePacket(1, triage = TriageLevel.SUPPLIES) // triage 1
        val pInjured = makePacket(2, triage = TriageLevel.INJURED)   // triage 2
        val pTrapped = makePacket(3, triage = TriageLevel.TRAPPED)   // triage 3

        assertTrue(buffer.add(pSupplies, nowMs = 1_000_000_000L))
        assertTrue(buffer.add(pInjured, nowMs = 1_000_000_000L))

        // Adding pTrapped should evict pSupplies
        assertTrue(buffer.add(pTrapped, nowMs = 1_000_000_000L))
        assertFalse(buffer.contains(pSupplies.packetId), "Supplies packet should be evicted before injured")
        assertTrue(buffer.contains(pInjured.packetId))
        assertTrue(buffer.contains(pTrapped.packetId))
    }

    @Test
    fun `purgeExpired removes expired packets`() {
        val buffer = PacketBuffer(maxCapacity = 10)
        val p = makePacket(1, ttlHours = 1u, timestamp = 1_000L) // expires at 1_000s + 3600s = 4600s = 4_600_000 ms

        buffer.add(p, nowMs = 1_000_000L)
        assertEquals(1, buffer.size)

        // Before expiry
        val purged1 = buffer.purgeExpired(nowMs = 4_000_000L)
        assertTrue(purged1.isEmpty())
        assertEquals(1, buffer.size)

        // After expiry
        val purged2 = buffer.purgeExpired(nowMs = 5_000_000L)
        assertEquals(1, purged2.size)
        assertEquals(0, buffer.size)
    }
}
