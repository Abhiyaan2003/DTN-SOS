package dtnsos.core

import java.util.zip.CRC32

/**
 * Encodes and decodes the canonical 150-byte SOS packet.
 *
 * Layout is defined in ARCHITECTURE.md §4. Do NOT change the layout without:
 *   1. Updating ARCHITECTURE.md §4.
 *   2. Updating the golden test vectors in PacketCodecTest.
 *   3. Updating this file — all in the same commit.
 *
 * Receiver check order (ARCHITECTURE.md §4, last line):
 *   length → CRC → version/type → expiry → Bloom → signature → store
 * This codec handles length, CRC, and version/type.
 * Expiry, Bloom, and signature are checked by the Router after decode.
 */
object PacketCodec {

    /**
     * Encode a [SosPacket] into exactly [SosPacket.SIZE_BYTES] = 150 bytes (big-endian).
     * The [packet] must already have valid [SosPacket.signature] and [SosPacket.crc32] set.
     * Typically you call [computeCrc32] after signing to fill those fields.
     */
    fun encode(packet: SosPacket): ByteArray {
        val buf = ByteArray(SosPacket.SIZE_BYTES)
        buf[SosPacket.OFFSET_VER_FLAGS] = packet.verFlags

        packet.packetId.copyInto(buf, SosPacket.OFFSET_PACKET_ID)

        putUInt32(buf, SosPacket.OFFSET_TIMESTAMP, packet.timestamp)
        putInt32(buf, SosPacket.OFFSET_LATITUDE, packet.latitude)
        putInt32(buf, SosPacket.OFFSET_LONGITUDE, packet.longitude)

        buf[SosPacket.OFFSET_TRIAGE]     = packet.triage.toByte()
        buf[SosPacket.OFFSET_HEAD_COUNT] = packet.headCount.toByte()
        buf[SosPacket.OFFSET_TTL_HOURS]  = packet.ttlHours.toByte()
        buf[SosPacket.OFFSET_TOKENS]     = packet.tokens.toByte()

        packet.senderPubKey.copyInto(buf, SosPacket.OFFSET_SENDER_PUB_KEY)
        packet.freeText.copyInto(buf, SosPacket.OFFSET_FREE_TEXT)
        packet.signature.copyInto(buf, SosPacket.OFFSET_SIGNATURE)
        packet.crc32.copyInto(buf, SosPacket.OFFSET_CRC32)

        return buf
    }

    /**
     * Decode raw bytes into a [SosPacket].
     * Returns [DecodeResult.Ok] or [DecodeResult.Err] with a typed [DropReason].
     * Only checks length, CRC, version, and msgType.
     */
    fun decode(raw: ByteArray): DecodeResult {
        // 1. Length
        if (raw.size != SosPacket.SIZE_BYTES) {
            return DecodeResult.Err(DropReason.LengthInvalid)
        }

        // 2. CRC — compute over [0..145] with byte 24 zeroed
        val expectedCrc = computeCrc32OverBuffer(raw)
        val actualCrc   = getUInt32(raw, SosPacket.OFFSET_CRC32)
        if (expectedCrc != actualCrc) {
            return DecodeResult.Err(DropReason.CrcInvalid)
        }

        // 3. Version nibble must be 1
        val verFlags = raw[SosPacket.OFFSET_VER_FLAGS]
        val version  = (verFlags.toInt() ushr 4) and 0x0F
        if (version != 1) {
            return DecodeResult.Err(DropReason.VersionUnknown)
        }

        // 4. msgType (bits 3-2) — only SOS (0b00) is handled here; beacons decoded separately
        val msgTypeBits = (verFlags.toInt() ushr 2) and 0x03
        if (msgTypeBits != MsgType.SOS.bits) {
            return DecodeResult.Err(DropReason.WrongMsgType)
        }

        val packet = SosPacket(
            verFlags    = verFlags,
            packetId    = raw.copyOfRange(SosPacket.OFFSET_PACKET_ID, SosPacket.OFFSET_PACKET_ID + 8),
            timestamp   = getUInt32(raw, SosPacket.OFFSET_TIMESTAMP),
            latitude    = getInt32(raw, SosPacket.OFFSET_LATITUDE),
            longitude   = getInt32(raw, SosPacket.OFFSET_LONGITUDE),
            triage      = raw[SosPacket.OFFSET_TRIAGE].toUByte(),
            headCount   = raw[SosPacket.OFFSET_HEAD_COUNT].toUByte(),
            ttlHours    = raw[SosPacket.OFFSET_TTL_HOURS].toUByte(),
            tokens      = raw[SosPacket.OFFSET_TOKENS].toUByte(),
            senderPubKey = raw.copyOfRange(SosPacket.OFFSET_SENDER_PUB_KEY, SosPacket.OFFSET_SENDER_PUB_KEY + 32),
            freeText    = raw.copyOfRange(SosPacket.OFFSET_FREE_TEXT, SosPacket.OFFSET_FREE_TEXT + 25),
            signature   = raw.copyOfRange(SosPacket.OFFSET_SIGNATURE, SosPacket.OFFSET_SIGNATURE + 64),
            crc32       = raw.copyOfRange(SosPacket.OFFSET_CRC32, SosPacket.OFFSET_CRC32 + 4),
        )
        return DecodeResult.Ok(packet)
    }

