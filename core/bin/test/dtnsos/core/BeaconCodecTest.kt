package dtnsos.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BeaconCodecTest {

    @Test
    fun `ACK beacon encode and decode roundtrip with multiple packet IDs`() {
        val nodeId = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val id1 = ByteArray(8) { (0x10 + it).toByte() }
        val id2 = ByteArray(8) { (0x20 + it).toByte() }
        val id3 = ByteArray(8) { (0x30 + it).toByte() }

        val beacon = AckBeacon(nodeId, listOf(id1, id2, id3))
        val encoded = BeaconCodec.encodeAck(beacon)

        // 1 + 4 + 1 + 3*8 = 30 bytes
        assertEquals(30, encoded.size)
        assertEquals(BeaconCodec.VER_FLAGS_ACK, encoded[0])

        val result = BeaconCodec.decode(encoded)
        assertInstanceOf(BeaconDecodeResult.Ack::class.java, result)
        val decoded = (result as BeaconDecodeResult.Ack).beacon

        assertArrayEquals(nodeId, decoded.nodeId)
        assertEquals(3, decoded.packetIds.size)
        assertArrayEquals(id1, decoded.packetIds[0])
        assertArrayEquals(id2, decoded.packetIds[1])
        assertArrayEquals(id3, decoded.packetIds[2])
    }

    @Test
    fun `Gateway beacon encode and decode roundtrip`() {
        val gwId = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte())
        val delId = ByteArray(8) { (0x50 + it).toByte() }

        val beacon = GatewayBeacon(gwId, listOf(delId))
        val encoded = BeaconCodec.encodeGateway(beacon)

        assertEquals(14, encoded.size) // 1 + 4 + 1 + 8
        assertEquals(BeaconCodec.VER_FLAGS_GATEWAY, encoded[0])

        val result = BeaconCodec.decode(encoded)
        assertInstanceOf(BeaconDecodeResult.Gateway::class.java, result)
        val decoded = (result as BeaconDecodeResult.Gateway).beacon

        assertArrayEquals(gwId, decoded.gatewayId)
        assertEquals(1, decoded.deliveredIds.size)
        assertArrayEquals(delId, decoded.deliveredIds[0])
    }

    @Test
    fun `reject beacon with count exceeding max allowable 8`() {
        val gwId = byteArrayOf(1, 2, 3, 4)
        val ids = (1..9).map { ByteArray(8) { it.toByte() } }
        assertThrows<IllegalArgumentException> {
            GatewayBeacon(gwId, ids)
        }
    }

    @Test
    fun `decode truncated beacon returns error`() {
        val nodeId = byteArrayOf(1, 2, 3, 4)
        val id1 = ByteArray(8) { 1 }
        val encoded = BeaconCodec.encodeAck(AckBeacon(nodeId, listOf(id1)))

        val truncated = encoded.copyOfRange(0, encoded.size - 2)
        val result = BeaconCodec.decode(truncated)
        assertInstanceOf(BeaconDecodeResult.Err::class.java, result)
    }
}
