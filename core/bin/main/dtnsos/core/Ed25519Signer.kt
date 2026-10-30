package dtnsos.core

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer as BcEd25519Signer

/**
 * Ed25519 signing using BouncyCastle's **lightweight API**.
 *
 * We use [BcEd25519Signer] directly — NOT the JCE provider path.
 * This avoids "JCE cannot authenticate the provider" errors on Android where
 * the platform already registers its own BouncyCastle JCE provider.
 *
 * Reference: https://www.bouncycastle.org/docs/docs1.8on/index.html
 * (org.bouncycastle.crypto.signers.Ed25519Signer)
 *
 * Key generation: [Ed25519Signer.generate] uses [Rng] so tests are deterministic.
 */
class Ed25519Signer private constructor(
    private val privateKey: Ed25519PrivateKeyParameters,
    override val publicKey: ByteArray,
) : Signer {

    override fun sign(message: ByteArray): ByteArray {
        val signer = BcEd25519Signer()
        signer.init(true, privateKey)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    override fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean =
        Companion.verify(message, signature, publicKey)

    companion object {
        /**
         * Verify an Ed25519 signature given the message, signature, and 32-byte public key.
         * Can be called without holding private key material.
         */
        fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
            require(publicKey.size == 32) { "Ed25519 public key must be 32 bytes" }
            require(signature.size == 64) { "Ed25519 signature must be 64 bytes" }
            val pubKeyParams = Ed25519PublicKeyParameters(publicKey, 0)
            val verifier = BcEd25519Signer()
            verifier.init(false, pubKeyParams)
            verifier.update(message, 0, message.size)
            return verifier.verifySignature(signature)
        }
        /**
         * Generate a new key pair using the provided [Rng].
         * The [Rng] supplies 32 bytes of seed material.
         */
        fun generate(rng: Rng): Ed25519Signer {
            val seed = rng.nextBytes(32)
            return fromSeed(seed)
        }

        /**
         * Restore a signer from a 32-byte private key seed.
         * Used by the Android KeyStore path (seed is stored encrypted).
         */
        fun fromSeed(seed: ByteArray): Ed25519Signer {
            require(seed.size == 32) { "Ed25519 seed must be 32 bytes" }
            val privateKey = Ed25519PrivateKeyParameters(seed, 0)
            val publicKey  = privateKey.generatePublicKey().encoded
            return Ed25519Signer(privateKey, publicKey)
        }
    }
}
