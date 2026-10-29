package dtnsos.core.sim

import dtnsos.core.*

class SimNode(
    val name: String,
    val role: NodeRole,
    val nodeId: ByteArray,
    val tokenPolicy: TokenPolicy,
    val clock: Clock,
    val rng: Rng,
    val signer: Ed25519Signer = Ed25519Signer.generate(rng)
) {
    val router = Router(
        role = role,
        nodeId = nodeId,
        tokenPolicy = tokenPolicy,
        clock = clock,
        rng = rng
    )

    var currentAdvQueue: List<ByteArray> = emptyList()
    var currentAdvIndex: Int = 0

    val currentAdvPayload: ByteArray?
        get() = if (currentAdvQueue.isNotEmpty()) {
            currentAdvQueue[currentAdvIndex % currentAdvQueue.size]
        } else null

    fun tick(nowMs: Long): List<RouterAction> {
        val actions = router.process(RouterEvent.Tick(nowMs))
        for (action in actions) {
            if (action is RouterAction.SetAdvertiseQueue) {
                currentAdvQueue = action.items
                if (currentAdvQueue.isNotEmpty()) {
                    currentAdvIndex = (currentAdvIndex + 1) % currentAdvQueue.size
                }
            }
        }
        return actions
    }

    fun scan(raw: ByteArray, rssi: Int, nowMs: Long): List<RouterAction> {
        val actions = router.process(RouterEvent.Scanned(raw, rssi, nowMs))
        for (action in actions) {
            if (action is RouterAction.SetAdvertiseQueue) {
                currentAdvQueue = action.items
            }
        }
        return actions
    }
}
