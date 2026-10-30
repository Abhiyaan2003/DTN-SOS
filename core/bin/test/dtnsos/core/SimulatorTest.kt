package dtnsos.core

import dtnsos.core.sim.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimulatorTest {

    @Test
    fun `3-node simulation delivers SOS packet from Victim via Mule to Gateway in 60s`() {
        val clock = FakeClock(1_730_000_000_000L)
        val rng = FakeRng(ByteArray(32) { 0x55 })

        val victim = SimNode("victim", NodeRole.VICTIM, byteArrayOf(1, 1, 1, 1), HalveOnAdvertisePolicy, clock, rng)
        val mule = SimNode("mule", NodeRole.MULE, byteArrayOf(2, 2, 2, 2), HalveOnAdvertisePolicy, clock, rng)
        val gateway = SimNode("gateway", NodeRole.GATEWAY, byteArrayOf(9, 9, 9, 9), HalveOnAdvertisePolicy, clock, rng)

        // Contact schedule:
        // 0s - 15s : Victim in contact with Mule
        // 20s - 40s : Mule in contact with Gateway
        val contacts = listOf(
            ContactWindow("victim", "mule", startMs = 1_730_000_000_000L, endMs = 1_730_000_015_000L),
            ContactWindow("mule", "gateway", startMs = 1_730_000_020_000L, endMs = 1_730_000_040_000L)
        )

        val sim = Simulator(listOf(victim, mule, gateway), contacts, clock, rng)

        // Victim generates SOS at t=0
        victim.router.process(
            RouterEvent.UserCreatedSos(
                triage = TriageLevel.CRITICAL,
                headCount = 3u,
                freeText = "FLOOD RESCUE",
                latitude = 22.5726,
                longitude = 88.3639,
                signer = victim.signer,
                nowMs = clock.nowMs()
            )
        )
        val createdPacketId = victim.router.buffer.getHeldPackets().first().packet.packetId
        sim.metrics.onPacketCreated(createdPacketId, clock.nowMs())

        // Run simulation for 60 seconds (60,000 ms)
        sim.run(durationMs = 60_000L, stepMs = 1_500L)

        // Assertions
        assertEquals(1.0, sim.metrics.deliveryRatio(), "Packet must be delivered to Gateway")
        assertTrue(sim.metrics.averageLatencyMs() in 20_000.0..35_000.0, "Latency should reflect the contact window arrival")
        println("Simulated 3-node delivery latency: ${sim.metrics.averageLatencyMs()} ms")
    }

    @Test
    fun `measure broadcast overshoot for Policy A B C across concurrent mule contacts`() {
        // PRD OI-1: "simulator reports max copies per policy. Do not assert the 'never exceeds L' claim for broadcast; measure overshoot and report it"
        val policies = listOf(
            HalveOnAdvertisePolicy,
            AckConfirmedPolicy,
            SourceSprayPolicy
        )

        for (policy in policies) {
            val clock = FakeClock(1_730_000_000_000L)
            val rng = FakeRng(ByteArray(32) { 0x33 })

            val victim = SimNode("victim", NodeRole.VICTIM, byteArrayOf(1, 1, 1, 1), policy, clock, rng)
            val mules = (1..4).map { i ->
                SimNode("mule-$i", NodeRole.MULE, byteArrayOf(2, 2, 2, i.toByte()), policy, clock, rng)
            }
            val gateway = SimNode("gateway", NodeRole.GATEWAY, byteArrayOf(9, 9, 9, 9), policy, clock, rng)

            // Victim in broadcast contact with ALL 4 mules at once for 10 seconds
            val contacts = mules.map { mule ->
                ContactWindow("victim", mule.name, startMs = 1_730_000_000_000L, endMs = 1_730_000_010_000L)
            }

            val sim = Simulator(listOf(victim) + mules + listOf(gateway), contacts, clock, rng)

            // L = 8 tokens
            victim.router.process(
                RouterEvent.UserCreatedSos(
                    triage = TriageLevel.TRAPPED,
                    headCount = 1u,
                    freeText = "BROADCAST TEST",
                    latitude = 0.0,
                    longitude = 0.0,
                    signer = victim.signer,
                    nowMs = clock.nowMs()
                )
            )
            val pId = victim.router.buffer.getHeldPackets().first().packet.packetId
            sim.metrics.onPacketCreated(pId, clock.nowMs())

            sim.run(durationMs = 20_000L, stepMs = 1_500L)

            val maxCopies = sim.metrics.maxCopies(pId)
            val L = Config.DEFAULT_L
            val overshoot = (maxCopies - L).coerceAtLeast(0)

            println("[PRD OI-1 Measured] Policy: ${policy.name} | L=$L | MaxCopiesObserved=$maxCopies | Overshoot=$overshoot")

            // Verify simulation completed and nodes behaved sensibly
            assertTrue(maxCopies >= 1, "At least 1 copy must exist")
        }
    }
}
