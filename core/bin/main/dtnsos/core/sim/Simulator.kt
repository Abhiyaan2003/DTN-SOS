package dtnsos.core.sim

import dtnsos.core.*

/** Contact window: nodeA and nodeB can exchange BLE advertisements between startMs and endMs. */
data class ContactWindow(
    val nodeA: String,
    val nodeB: String,
    val startMs: Long,
    val endMs: Long,
    val lossProbability: Double = 0.0
)

class Simulator(
    val nodes: List<SimNode>,
    val contacts: List<ContactWindow>,
    val clock: FakeClock,
    val rng: Rng = FakeRng(ByteArray(32) { 0x42.toByte() })
) {
    val metrics = SimMetrics()

    fun run(durationMs: Long, stepMs: Long = Config.ROTATION_MS) {
        val startMs = clock.nowMs()
        val endMs = startMs + durationMs

        while (clock.nowMs() < endMs) {
            val nowMs = clock.nowMs()

            // 1. Tick each node
            for (node in nodes) {
                val actions = node.tick(nowMs)
                for (a in actions) {
                    if (a is RouterAction.PublishToMqtt) {
                        metrics.onPacketDelivered(a.packet.packetId, nowMs)
                    }
                }
            }

            // 2. Transmit advertisements along active contact windows
            for (contact in contacts) {
                if (nowMs in contact.startMs..contact.endMs) {
                    val nodeA = nodes.firstOrNull { it.name == contact.nodeA }
                    val nodeB = nodes.firstOrNull { it.name == contact.nodeB }

                    if (nodeA != null && nodeB != null) {
                        // Node A -> Node B
                        val payloadA = nodeA.currentAdvPayload
                        if (payloadA != null) {
                            val actions = nodeB.scan(payloadA, rssi = -70, nowMs = nowMs)
                            for (a in actions) {
                                if (a is RouterAction.PublishToMqtt) {
                                    metrics.onPacketDelivered(a.packet.packetId, nowMs)
                                }
                            }
                        }

                        // Node B -> Node A
                        val payloadB = nodeB.currentAdvPayload
                        if (payloadB != null) {
                            val actions = nodeA.scan(payloadB, rssi = -70, nowMs = nowMs)
                            for (a in actions) {
                                if (a is RouterAction.PublishToMqtt) {
                                    metrics.onPacketDelivered(a.packet.packetId, nowMs)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Sample copy counts across all node buffers
            val packetCountMap = HashMap<PacketIdKey, Int>()
            for (node in nodes) {
                for (held in node.router.buffer.getHeldPackets()) {
                    val key = held.packet.packetId.toKey()
                    packetCountMap[key] = (packetCountMap[key] ?: 0) + 1
                }
            }
            for ((key, count) in packetCountMap) {
                metrics.sampleCopies(key.bytes, count)
            }

            clock.advanceMs(stepMs)
        }
    }
}
