package dtnsos.core

import java.util.BitSet

/**
 * Single-epoch Bloom filter with m bits and k hash functions.
 * Uses Kirsch-Mitzenmacher technique (h_i = h1 + i * h2 mod m) for k hashes.
 */
class SingleBloomFilter(
    val numBits: Int = DEFAULT_BITS,
    val numHashes: Int = Config.BLOOM_K
) {
    companion object {
        // Optimal m for n=1000, p=0.01: -1000 * ln(0.01) / (ln 2)^2 ~= 9585 bits.
        // We round to 9600 bits (1200 bytes) for clean alignment.
        const val DEFAULT_BITS: Int = 9600
    }

    private val bitSet = BitSet(numBits)
    var count: Int = 0
        private set

    fun add(data: ByteArray) {
        val (h1, h2) = hash64Pair(data)
        for (i in 0 until numHashes) {
            val combined = (h1 + i * h2).toULong()
            val bitIndex = (combined % numBits.toULong()).toInt()
            bitSet.set(bitIndex)
        }
        count++
    }

    fun contains(data: ByteArray): Boolean {
        val (h1, h2) = hash64Pair(data)
        for (i in 0 until numHashes) {
            val combined = (h1 + i * h2).toULong()
            val bitIndex = (combined % numBits.toULong()).toInt()
            if (!bitSet.get(bitIndex)) {
                return false
            }
        }
        return true
    }

    fun clear() {
        bitSet.clear()
        count = 0
    }

    /**
     * Compute two 64-bit hashes using FNV-1a variants for speed and low collision rate.
     */
    private fun hash64Pair(data: ByteArray): Pair<Long, Long> {
        var h1 = -3750763034362895579L // FNV offset basis 64-bit
        var h2 = -2882303761517117440L // Alternate seed

        val prime1 = 1099511628211L
        val prime2 = 14029467366897L

        for (b in data) {
            val v = (b.toInt() and 0xFF).toLong()
            h1 = (h1 xor v) * prime1
            h2 = (h2 xor v) * prime2
        }

        // Ensure non-zero and odd step for h2 to cover all bits mod m
        if (h2 == 0L) h2 = 1L
        if ((h2 and 1L) == 0L) h2 += 1L

        return Pair(h1, h2)
    }
}

/**
 * Two-epoch rotating Bloom filter per ARCHITECTURE.md §6 & §11.
 *
 * Query returns true if present in either the active or previous epoch.
 * Insertion adds to the active epoch.
 * When active epoch reaches [maxCapacityPerEpoch], it rotates:
 * active becomes previous, and a new active is allocated.
 */
class RotatingBloomFilter(
    val maxCapacityPerEpoch: Int = Config.BLOOM_N,
    val numBits: Int = SingleBloomFilter.DEFAULT_BITS,
    val numHashes: Int = Config.BLOOM_K
) {
    private var active = SingleBloomFilter(numBits, numHashes)
    private var previous = SingleBloomFilter(numBits, numHashes)

    val activeCount: Int
        get() = active.count

    /** Returns true if item is likely in the active or previous epoch. */
    fun contains(packetId: ByteArray): Boolean {
        return active.contains(packetId) || previous.contains(packetId)
    }

    /**
     * Add packetId to active filter. If capacity reached, rotates automatically.
     */
    fun add(packetId: ByteArray) {
        if (active.count >= maxCapacityPerEpoch) {
            rotate()
        }
        active.add(packetId)
    }

    /**
     * Manual epoch rotation.
     */
    fun rotate() {
        previous = active
        active = SingleBloomFilter(numBits, numHashes)
    }

    /** Clear both epochs. */
    fun clear() {
        active.clear()
        previous.clear()
    }
}
