package dtnsos.core.sim

import dtnsos.core.PacketIdKey
import dtnsos.core.toKey

data class PacketRecord(
    val packetId: ByteArray,
    val createdMs: Long,
    var deliveredMs: Long? = null,
    var maxCopiesObserved: Int = 1
)

/**
 * Metric recorder for simulation runs.
 * Specifically measures broadcast overshoot (PRD OI-1).
 */
class SimMetrics {
    private val records = HashMap<PacketIdKey, PacketRecord>()

    fun onPacketCreated(packetId: ByteArray, nowMs: Long) {
        records[packetId.toKey()] = PacketRecord(packetId, nowMs)
    }

    fun onPacketDelivered(packetId: ByteArray, nowMs: Long) {
        records[packetId.toKey()]?.let {
            if (it.deliveredMs == null) {
                it.deliveredMs = nowMs
            }
        }
    }

    fun sampleCopies(packetId: ByteArray, currentCount: Int) {
        records[packetId.toKey()]?.let {
            if (currentCount > it.maxCopiesObserved) {
                it.maxCopiesObserved = currentCount
            }
        }
    }

    fun maxCopies(packetId: ByteArray): Int =
        records[packetId.toKey()]?.maxCopiesObserved ?: 0

    fun deliveryRatio(): Double {
        if (records.isEmpty()) return 0.0
        val delivered = records.values.count { it.deliveredMs != null }
        return delivered.toDouble() / records.size
    }

    fun averageLatencyMs(): Double {
        val latencies = records.values.mapNotNull {
            it.deliveredMs?.minus(it.createdMs)
        }
        if (latencies.isEmpty()) return 0.0
        return latencies.average()
    }
}
