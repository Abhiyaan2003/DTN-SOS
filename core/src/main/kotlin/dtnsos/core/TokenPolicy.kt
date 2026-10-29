package dtnsos.core

import kotlin.math.floor

/**
 * Interface for spray token dissemination policies.
 * Reference: ARCHITECTURE.md §6 & PRD OI-1.
 */
interface TokenPolicy {
    val name: String

    /**
     * Compute tokens to adopt upon receiving a packet with advertised [nAdv] tokens.
     */
    fun onReceiveSos(nAdv: Int, isSource: Boolean): Int

    /**
     * Compute remaining tokens on this node after broadcasting the packet.
     */
    fun onBroadcastSos(currentTokens: Int): Int

    /**
     * Compute remaining tokens when an ACK is overheard from a new distinct node.
     */
    fun onOverhearAck(currentTokens: Int): Int
}

/**
 * Policy A: HalveOnAdvertise.
 * - Adopted tokens = max(1, floor(nAdv / 2)).
 * - On each broadcast, tokens = max(1, currentTokens - floor(currentTokens / 2)).
 * - On overhearing ACK, tokens = max(1, currentTokens - floor(currentTokens / 2)).
 */
object HalveOnAdvertisePolicy : TokenPolicy {
    override val name: String = "HalveOnAdvertise"

    override fun onReceiveSos(nAdv: Int, isSource: Boolean): Int {
        if (isSource) return nAdv
        return (nAdv / 2).coerceAtLeast(1)
    }

    override fun onBroadcastSos(currentTokens: Int): Int {
        if (currentTokens <= 1) return 1
        val toGive = currentTokens / 2
        return (currentTokens - toGive).coerceAtLeast(1)
    }

    override fun onOverhearAck(currentTokens: Int): Int {
        if (currentTokens <= 1) return 1
        val halve = currentTokens / 2
        return (currentTokens - halve).coerceAtLeast(1)
    }
}

/**
 * Policy B: AckConfirmed (backoff + contention suppression).
 * - Adopted tokens = max(1, floor(nAdv / 2)).
 * - Broadcast does not decrement immediately; decrement happens on overheard ACK.
 * - On overhearing ACK, tokens = max(1, currentTokens - floor(currentTokens / 2)).
 */
object AckConfirmedPolicy : TokenPolicy {
    override val name: String = "AckConfirmed"

    override fun onReceiveSos(nAdv: Int, isSource: Boolean): Int {
        if (isSource) return nAdv
        return (nAdv / 2).coerceAtLeast(1)
    }

    override fun onBroadcastSos(currentTokens: Int): Int {
        // In AckConfirmed, tokens are held until an ACK confirms delivery
        return currentTokens
    }

    override fun onOverhearAck(currentTokens: Int): Int {
        if (currentTokens <= 1) return 1
        val halve = currentTokens / 2
        return (currentTokens - halve).coerceAtLeast(1)
    }
}

/**
 * Policy C: SourceSpray.
 * - Source node generates packet with L tokens.
 * - Relay/Mule always adopts 1 token (wait phase immediately for relays).
 * - Source retains remaining tokens and decrements by 1 for each distinct ACK overheard,
 *   until 1 token remains (source enters wait phase).
 */
object SourceSprayPolicy : TokenPolicy {
    override val name: String = "SourceSpray"

    override fun onReceiveSos(nAdv: Int, isSource: Boolean): Int {
        if (isSource) return nAdv
        return 1 // Non-source relays always hold exactly 1 token
    }

    override fun onBroadcastSos(currentTokens: Int): Int {
        return currentTokens
    }

    override fun onOverhearAck(currentTokens: Int): Int {
        // Hand out 1 token per ACK heard until only 1 token left
        return (currentTokens - 1).coerceAtLeast(1)
    }
}
