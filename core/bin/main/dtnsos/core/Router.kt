package dtnsos.core

/**
 * Node roles defined in PRD and ARCHITECTURE.md §3.
 */
enum class NodeRole {
    VICTIM,
    MULE,
    GATEWAY
}

/**
 * Input events for the pure Router state machine.
 */
sealed class RouterEvent {
    data class Scanned(
        val raw: ByteArray,
        val rssi: Int,
        val nowMs: Long
    ) : RouterEvent()

    data class Tick(
        val nowMs: Long
    ) : RouterEvent()

    data class UserCreatedSos(
        val triage: TriageLevel,
        val headCount: UByte,
        val freeText: String,
        val latitude: Double,
        val longitude: Double,
        val signer: Ed25519Signer,
        val nowMs: Long
    ) : RouterEvent()

    data class UserCancel(
        val packetId: ByteArray,
        val nowMs: Long
    ) : RouterEvent()

    object ClearBuffer : RouterEvent()
}

/**
 * Output actions emitted by the pure Router state machine.
 */
sealed class RouterAction {
    data class Store(val packet: SosPacket) : RouterAction()
    data class Drop(val reason: DropReason, val packetId: ByteArray? = null) : RouterAction()
    data class SetAdvertiseQueue(val items: List<ByteArray>) : RouterAction()
    data class EmitAckBeacon(val beacon: AckBeacon) : RouterAction()
    data class EmitGatewayBeacon(val beacon: GatewayBeacon) : RouterAction()
    data class PublishToMqtt(val packet: SosPacket) : RouterAction()
    data class UiEvent(val kind: String, val message: String, val packetId: ByteArray? = null) : RouterAction()
}

/**
 * Pure, deterministic Router state machine for DTN-SOS.
 * Implements all protocol rules from ARCHITECTURE.md §6.
 */
