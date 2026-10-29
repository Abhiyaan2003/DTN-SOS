package dtnsos.core

/**
 * Signer interface — injected so tests can use deterministic key material.
 * Implementations must NOT import any Android classes.
 */
interface Signer {
    /** Sign [message] and return a 64-byte Ed25519 signature. */
    fun sign(message: ByteArray): ByteArray

    /** Verify a 64-byte [signature] over [message] using [publicKey] (32 bytes). */
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean

    /** 32-byte public key for this signer instance. */
    val publicKey: ByteArray
}
