# AGENTS.md: DTN-SOS

## Mission
Build an Android + web demo of a connectionless BLE delay-tolerant network for disaster SOS packets. Source of truth: `docs/PRD.md` (what/why) and `docs/ARCHITECTURE.md` (how). Read both before any task. If code and docs disagree, stop and propose a doc change. Never silently deviate.

## Non-negotiables
1. **BLE only, connectionless.** No GATT connections, no pairing/bonding, no classic Bluetooth, no Wi-Fi Direct, no Nearby Connections. Advertise non-connectable; scan passively. The only internet use is Gateway to MQTT and the dashboard.
2. **No invented APIs.** Android BLE API names in the docs are from memory. Check official Android documentation before using any API, and say which doc you checked. If you can't verify, say so.
3. **`:core` has zero Android imports.** All protocol logic lives there and is unit-testable on the JVM.
4. **No unmeasured claims.** Metrics come from logs or the simulator and are labelled "measured" or "simulated". Never write that something works on a device unless a human checked it.
5. **Approved dependencies only** (ARCHITECTURE.md section 12). Ask before adding any.
6. **No secrets, no real personal data.** Fake coordinates, random session codes. Never commit keys or broker credentials.
7. **Do not change the packet layout** without updating ARCHITECTURE.md section 4 and golden test vectors in the same change.

## Repo layout
`core/` Kotlin/JVM logic and simulator | `app/` Android Compose app | `dashboard/` static web map | `docs/` PRD, ARCHITECTURE, PROGRESS, DEVICE_TESTS, DEMO_SCRIPT.

## Commands (confirm they exist before relying on them)
- `./gradlew :core:test` run JVM tests (must pass before every commit)
- `./gradlew :app:assembleDebug` build APK
- `./gradlew :app:lintDebug` lint
- `cd dashboard && npx serve .` local map preview

## Conventions
- Kotlin official style, explicit types on public APIs, immutable data classes, sealed classes for events/actions/drop reasons.
- Inject `Clock` and `Rng`; never call `System.currentTimeMillis()` or `Random` directly in `:core`.
- Router is a pure function of (state, event) to (state, actions). No threads, no I/O inside it.
- Coroutines for I/O in `:app`; BLE callbacks hand off to a single-threaded dispatcher.
- Log decisions as structured events (kind, packetId, reason, rssi). Every drop has a typed reason.
- Constants live in one `Config` object (ADV interval, ROTATION_MS, L, TTL, buffer size, Bloom params).
- Commit messages: conventional commits (`feat(core): ...`). One milestone per branch.
- UI text and docs: plain, precise, no hype.

## Testing rules
- Every protocol rule in ARCHITECTURE.md section 6 has a test.
- Codec: golden vectors, round trip, tamper each region (signed bytes fail; token byte passes; CRC fails).
- Bloom: measured false-positive rate near 1% at n=1000.
- TokenPolicy variants: simulator reports max copies per policy. **Do not assert the "never exceeds L" claim for broadcast; measure overshoot and report it** (PRD OI-1).
- BLE behaviour is verified by humans using `docs/DEVICE_TESTS.md`. You write that checklist; you do not mark it passed.

## Android BLE rules
- Extended advertising via `startAdvertisingSet`, legacy off, connectable off, scannable off. Payload is manufacturer data with company ID 0xFFFF.
- Scan with legacy off. Run advertiser and scanner inside a foreground service. Handle Android 11 vs 12+ permission differences.
- Always run `CapabilityChecker` first and drive Mode A/B/C from it (PRD FR-1). Never assume both phones support extended advertising.
- Assume background scanning is unreliable; the demo runs with the screen on. Do not build workarounds that hide this.
- Target devices: Redmi K20 (HyperOS/MIUI restrictions) and Nothing 4a Pro. Include in-app guidance for each.

## Agent roles
Use these personas in Antigravity's Agent Manager. Hand off through artifacts (plan, test report, PROGRESS.md), not chat.
| Role | Owns | Must not |
|---|---|---|
| Architect | Plans, ARCHITECTURE.md changes, spec decisions, open issues | Write feature code |
| Core Engineer | `:core` codec, crypto, Bloom, buffer, router, token policies, simulator | Import Android classes |
| Android BLE Engineer | `:app` BLE, service, Room, permissions, MQTT, UI | Add connections or pairing; skip CapabilityChecker |
| Dashboard Engineer | `dashboard/` map and stats | Add a backend |
| QA Verifier | Test plans, `DEVICE_TESTS.md`, reviewing diffs against the PRD | Claim device-verified results |
| Docs Lead | README, DEMO_SCRIPT, PROGRESS.md | Invent results |

## Workflow
1. **Plan first.** For each milestone, produce an implementation-plan artifact (tasks, files, tests, risks, questions). Wait for human approval before coding.
2. **One milestone at a time** (PRD section 8). Do not start the next until the gate passes and the human confirms.
3. **After each milestone:** tests green, update `docs/PROGRESS.md` (done, decisions, deviations from docs, open questions, how to verify), update docs if behaviour changed.
4. **Human gates:** device tests (BLE), phone permission flows, broker connectivity. Produce clear step-by-step checklists and expected results; ask the human to run them and report back.
5. **Ambiguity:** stop and ask. Offer 2-3 options with trade-offs and a recommendation. Do not guess.
6. **Scope discipline:** no features outside the PRD. Stretch items (Cancel, Mode C) only after M7 gate.

## Definition of done (per task)
Builds; `:core:test` passes; lint clean; no new dependencies; docs and PROGRESS.md updated; every claim labelled measured/simulated/unverified; no TODOs left without an owner and an entry in PROGRESS.md.

## Known open issues (do not hide)
- Token ceiling is soft in broadcast (PRD OI-1): implement policies A/B/C and measure.
- Adopted tokens = `max(1, floor(n/2))` (OI-2).
- Replay possible within TTL; clock skew (OI-3).
- Extended-adv support on the two phones is unverified until M0 device check.

## If something fails
Capture the exact error, the device model and Android version, and the log. Propose the smallest fix. Never delete tests to get green.
