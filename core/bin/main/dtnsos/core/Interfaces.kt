package dtnsos.core

/**
 * Clock abstraction — inject in Router and Simulator so tests are deterministic.
 * Never call System.currentTimeMillis() or System.nanoTime() directly in :core.
 */
interface Clock {
    /** Current time in milliseconds since Unix epoch. */
    fun nowMs(): Long

    /** Current time in seconds since Unix epoch. */
    fun nowSec(): Long = nowMs() / 1000L
}

/** System clock for production use (injected in :app). */
object SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}

/** Deterministic clock for tests. Advance with [advanceMs]. */
class FakeClock(private var currentMs: Long = 0L) : Clock {
    override fun nowMs(): Long = currentMs
    fun advanceMs(ms: Long) { currentMs += ms }
    fun set(ms: Long) { currentMs = ms }
}

/**
 * Random number generator abstraction — inject so tests are deterministic.
 * Never call kotlin.random.Random or java.util.Random directly in :core.
 */
interface Rng {
    /** Return [n] random bytes. */
    fun nextBytes(n: Int): ByteArray
}

/** System RNG for production use. */
object SystemRng : Rng {
    override fun nextBytes(n: Int): ByteArray =
        ByteArray(n).also { java.security.SecureRandom().nextBytes(it) }
}

/** Deterministic RNG for tests — returns a fixed repeating sequence. */
class FakeRng(private val seed: ByteArray) : Rng {
    private var pos = 0
    override fun nextBytes(n: Int): ByteArray = ByteArray(n) {
        seed[pos++ % seed.size]
    }
}
