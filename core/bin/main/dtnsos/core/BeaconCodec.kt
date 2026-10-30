package dtnsos.core

/**
 * Codec for ACK (0b01) and Gateway (0b10) beacons.
 * Reference: ARCHITECTURE.md §5.
 *
 * Wire format:
 * - ACK:     verFlags(1) | nodeId(4)    | count(1) | ids(8 × count, count ≤ 8)
 * - Gateway: verFlags(1) | gatewayId(4) | count(1) | deliveredIds(8 × count, count ≤ 8)
 */
data class AckBeacon(
    val nodeId: ByteArray,
    val packetIds: List<ByteArray>
) {
    init {
        require(nodeId.size == 4) { "nodeId must be 4 bytes" }
        require(packetIds.size <= MAX_IDS) { "packetIds count must be <= $MAX_IDS" }
        require(packetIds.all { it.size == 8 }) { "Each packetId must be 8 bytes" }
    }

    companion object {
        const val MAX_IDS = 8
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AckBeacon) return false
        if (!nodeId.contentEquals(other.nodeId)) return false
        if (packetIds.size != other.packetIds.size) return false
        return packetIds.indices.all { packetIds[it].contentEquals(other.packetIds[it]) }
    }

    override fun hashCode(): Int {
        var result = nodeId.contentHashCode()
        for (id in packetIds) {
            result = 31 * result + id.contentHashCode()
        }
        return result
    }
}

data class GatewayBeacon(
    val gatewayId: ByteArray,
    val deliveredIds: List<ByteArray>
) {
    init {
        require(gatewayId.size == 4) { "gatewayId must be 4 bytes" }
        require(deliveredIds.size <= MAX_IDS) { "deliveredIds count must be <= $MAX_IDS" }
        require(deliveredIds.all { it.size == 8 }) { "Each deliveredId must be 8 bytes" }
    }

    companion object {
        const val MAX_IDS = 8
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatewayBeacon) return false
        if (!gatewayId.contentEquals(other.gatewayId)) return false
        if (deliveredIds.size != other.deliveredIds.size) return false
        return deliveredIds.indices.all { deliveredIds[it].contentEquals(other.deliveredIds[it]) }
    }

    override fun hashCode(): Int {
        var result = gatewayId.contentHashCode()
        for (id in deliveredIds) {
            result = 31 * result + id.contentHashCode()
        }
        return result
    }
}

sealed class BeaconDecodeResult {
    data class Ack(val beacon: AckBeacon) : BeaconDecodeResult()
    data class Gateway(val beacon: GatewayBeacon) : BeaconDecodeResult()
    data class Err(val reason: String) : BeaconDecodeResult()
}

object BeaconCodec {
    // verFlags: version=1 (bits 7-4 = 0001 -> 0x10)
    // msgType: ACK = 01 (bits 3-2 = 01 -> 0x04) => 0x14
    // msgType: GATEWAY = 10 (bits 3-2 = 10 -> 0x08) => 0x18
    const val VER_FLAGS_ACK: Byte = 0x14.toByte()
    const val VER_FLAGS_GATEWAY: Byte = 0x18.toByte()

    fun encodeAck(beacon: AckBeacon): ByteArray {
        val totalSize = 1 + 4 + 1 + (beacon.packetIds.size * 8)
        val buf = ByteArray(totalSize)
        buf[0] = VER_FLAGS_ACK
        beacon.nodeId.copyInto(buf, destinationOffset = 1, startIndex = 0, endIndex = 4)
        buf[5] = beacon.packetIds.size.toByte()
        var offset = 6
        for (id in beacon.packetIds) {
            id.copyInto(buf, destinationOffset = offset, startIndex = 0, endIndex = 8)
            offset += 8
        }
        return buf
    }

    fun encodeGateway(beacon: GatewayBeacon): ByteArray {
        val totalSize = 1 + 4 + 1 + (beacon.deliveredIds.size * 8)
        val buf = ByteArray(totalSize)
        buf[0] = VER_FLAGS_GATEWAY
        beacon.gatewayId.copyInto(buf, destinationOffset = 1, startIndex = 0, endIndex = 4)
        buf[5] = beacon.deliveredIds.size.toByte()
        var offset = 6
        for (id in beacon.deliveredIds) {
            id.copyInto(buf, destinationOffset = offset, startIndex = 0, endIndex = 8)
            offset += 8
        }
        return buf
    }

    fun decode(buf: ByteArray): BeaconDecodeResult {
        if (buf.isEmpty()) {
            return BeaconDecodeResult.Err("Empty buffer")
        }
        val verFlags = buf[0]
        val version = (verFlags.toInt() ushr 4) and 0x0F
        if (version != 1) {
            return BeaconDecodeResult.Err("Version unknown: $version")
        }
        val msgTypeBits = (verFlags.toInt() ushr 2) and 0x03

        if (buf.size < 6) {
            return BeaconDecodeResult.Err("Buffer too short for beacon header: ${buf.size} bytes")
        }

        val idBytes = buf.copyOfRange(1, 5)
        val count = buf[5].toInt() and 0xFF
        if (count > AckBeacon.MAX_IDS) {
            return BeaconDecodeResult.Err("Count exceeds max allowable ($count > ${AckBeacon.MAX_IDS})")
        }
        val expectedSize = 6 + count * 8
        if (buf.size < expectedSize) {
            return BeaconDecodeResult.Err("Buffer truncated: expected $expectedSize bytes, got ${buf.size}")
        }

        val ids = ArrayList<ByteArray>(count)
        var offset = 6
        for (i in 0 until count) {
            ids.add(buf.copyOfRange(offset, offset + 8))
            offset += 8
        }

        return when (msgTypeBits) {
            MsgType.ACK.bits -> BeaconDecodeResult.Ack(AckBeacon(idBytes, ids))
            MsgType.GATEWAY.bits -> BeaconDecodeResult.Gateway(GatewayBeacon(idBytes, ids))
            else -> BeaconDecodeResult.Err("Unsupported msgType bits for beacon: $msgTypeBits")
        }
    }
}
