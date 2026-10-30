package dtnsos.core

enum class PacketState {
    HELD,
    DELIVERED,
    EXPIRED,
    CANCELLED
}

data class BufferedPacket(
    val packet: SosPacket,
    val storedAtMs: Long,
    var state: PacketState = PacketState.HELD,
    var tokens: Int = packet.tokens.toInt()
) {
    /** Expiry timestamp in milliseconds since epoch. */
    val expiresAtMs: Long = (packet.timestamp * 1000L) + (packet.ttlHours.toLong() * 3600L * 1000L)

    fun isExpired(nowMs: Long): Boolean = nowMs >= expiresAtMs
}

/**
 * Key wrapper for 8-byte packetId to use safely in HashMaps.
 */
class PacketIdKey(val bytes: ByteArray) {
    init {
        require(bytes.size == 8) { "Packet ID must be exactly 8 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PacketIdKey) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = bytes.joinToString("") { "%02x".format(it) }
}

fun ByteArray.toKey(): PacketIdKey = PacketIdKey(this)

/**
 * In-memory packet buffer with priority-based eviction per ARCHITECTURE.md §6 and §11.
 */
class PacketBuffer(
    val maxCapacity: Int = Config.BUFFER_MAX
) {
    private val map = LinkedHashMap<PacketIdKey, BufferedPacket>()

    val size: Int
        get() = map.size

    fun contains(packetId: ByteArray): Boolean = map.containsKey(packetId.toKey())

    fun get(packetId: ByteArray): BufferedPacket? = map[packetId.toKey()]

    fun getHeldPackets(): List<BufferedPacket> =
        map.values.filter { it.state == PacketState.HELD }

    /**
     * Inserts packet. If duplicate, returns false.
     * If at capacity, evicts lowest priority/triage candidate.
     * Returns true if stored, false if dropped.
     */
    fun add(packet: SosPacket, nowMs: Long): Boolean {
        val key = packet.packetId.toKey()
        if (map.containsKey(key)) {
            return false // Duplicate already in buffer
        }

        // Purge expired first if at capacity
        if (map.size >= maxCapacity) {
            purgeExpired(nowMs)
        }

        // If still at capacity, find an eviction candidate
        if (map.size >= maxCapacity) {
            val candidateKey = findEvictionCandidate(packet, nowMs)
            if (candidateKey != null) {
                map.remove(candidateKey)
            } else {
                return false // Cannot evict; buffer full and new packet is lower value
            }
        }

        map[key] = BufferedPacket(packet = packet, storedAtMs = nowMs)
        return true
    }

    fun updateTokens(packetId: ByteArray, newTokens: Int) {
        map[packetId.toKey()]?.let {
            it.tokens = newTokens.coerceAtLeast(0)
        }
    }

    fun markDelivered(packetId: ByteArray) {
        map[packetId.toKey()]?.let {
            it.state = PacketState.DELIVERED
        }
    }

    fun remove(packetId: ByteArray): Boolean {
        return map.remove(packetId.toKey()) != null
    }

    /**
     * Purges expired packets and updates their state to EXPIRED.
     * Returns list of purged packet IDs.
     */
    fun purgeExpired(nowMs: Long): List<ByteArray> {
        val expiredKeys = ArrayList<PacketIdKey>()
        for ((k, v) in map) {
            if (v.isExpired(nowMs) || v.state == PacketState.EXPIRED) {
                expiredKeys.add(k)
            }
        }
        for (k in expiredKeys) {
            map.remove(k)
        }
        return expiredKeys.map { it.bytes }
    }

    fun clear() {
        map.clear()
    }

    /**
     * Comparator for eviction: lowest score is evicted first.
     * 1. Already DELIVERED or CANCELLED packets are evicted first.
     * 2. Priority: lower priority evicted first.
     * 3. Triage: lower triage evicted first.
     * 4. Remaining TTL: earlier expiration evicted first.
     * 5. Oldest storedAtMs evicted first.
     */
    private fun findEvictionCandidate(incoming: SosPacket, nowMs: Long): PacketIdKey? {
        // Find existing candidate with lowest value
        var worstCandidateKey: PacketIdKey? = null
        var worstCandidate: BufferedPacket? = null

        for ((k, v) in map) {
            if (worstCandidate == null || isWorse(v, worstCandidate, nowMs)) {
                worstCandidate = v
                worstCandidateKey = k
            }
        }

        if (worstCandidate == null) return null

        // Only evict if incoming packet is higher value than worstCandidate, or candidate is non-HELD
        if (worstCandidate.state != PacketState.HELD) {
            return worstCandidateKey
        }

        val incomingBuffered = BufferedPacket(incoming, storedAtMs = nowMs)
        return if (isWorse(worstCandidate, incomingBuffered, nowMs)) {
            worstCandidateKey
        } else {
            null
        }
    }

    private fun isWorse(a: BufferedPacket, b: BufferedPacket, nowMs: Long): Boolean {
        // DELIVERED/CANCELLED is always worse (evict first)
        val aNonHeld = a.state != PacketState.HELD
        val bNonHeld = b.state != PacketState.HELD
        if (aNonHeld != bNonHeld) return aNonHeld

        // Priority (higher is better)
        val aPrio = a.packet.priority
        val bPrio = b.packet.priority
        if (aPrio != bPrio) return aPrio < bPrio

        // Triage (higher is better: 4=critical, 1=supplies)
        val aTriage = a.packet.triage.toInt()
        val bTriage = b.packet.triage.toInt()
        if (aTriage != bTriage) return aTriage < bTriage

        // Remaining TTL (more remaining is better)
        val aRemaining = a.expiresAtMs - nowMs
        val bRemaining = b.expiresAtMs - nowMs
        if (aRemaining != bRemaining) return aRemaining < bRemaining

        // Oldest storedAtMs is worse
        return a.storedAtMs < b.storedAtMs
    }
}