class Router(
    val role: NodeRole,
    val nodeId: ByteArray, // 4-byte random node identifier
    val tokenPolicy: TokenPolicy = HalveOnAdvertisePolicy,
    val clock: Clock = SystemClock,
    val rng: Rng = SystemRng,
    val buffer: PacketBuffer = PacketBuffer(),
    val bloomFilter: RotatingBloomFilter = RotatingBloomFilter()
) {
    init {
        require(nodeId.size == 4) { "nodeId must be 4 bytes" }
    }



    // Track distinct nodes that ACKed each held packet: packetId -> Set<NodeIdKey>
    private val ackedNodes = HashMap<PacketIdKey, HashSet<Int>>()

    // Pending packet IDs to include in the next ACK beacon (up to 8)
    private val pendingAcks = LinkedHashSet<PacketIdKey>()

    // Ring of last delivered packet IDs for Gateway role (up to 8)
    private val deliveredRing = ArrayList<ByteArray>()

    // Timestamp when a Gateway beacon was last heard (for 60s wait-phase window)
    var lastGatewayHeardMs: Long = 0L
        private set

    // Rotation counter for interleaving ACK beacons
    private var tickCount: Long = 0L

    /**
     * Process an incoming [RouterEvent] and return the list of resulting [RouterAction]s.
     */
    fun process(event: RouterEvent): List<RouterAction> {
        val actions = mutableListOf<RouterAction>()

        when (event) {
            is RouterEvent.Scanned -> handleScanned(event, actions)
            is RouterEvent.Tick -> handleTick(event, actions)
            is RouterEvent.UserCreatedSos -> handleUserCreatedSos(event, actions)
            is RouterEvent.UserCancel -> handleUserCancel(event, actions)
            is RouterEvent.ClearBuffer -> handleClearBuffer(actions)
        }

        return actions
    }

    private fun handleScanned(event: RouterEvent.Scanned, actions: MutableList<RouterAction>) {
        val raw = event.raw
        if (raw.isEmpty()) {
            actions.add(RouterAction.Drop(DropReason.LengthInvalid))
            return
        }

        val verFlags = raw[0]
        val version = (verFlags.toInt() ushr 4) and 0x0F
        if (version != 1) {
            actions.add(RouterAction.Drop(DropReason.VersionUnknown))
            return
        }

        val msgTypeBits = (verFlags.toInt() ushr 2) and 0x03
        when (msgTypeBits) {
            MsgType.SOS.bits -> handleSosScanned(raw, event.nowMs, actions)
            MsgType.ACK.bits -> handleAckScanned(raw, event.nowMs, actions)
            MsgType.GATEWAY.bits -> handleGatewayScanned(raw, event.nowMs, actions)
            else -> actions.add(RouterAction.Drop(DropReason.WrongMsgType))
        }
    }

    private fun handleSosScanned(raw: ByteArray, nowMs: Long, actions: MutableList<RouterAction>) {
        // Step 1: Decode
        val decodeResult = PacketCodec.decode(raw)
        val packet = when (decodeResult) {
            is DecodeResult.Ok -> decodeResult.packet
            is DecodeResult.Err -> {
                actions.add(RouterAction.Drop(decodeResult.reason))
                return
            }
        }

        // Step 2: Timestamp checks (Max future skew & TTL expiry)
        val nowSec = nowMs / 1000L
        if (packet.timestamp > nowSec + Config.MAX_FUTURE_SKEW_SEC) {
            actions.add(RouterAction.Drop(DropReason.Expired, packet.packetId))
            return
        }
        val expiresAtSec = packet.timestamp + (packet.ttlHours.toLong() * 3600L)
        if (nowSec >= expiresAtSec) {
            actions.add(RouterAction.Drop(DropReason.Expired, packet.packetId))
            return
        }

        // Step 3: Bloom Filter deduplication check
        if (bloomFilter.contains(packet.packetId)) {
            actions.add(RouterAction.Drop(DropReason.BloomDuplicate, packet.packetId))
            return
        }

        // Step 4: Ed25519 Signature verification
        val signedRegion = PacketCodec.signedRegion(raw)
        val validSig = Ed25519Signer.verify(signedRegion, packet.signature, packet.senderPubKey)
        if (!validSig) {
            actions.add(RouterAction.Drop(DropReason.SigInvalid, packet.packetId))
            return
        }

        // Add to Bloom filter
        bloomFilter.add(packet.packetId)

        // Gateway Role handling
        if (role == NodeRole.GATEWAY) {
            actions.add(RouterAction.PublishToMqtt(packet))
            actions.add(RouterAction.UiEvent("DELIVERED", "SOS delivered to Gateway", packet.packetId))

            // Add to delivered ring (up to 8)
            deliveredRing.add(packet.packetId)
            if (deliveredRing.size > 8) deliveredRing.removeAt(0)

            actions.add(RouterAction.EmitGatewayBeacon(GatewayBeacon(nodeId, ArrayList(deliveredRing))))
            return
        }

        // Mule & Victim handling: calculate adopted tokens
        val adoptedTokens = tokenPolicy.onReceiveSos(packet.tokens.toInt(), isSource = false)
        val storedPacket = packet.copy(tokens = adoptedTokens.toUByte())

        // Add to PacketBuffer
        val stored = buffer.add(storedPacket, nowMs)
        if (!stored) {
            actions.add(RouterAction.Drop(DropReason.BufferFull, packet.packetId))
            return
        }

        buffer.updateTokens(packet.packetId, adoptedTokens)
        actions.add(RouterAction.Store(storedPacket))
        actions.add(RouterAction.UiEvent("STORED", "SOS stored with $adoptedTokens tokens", packet.packetId))

        // Queue an ACK for this packet ID
        pendingAcks.add(packet.packetId.toKey())
        if (pendingAcks.size > AckBeacon.MAX_IDS) {
            // Keep most recent
            val toRemove = pendingAcks.first()
            pendingAcks.remove(toRemove)
        }

        updateAdvertiseQueue(actions)
    }

    private fun handleAckScanned(raw: ByteArray, nowMs: Long, actions: MutableList<RouterAction>) {
        val result = BeaconCodec.decode(raw)
        if (result !is BeaconDecodeResult.Ack) return
        val beacon = result.beacon

        val ackNodeInt = beacon.nodeId.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }

        for (id in beacon.packetIds) {
            val key = id.toKey()
            val held = buffer.get(id) ?: continue

            val nodeSet = ackedNodes.getOrPut(key) { HashSet() }
            if (!nodeSet.contains(ackNodeInt)) {
                nodeSet.add(ackNodeInt)

                // Overhearing ACK halves or decrements tokens per policy
                val newTokens = tokenPolicy.onOverhearAck(held.tokens)
                buffer.updateTokens(id, newTokens)
                actions.add(RouterAction.UiEvent("ACK_OVERHEARD", "Tokens updated to $newTokens", id))
            }
        }
        updateAdvertiseQueue(actions)
    }

    private fun handleGatewayScanned(raw: ByteArray, nowMs: Long, actions: MutableList<RouterAction>) {
        val result = BeaconCodec.decode(raw)
        if (result !is BeaconDecodeResult.Gateway) return
        val beacon = result.beacon

        lastGatewayHeardMs = nowMs

        // For each delivered ID, mark delivered and remove from held
        for (id in beacon.deliveredIds) {
            if (buffer.contains(id)) {
                buffer.markDelivered(id)
                buffer.remove(id)
                actions.add(RouterAction.UiEvent("DELIVERED_AT_GATEWAY", "Packet confirmed at gateway", id))
            }
        }
        updateAdvertiseQueue(actions)
    }

    private fun handleUserCreatedSos(event: RouterEvent.UserCreatedSos, actions: MutableList<RouterAction>) {
        val packet = SosPacket.create(
            signer = event.signer,
            triage = event.triage,
            headCount = event.headCount,
            freeText = event.freeText,
            latitude = event.latitude,
            longitude = event.longitude,
            tokens = Config.DEFAULT_L.toUByte(),
            ttlHours = Config.TTL_HOURS_DEFAULT.toUByte(),
            timestamp = event.nowMs / 1000L
        )

        val stored = buffer.add(packet, event.nowMs)
        if (stored) {
            bloomFilter.add(packet.packetId)
            actions.add(RouterAction.Store(packet))
            actions.add(RouterAction.UiEvent("CREATED", "SOS created by user", packet.packetId))
            updateAdvertiseQueue(actions)
        } else {
            actions.add(RouterAction.Drop(DropReason.BufferFull, packet.packetId))
        }
    }

    private fun handleUserCancel(event: RouterEvent.UserCancel, actions: MutableList<RouterAction>) {
        buffer.remove(event.packetId)
        actions.add(RouterAction.UiEvent("CANCELLED", "SOS cancelled by user", event.packetId))
        updateAdvertiseQueue(actions)
    }

    private fun handleClearBuffer(actions: MutableList<RouterAction>) {
        buffer.clear()
        bloomFilter.clear()
        pendingAcks.clear()
        ackedNodes.clear()
        actions.add(RouterAction.SetAdvertiseQueue(emptyList()))
        actions.add(RouterAction.UiEvent("BUFFER_CLEARED", "Buffer and cache cleared by user"))
    }

    private fun handleTick(event: RouterEvent.Tick, actions: MutableList<RouterAction>) {
        tickCount++

        // Purge expired packets
        val expired = buffer.purgeExpired(event.nowMs)
        for (id in expired) {
            actions.add(RouterAction.Drop(DropReason.Expired, id))
        }

        // Interleave ACK beacon every Config.ACK_INTERLEAVE_SLOTS ticks if pending
        if (pendingAcks.isNotEmpty() && (tickCount % Config.ACK_INTERLEAVE_SLOTS == 0L)) {
            val idsToSend = pendingAcks.take(AckBeacon.MAX_IDS).map { it.bytes }
            val beacon = AckBeacon(nodeId, idsToSend)
            actions.add(RouterAction.EmitAckBeacon(beacon))
            pendingAcks.clear()
        }

        updateAdvertiseQueue(actions)
    }

    private fun updateAdvertiseQueue(actions: MutableList<RouterAction>) {
        val nowMs = clock.nowMs()
        val withinGatewayWindow = (nowMs - lastGatewayHeardMs) <= Config.GATEWAY_WINDOW_MS

        val queue = mutableListOf<ByteArray>()

        for (item in buffer.getHeldPackets()) {
            // Rule 4: Wait phase — if tokens == 1, do not advertise unless within Gateway window
            if (item.tokens > 1 || withinGatewayWindow) {
                // Re-encode packet with current tokens
                val updatedPacket = item.packet.copy(tokens = item.tokens.toUByte())
                val encoded = PacketCodec.encode(updatedPacket)
                queue.add(encoded)
            }
        }

        actions.add(RouterAction.SetAdvertiseQueue(queue))
    }
}
