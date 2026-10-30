package dtnsos.core

/**
 * Sealed class for every reason a packet is dropped.
 * Every drop event must carry one of these. Logged as structured events.
 * ARCHITECTURE.md §6 — Router outputs Drop(reason).
 */
sealed class DropReason {
    /** Raw bytes shorter than [SosPacket.SIZE_BYTES]. */
    object LengthInvalid : DropReason()

    /** CRC32 over [0..145] (byte 24 zeroed) does not match bytes [146..149]. */
    object CrcInvalid : DropReason()

    /** Version nibble (verFlags bits 7-4) != 1. */
    object VersionUnknown : DropReason()

    /** msgType is not SOS (0b00) when a SOS is expected. */
    object WrongMsgType : DropReason()

    /** Packet timestamp is older than TTL, or more than 5 min in the future. */
    object Expired : DropReason()

    /** Both Bloom filters returned a hit — probable duplicate. */
    object BloomDuplicate : DropReason()

    /** Ed25519 signature check failed over the signed region. */
    object SigInvalid : DropReason()

    /** Buffer is at capacity and no eviction slot was available for this priority. */
    object BufferFull : DropReason()

    override fun toString(): String = this::class.simpleName ?: "DropReason"
}
