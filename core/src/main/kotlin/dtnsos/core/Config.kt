package dtnsos.core

/**
 * Global protocol constants.
 * All tuneable values live here; nothing is hard-coded in logic classes.
 * ARCHITECTURE.md §7 and §6.
 */
object Config {
    /** Default BLE advertising rotation interval in ms. */
    const val ROTATION_MS: Long = 1_500L

    /** Default spray-and-wait token count L. */
    const val DEFAULT_L: Int = 8

    /** Maximum packets in the buffer. */
    const val BUFFER_MAX: Int = 500

    /** Default TTL in hours (packet field range 24-48). */
    const val TTL_HOURS_DEFAULT: Int = 24

    /** Bloom filter: expected number of elements before rotation. */
    const val BLOOM_N: Int = 1_000

    /** Bloom filter: target false-positive rate. */
    const val BLOOM_P: Double = 0.01

    /** Number of hash functions for Bloom filter (k = 7 for N=1000, P=0.01). */
    const val BLOOM_K: Int = 7

    /** Window in ms during which a Gateway beacon keeps the wait-phase door open. */
    const val GATEWAY_WINDOW_MS: Long = 60_000L

    /** Reject packets with timestamps more than this many seconds in the future. */
    const val MAX_FUTURE_SKEW_SEC: Long = 300L

    /** ACK beacon is interleaved every N advertising slots. */
    const val ACK_INTERLEAVE_SLOTS: Int = 3

    /** BLE manufacturer company ID used in the advertisement. 0xFFFF = test/experimental. */
    const val BLE_COMPANY_ID: Int = 0xFFFF

    /** AD structure overhead: [len][0xFF][0xFF 0xFF] = 4 bytes prepended to the 150-byte payload = 154 bytes total. */
    const val BLE_ADV_PAYLOAD_BYTES: Int = 154
}
