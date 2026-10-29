package dtnsos.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BloomFilterTest {

    @Test
    fun `no false negatives on inserted items`() {
        val filter = RotatingBloomFilter()
        val items = (1..500).map { i ->
            ByteBuffer.allocate(8).putLong(i.toLong()).array()
        }

        for (item in items) {
            filter.add(item)
        }

        for (item in items) {
            assertTrue(filter.contains(item), "Inserted item must be found in Bloom filter")
        }
    }

    @Test
    fun `measured false-positive rate near 1 percent at n=1000`() {
        // AGENTS.md requirement: Bloom: measured false-positive rate near 1% at n=1000
        val filter = SingleBloomFilter(numBits = 9600, numHashes = 7)

        val n = 1000
        for (i in 1..n) {
            val item = ByteBuffer.allocate(8).putLong(i.toLong()).array()
            filter.add(item)
        }

        // Test with 10,000 non-inserted items
        val testTrials = 10_000
        var falsePositives = 0
        for (i in (n + 1)..(n + testTrials)) {
            val nonInserted = ByteBuffer.allocate(8).putLong(i.toLong()).array()
            if (filter.contains(nonInserted)) {
                falsePositives++
            }
        }

        val measuredFpr = falsePositives.toDouble() / testTrials
        println("Measured Bloom FPR at n=$n with $testTrials queries: ${"%.4f".format(measuredFpr)} ($falsePositives false positives)")

        // Target is 1% (0.01). Tolerance 0.2% - 2.5% to account for statistical variance
        assertTrue(
            measuredFpr in 0.002..0.025,
            "Bloom FPR must be near 1% (measured: $measuredFpr)"
        )
    }

    @Test
    fun `epoch rotation retains items for 1 epoch and expires after 2 epochs`() {
        val filter = RotatingBloomFilter(maxCapacityPerEpoch = 100)
        val itemA = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val itemB = byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1)

        filter.add(itemA)
        assertTrue(filter.contains(itemA))

        // First rotation: itemA moves to previous epoch
        filter.rotate()
        assertTrue(filter.contains(itemA), "Item must still be retained in previous epoch after 1 rotation")

        // Add itemB to new active epoch
        filter.add(itemB)
        assertTrue(filter.contains(itemB))

        // Second rotation: previous epoch discarded, active becomes previous
        filter.rotate()
        assertFalse(filter.contains(itemA), "ItemA must be expired after 2 rotations")
        assertTrue(filter.contains(itemB), "ItemB should still be in previous epoch after 1 rotation")

        // Third rotation: itemB expired
        filter.rotate()
        assertFalse(filter.contains(itemB), "ItemB must be expired after 2 rotations")
    }
}
