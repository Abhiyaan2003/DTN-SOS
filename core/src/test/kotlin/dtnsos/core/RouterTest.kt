package dtnsos.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RouterTest {

    private val fakeClock = FakeClock(currentMs = 1_730_000_000_000L) // 1730000000 sec
    private val fakeRng = FakeRng(ByteArray(32) { 0x77.toByte() })
    private val muleNodeId = byteArrayOf(1, 2, 3, 4)

    private fun createValidSosRaw(tokens: UByte = 8u): ByteArray {
        val signer = Ed25519Signer.fromSeed(ByteArray(32) { 0x01 })
        val packet = SosPacket.create(
            signer = signer,
            triage = TriageLevel.TRAPPED,
            headCount = 2u,
            freeText = "HELP NEEDED",
            latitude = 22.5726,
            longitude = 88.3639,
            tokens = tokens,
            ttlHours = 24u,
            timestamp = fakeClock.nowSec()
        )
        return PacketCodec.encode(packet)
    }

    @Test
    fun `scanned valid SOS stores packet adopts halved tokens and queues ACK`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw(tokens = 8u)
        val actions = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))

        // 1. Must emit Store action
        val storeAction = actions.filterIsInstance<RouterAction.Store>().firstOrNull()
        assertNotNull(storeAction, "Must emit Store action")
        assertEquals(4u.toUByte(), storeAction?.packet?.tokens, "Adopted tokens must be floor(8/2) = 4")

        // 2. Buffer must contain packet
        assertEquals(1, router.buffer.size)
        val held = router.buffer.getHeldPackets().first()
        assertEquals(4, held.tokens)

        // 3. Adv queue must contain encoded packet with 4 tokens
        val advQueueAction = actions.filterIsInstance<RouterAction.SetAdvertiseQueue>().firstOrNull()
        assertNotNull(advQueueAction)
        assertEquals(1, advQueueAction?.items?.size)

        // 4. Tick triggers interleaved ACK beacon every ACK_INTERLEAVE_SLOTS slots
        var ackAction: RouterAction.EmitAckBeacon? = null
        for (i in 1..Config.ACK_INTERLEAVE_SLOTS) {
            fakeClock.advanceMs(Config.ROTATION_MS)
            val tickActions = router.process(RouterEvent.Tick(fakeClock.nowMs()))
            val found = tickActions.filterIsInstance<RouterAction.EmitAckBeacon>().firstOrNull()
            if (found != null) {
                ackAction = found
            }
        }
        assertNotNull(ackAction, "ACK beacon must be emitted within 3 slots")
        assertEquals(1, ackAction?.beacon?.packetIds?.size)
    }

    @Test
    fun `duplicate SOS is dropped by Bloom filter`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw()

        // First scan succeeds
        val actions1 = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        assertTrue(actions1.any { it is RouterAction.Store })

        // Second scan must drop due to Bloom filter duplicate
        val actions2 = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        val dropAction = actions2.filterIsInstance<RouterAction.Drop>().firstOrNull()
        assertNotNull(dropAction)
        assertEquals(DropReason.BloomDuplicate, dropAction?.reason)
    }

    @Test
    fun `tampered signature is rejected by Router with SigInvalid`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw()
        // Tamper coordinate (in signed region) and recompute CRC so CRC check passes
        raw[13] = (raw[13].toInt() xor 0x01).toByte()
        val newCrc = PacketCodec.computeCrc32Bytes(raw)
        newCrc.copyInto(raw, 146)

        val actions = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        val dropAction = actions.filterIsInstance<RouterAction.Drop>().firstOrNull()
        assertNotNull(dropAction)
        assertEquals(DropReason.SigInvalid, dropAction?.reason)
    }

    @Test
    fun `overhearing ACK from distinct node halves tokens`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw(tokens = 8u)
        router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        val packetId = router.buffer.getHeldPackets().first().packet.packetId
        assertEquals(4, router.buffer.get(packetId)?.tokens)

        // Distinct node 1 overheards ACK -> tokens: 4 -> 2
        val ack1 = BeaconCodec.encodeAck(AckBeacon(byteArrayOf(5, 5, 5, 5), listOf(packetId)))
        router.process(RouterEvent.Scanned(ack1, rssi = -70, nowMs = fakeClock.nowMs()))
        assertEquals(2, router.buffer.get(packetId)?.tokens)

        // Same node 1 sends duplicate ACK -> tokens must NOT halve again
        router.process(RouterEvent.Scanned(ack1, rssi = -70, nowMs = fakeClock.nowMs()))
        assertEquals(2, router.buffer.get(packetId)?.tokens)

        // Distinct node 2 sends ACK -> tokens: 2 -> 1 (wait phase)
        val ack2 = BeaconCodec.encodeAck(AckBeacon(byteArrayOf(6, 6, 6, 6), listOf(packetId)))
        router.process(RouterEvent.Scanned(ack2, rssi = -70, nowMs = fakeClock.nowMs()))
        assertEquals(1, router.buffer.get(packetId)?.tokens)
    }

    @Test
    fun `wait phase silences advertising when tokens equal 1 unless Gateway heard`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw(tokens = 2u) // adopted = 1
        val actions1 = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        assertEquals(1, router.buffer.getHeldPackets().first().tokens)

        // Queue must be empty because tokens == 1 and no gateway heard
        val queue1 = actions1.filterIsInstance<RouterAction.SetAdvertiseQueue>().first().items
        assertTrue(queue1.isEmpty(), "Packet with 1 token must be silenced during wait phase")

        // Hearing Gateway beacon opens the 60s window
        val gwBeacon = BeaconCodec.encodeGateway(GatewayBeacon(byteArrayOf(9, 9, 9, 9), emptyList()))
        val actions2 = router.process(RouterEvent.Scanned(gwBeacon, rssi = -70, nowMs = fakeClock.nowMs()))

        val queue2 = actions2.filterIsInstance<RouterAction.SetAdvertiseQueue>().first().items
        assertEquals(1, queue2.size, "Packet must be advertised during active Gateway window even in wait phase")

        // Advance 61 seconds (past 60s window) -> silenced again
        fakeClock.advanceMs(61_000L)
        val tickActions = router.process(RouterEvent.Tick(fakeClock.nowMs()))
        val queue3 = tickActions.filterIsInstance<RouterAction.SetAdvertiseQueue>().first().items
        assertTrue(queue3.isEmpty(), "Packet must be silenced after Gateway window expires")
    }

    @Test
    fun `Gateway role publishes to MQTT on valid SOS`() {
        val router = Router(
            role = NodeRole.GATEWAY,
            nodeId = byteArrayOf(0xEE.toByte(), 0xEE.toByte(), 0xEE.toByte(), 0xEE.toByte()),
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw()
        val actions = router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))

        val mqttAction = actions.filterIsInstance<RouterAction.PublishToMqtt>().firstOrNull()
        assertNotNull(mqttAction, "Gateway must emit PublishToMqtt action")

        val gwBeaconAction = actions.filterIsInstance<RouterAction.EmitGatewayBeacon>().firstOrNull()
        assertNotNull(gwBeaconAction, "Gateway must emit Gateway beacon")
    }

    @Test
    fun `ClearBuffer event empties buffer and resets advertising queue`() {
        val router = Router(
            role = NodeRole.MULE,
            nodeId = muleNodeId,
            clock = fakeClock,
            rng = fakeRng
        )

        val raw = createValidSosRaw(tokens = 8u)
        router.process(RouterEvent.Scanned(raw, rssi = -70, nowMs = fakeClock.nowMs()))
        assertEquals(1, router.buffer.size)

        val clearActions = router.process(RouterEvent.ClearBuffer)
        assertEquals(0, router.buffer.size, "Buffer must be empty after ClearBuffer")

        val queueAction = clearActions.filterIsInstance<RouterAction.SetAdvertiseQueue>().firstOrNull()
        assertNotNull(queueAction)
        assertTrue(queueAction?.items?.isEmpty() == true, "Adv queue must be empty after ClearBuffer")
    }
}
