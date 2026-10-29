package dtnsos.core

/**
 * Canonical 150-byte SOS packet.
 *
 * Field layout matches ARCHITECTURE.md §4 exactly.
 * Immutable; per-hop mutable field [tokens] is set by the codec on decode and by the Router before advertising.
 *
 * SIGNED REGION : bytes [0..23] ‖ [25..81]  (skipping [tokens] at offset 24)
 * CRC REGION    : bytes [0..145] with byte 24 zeroed during computation
 */
data class SosPacket(
    /** bits7-4=version(1), bits3-2=msgType, bits1-0=priority */
    val verFlags: Byte,
    /** First 8 bytes of SHA-256(pubKey ‖ timestamp ‖ seq). Big-endian 8 bytes. */
    val packetId: ByteArray,
    /** Unix epoch seconds, unsigned 32-bit stored as Long to avoid sign issues. */
    val timestamp: Long,
    /** Degrees × 1e7, signed int32. */
    val latitude: Int,
    /** Degrees × 1e7, signed int32. */
    val longitude: Int,
    /** 1=supplies, 2=injured, 3=trapped, 4=critical */
    val triage: UByte,
    /** 1-255 */
    val headCount: UByte,
    /** 24-48. Demo override is in app config, NOT in this field. */
    val ttlHours: UByte,
    /** Mutable per hop. NOT included in signature. */
    val tokens: UByte,
    /** Ed25519 public key, 32 bytes. */
    val senderPubKey: ByteArray,
    /** UTF-8/ASCII, zero-padded to 25 bytes. */
    val freeText: ByteArray,
    /** Ed25519 signature over signed region, 64 bytes. */
    val signature: ByteArray,
    /** CRC32 over CRC region (byte 24 treated as 0), 4 bytes big-endian. */
    val crc32: ByteArray,
) {
    init {
        require(packetId.size == 8)     { "packetId must be 8 bytes" }
        require(senderPubKey.size == 32) { "senderPubKey must be 32 bytes" }
        require(freeText.size == 25)     { "freeText must be 25 bytes" }
        require(signature.size == 64)    { "signature must be 64 bytes" }
        require(crc32.size == 4)         { "crc32 must be 4 bytes" }
    }

    // ByteArray fields require structural equality.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SosPacket) return false
        return verFlags == other.verFlags &&
            packetId.contentEquals(other.packetId) &&
            timestamp == other.timestamp &&
            latitude == other.latitude &&
            longitude == other.longitude &&
            triage == other.triage &&
            headCount == other.headCount &&
            ttlHours == other.ttlHours &&
            tokens == other.tokens &&
            senderPubKey.contentEquals(other.senderPubKey) &&
            freeText.contentEquals(other.freeText) &&
            signature.contentEquals(other.signature) &&
            crc32.contentEquals(other.crc32)
    }

    override fun hashCode(): Int {
        var result = verFlags.toInt()
        result = 31 * result + packetId.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + latitude
        result = 31 * result + longitude
        result = 31 * result + triage.hashCode()
        result = 31 * result + headCount.hashCode()
        result = 31 * result + ttlHours.hashCode()
        result = 31 * result + tokens.hashCode()
        result = 31 * result + senderPubKey.contentHashCode()
        result = 31 * result + freeText.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + crc32.contentHashCode()
        return result
    }

    /** Convenience: hex string of the first 8-byte packet ID. */
    fun packetIdHex(): String = packetId.joinToString("") { "%02x".format(it) }

    /** Decoded UTF-8 free text message, trimming trailing zero-padding bytes. */
    fun messageText(): String {
        val nonZeroLen = freeText.indexOfFirst { it == 0.toByte() }.let { if (it == -1) freeText.size else it }
        return String(freeText, 0, nonZeroLen, Charsets.UTF_8)
    }

    /** Latitude converted back to decimal degrees. */
    val latitudeDegrees: Double get() = latitude / 1e7

    /** Longitude converted back to decimal degrees. */
    val longitudeDegrees: Double get() = longitude / 1e7

    /** Message type extracted from verFlags bits 3-2. */
    fun msgType(): MsgType = MsgType.fromBits((verFlags.toInt() ushr 2) and 0x03)

    /** Priority extracted from verFlags bits 1-0. */
    val priority: Int get() = verFlags.toInt() and 0x03
    fun priority(): Int = priority

    /** Version extracted from verFlags bits 7-4. */
    fun version(): Int = (verFlags.toInt() ushr 4) and 0x0F

    companion object {
        const val SIZE_BYTES = 150
        const val OFFSET_VER_FLAGS = 0
        const val OFFSET_PACKET_ID = 1
        const val OFFSET_TIMESTAMP = 9
        const val OFFSET_LATITUDE = 13
        const val OFFSET_LONGITUDE = 17
        const val OFFSET_TRIAGE = 21
        const val OFFSET_HEAD_COUNT = 22
        const val OFFSET_TTL_HOURS = 23
        const val OFFSET_TOKENS = 24
        const val OFFSET_SENDER_PUB_KEY = 25
        const val OFFSET_FREE_TEXT = 57
        const val OFFSET_SIGNATURE = 82
        const val OFFSET_CRC32 = 146

        /**
         * Create, sign, and CRC-finalize a canonical SOS packet.
         */
        fun create(
            signer: Ed25519Signer,
            triage: TriageLevel,
            headCount: UByte,
            freeText: String,
            latitude: Double,
            longitude: Double,
            tokens: UByte = Config.DEFAULT_L.toUByte(),
            ttlHours: UByte = Config.TTL_HOURS_DEFAULT.toUByte(),
            seq: Long = 0L,
            timestamp: Long,
            priority: Int = 0
        ): SosPacket {
            val verFlags = ((1 shl 4) or (MsgType.SOS.bits shl 2) or (priority and 0x03)).toByte()

            val idDigest = java.security.MessageDigest.getInstance("SHA-256")
            idDigest.update(signer.publicKey)
            val tsBytes = ByteArray(8).also { PacketCodec.putUInt32(it, 4, timestamp) }
            idDigest.update(tsBytes)
            val seqBytes = ByteArray(8).also { PacketCodec.putUInt32(it, 4, seq) }
            idDigest.update(seqBytes)
            val hash = idDigest.digest()
            val packetId = hash.copyOfRange(0, 8)

            val latInt = (latitude * 1e7).toInt()
            val lonInt = (longitude * 1e7).toInt()

            val textBytes = freeText.toByteArray(Charsets.UTF_8)
            val paddedText = ByteArray(25)
            textBytes.copyInto(paddedText, destinationOffset = 0, startIndex = 0, endIndex = minOf(textBytes.size, 25))

            val partial = SosPacket(
                verFlags = verFlags,
                packetId = packetId,
                timestamp = timestamp,
                latitude = latInt,
                longitude = lonInt,
                triage = triage.code,
                headCount = headCount,
                ttlHours = ttlHours,
                tokens = tokens,
                senderPubKey = signer.publicKey,
                freeText = paddedText,
                signature = ByteArray(64),
                crc32 = ByteArray(4)
            )

            val signedRegion = PacketCodec.signedRegionFromPacket(partial)
            val signature = signer.sign(signedRegion)

            val withSig = partial.copy(signature = signature)
            val encoded = PacketCodec.encode(withSig)
            val crc32 = PacketCodec.computeCrc32Bytes(encoded)

            return withSig.copy(crc32 = crc32)
        }
    }
}

/** Triage priority levels per ARCHITECTURE.md §4. */
enum class TriageLevel(val code: UByte) {
    SUPPLIES(1u),
    INJURED(2u),
    TRAPPED(3u),
    CRITICAL(4u);

    companion object {
        fun fromCode(code: UByte): TriageLevel = entries.firstOrNull { it.code == code } ?: SUPPLIES
    }
}

/** Message types encoded in verFlags bits 3-2. */
enum class MsgType(val bits: Int) {
    SOS(0b00), ACK(0b01), GATEWAY(0b10), CANCEL(0b11);

    companion object {
        fun fromBits(bits: Int): MsgType = entries.first { it.bits == bits }
    }
}
