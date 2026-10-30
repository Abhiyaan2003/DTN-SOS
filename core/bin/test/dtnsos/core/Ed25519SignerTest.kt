package dtnsos.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Ed25519Signer unit tests — M1 gate.
 *
 * Covers (ARCHITECTURE.md §11):
 *  - Sign + verify with correct key → valid.
 *  - Sign with key A, verify with key B → invalid.
 *  - Sign + tamper message → invalid.
 *  - Round-trip from seed → stable public key.
 */
class Ed25519SignerTest {

    private val seedA = ByteArray(32) { it.toByte() }
    private val seedB = ByteArray(32) { (32 - it).toByte() }

    @Test
    fun `sign and verify with same key returns true`() {
        val signer  = Ed25519Signer.fromSeed(seedA)
        val message = "hello DTN-SOS".toByteArray()
        val sig     = signer.sign(message)
        assertTrue(signer.verify(message, sig, signer.publicKey))
    }

    @Test
    fun `verify with different public key returns false`() {
        val signerA  = Ed25519Signer.fromSeed(seedA)
        val signerB  = Ed25519Signer.fromSeed(seedB)
        val message  = "hello".toByteArray()
        val sig      = signerA.sign(message)
        assertFalse(signerA.verify(message, sig, signerB.publicKey))
    }

    @Test
    fun `tampered message fails verification`() {
        val signer  = Ed25519Signer.fromSeed(seedA)
        val message = ByteArray(80) { it.toByte() }
        val sig     = signer.sign(message)
        val tampered = message.copyOf()
        tampered[10] = (tampered[10].toInt() xor 0xFF).toByte()
        assertFalse(signer.verify(tampered, sig, signer.publicKey))
    }

    @Test
    fun `tampered signature fails verification`() {
        val signer  = Ed25519Signer.fromSeed(seedA)
        val message = "test".toByteArray()
        val sig     = signer.sign(message).also { it[0] = (it[0].toInt() xor 0x01).toByte() }
        assertFalse(signer.verify(message, sig, signer.publicKey))
    }

    @Test
    fun `fromSeed produces stable 32-byte public key`() {
        val s1 = Ed25519Signer.fromSeed(seedA)
        val s2 = Ed25519Signer.fromSeed(seedA)
        assertEquals(32, s1.publicKey.size)
        assertArrayEquals(s1.publicKey, s2.publicKey, "Same seed must always yield same public key")
    }

    @Test
    fun `signature is 64 bytes`() {
        val signer = Ed25519Signer.fromSeed(seedA)
        val sig    = signer.sign("data".toByteArray())
        assertEquals(64, sig.size)
    }

    @Test
    fun `generate via FakeRng is deterministic`() {
        val rng = FakeRng(seedA)
        val s1  = Ed25519Signer.generate(FakeRng(seedA))
        val s2  = Ed25519Signer.generate(FakeRng(seedA))
        assertArrayEquals(s1.publicKey, s2.publicKey)
    }

    @Test
    fun `different seeds produce different public keys`() {
        val s1 = Ed25519Signer.fromSeed(seedA)
        val s2 = Ed25519Signer.fromSeed(seedB)
        assertFalse(s1.publicKey.contentEquals(s2.publicKey))
    }
}