    /**
     * Returns the signed region for Ed25519: bytes [0..23] ‖ [25..81].
     * Byte 24 (tokens) is intentionally excluded.
     * Input is a 150-byte encoded buffer.
     */
    fun signedRegion(buf: ByteArray): ByteArray {
        require(buf.size == SosPacket.SIZE_BYTES)
        val region = ByteArray(24 + 57) // 81 bytes: [0..23]=24 bytes, [25..81]=57 bytes
        buf.copyInto(region, destinationOffset = 0, startIndex = 0, endIndex = 24)   // [0..23]
        buf.copyInto(region, destinationOffset = 24, startIndex = 25, endIndex = 82) // [25..81]
        return region
    }

    /**
     * Convenience: build the signed region directly from a [SosPacket] (without full encode).
     */
    fun signedRegionFromPacket(packet: SosPacket): ByteArray {
        // Encode into a temp buffer (signature and crc may be zero at this point)
        val tempBuf = encode(packet)
        return signedRegion(tempBuf)
    }

    /**
     * Compute CRC32 over buf[0..145] with buf[24] treated as 0.
     * Returns the value as a Long (unsigned 32-bit).
     */
    fun computeCrc32OverBuffer(buf: ByteArray): Long {
        require(buf.size >= 146)
        val crcBuf = buf.copyOfRange(0, 146)
        crcBuf[24] = 0 // tokens field zeroed for CRC
        val crc = CRC32()
        crc.update(crcBuf)
        return crc.value // unsigned 32-bit as Long
    }

    /**
     * Compute CRC32 and return as 4 big-endian bytes, ready to set in the packet buffer.
     */
    fun computeCrc32Bytes(buf: ByteArray): ByteArray {
        val value = computeCrc32OverBuffer(buf)
        return ByteArray(4).also { putUInt32(it, 0, value) }
    }

    // ─── Byte-order helpers (big-endian) ──────────────────────────────────────

    fun putUInt32(buf: ByteArray, offset: Int, value: Long) {
        buf[offset]     = ((value ushr 24) and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr  8) and 0xFF).toByte()
        buf[offset + 3] = ( value          and 0xFF).toByte()
    }

    fun getUInt32(buf: ByteArray, offset: Int): Long =
        ((buf[offset].toLong()     and 0xFF) shl 24) or
        ((buf[offset + 1].toLong() and 0xFF) shl 16) or
        ((buf[offset + 2].toLong() and 0xFF) shl  8) or
        ( buf[offset + 3].toLong() and 0xFF)

    fun putInt32(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = ((value ushr 24) and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr  8) and 0xFF).toByte()
        buf[offset + 3] = ( value          and 0xFF).toByte()
    }

    fun getInt32(buf: ByteArray, offset: Int): Int =
        ((buf[offset].toInt()     and 0xFF) shl 24) or
        ((buf[offset + 1].toInt() and 0xFF) shl 16) or
        ((buf[offset + 2].toInt() and 0xFF) shl  8) or
        ( buf[offset + 3].toInt() and 0xFF)
}

/** Result type for [PacketCodec.decode]. */
sealed class DecodeResult {
    data class Ok(val packet: SosPacket) : DecodeResult()
    data class Err(val reason: DropReason) : DecodeResult()
}
