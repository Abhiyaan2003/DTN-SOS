# DTN-SOS: Connectionless BLE Delay-Tolerant Network for Disaster SOS

**DTN-SOS** is a zero-infrastructure, connectionless Bluetooth Low Energy (BLE) delay-tolerant network designed for disaster scenarios where cellular networks and internet connectivity are completely unavailable.

Victims can broadcast cryptographically signed emergency SOS packets. Nearby mobile nodes ("Mules"—civilians, emergency responders, volunteers) passively collect, hold, and relay packets using spray-and-wait routing until reaching a Gateway node with satellite, cellular, or MQTT backhaul.

---

## 🌟 Key Features

1. **Zero Connections, Pairing, or Bonding**:
   - Strictly connectionless: operates via Bluetooth 5 Extended Advertising and passive scanning.
   - Non-connectable, non-scannable advertisements under standard manufacturer data ID `0xFFFF`.
   - Never triggers annoying Bluetooth pairing/bonding prompts.
2. **Deterministic Protocol Core (`:core`)**:
   - Zero Android dependencies—100% unit testable on the JVM.
   - Pure state machine router: `(State, Event) -> (State, Actions)`.
   - Injected `Clock` and `Rng` for fully reproducible execution.
3. **Spray-and-Wait Routing & Token Halving**:
   - Packets start with a token budget $L$ (default $L=8$).
   - On relay hop, adopting node takes $\lfloor n/2 \rfloor$ tokens.
   - Silenced into a wait phase when tokens reach 1 until proximity with a Gateway.
   - Advisory ACK beacons interleave to halve remaining tokens upon confirmed receipt.
4. **Rotating Bloom Filter Deduplication**:
   - 2-epoch rotating Bloom filter ($m=9600$ bits, $k=7$).
   - Measured false-positive rate < 1% at $n=1000$ items to prevent network broadcast storms.
5. **Multi-Attribute Priority Eviction**:
   - 500-packet buffer capacity with priority and triage eviction ordering.
   - Expired TTL packets automatically purged.
6. **Cryptographic Integrity & Authenticity**:
   - Ed25519 digital signatures over the canonical 150-byte packet.
   - Mutable hop tokens are excluded from signature verification.
   - CRC32 checksum for quick corrupted packet rejection.
7. **Offline Location Fallback**:
   - Persistent Last Known Location Buffer caches coordinates across app reboots when GPS has no lock and cellular towers are down.
   - Built-in disaster zone presets (Kolkata, Delhi).
8. **Multi-Role Jetpack Compose UI (`:app`)**:
   - **🚨 Victim**: Emergency broadcast trigger, triage selector, headcount, 25-character message, and big SOS button.
   - **🎒 Mule**: Live activity feed, held buffer monitoring, real-time packet message inspector, and buffer clearing.
   - **📡 Gateway**: Delivery confirmation, gateway beacon broadcaster, and MQTT uplink status.
   - **⚙️ Setup**: Hardware capability analysis (Mode A vs Mode B) and battery optimization guides.

---

## 📁 Repository Structure

```
dtn-sos/
├── app/                  # Android application module (Jetpack Compose, BLE Service)
│   ├── src/main/kotlin/dtnsos/app/
│   │   ├── ble/          # BleAdvertiser, BleScanner, BleEnvelope, CapabilityChecker
│   │   ├── location/     # LocationBuffer (offline persistence)
│   │   ├── service/      # BleForegroundService (connectedDevice type)
│   │   └── ui/           # Compose screens (Main, Victim, Mule, Gateway, Setup)
├── core/                 # Pure Kotlin/JVM protocol engine & simulator
│   ├── src/main/kotlin/dtnsos/core/
│   │   ├── BeaconCodec.kt       # ACK & Gateway beacon serialization
│   │   ├── BloomFilter.kt       # 2-epoch rotating Bloom filter
│   │   ├── Ed25519Signer.kt     # BouncyCastle lightweight Ed25519
│   │   ├── PacketBuffer.kt      # Priority/triage eviction buffer
│   │   ├── PacketCodec.kt       # 150-byte canonical wire layout
│   │   ├── Router.kt            # Pure state machine
│   │   ├── TokenPolicy.kt       # Policies A, B, and C
│   │   └── sim/                 # Discrete-event multi-node simulator
│   └── src/test/kotlin/         # 55 JVM unit tests
└── docs/                 # Specifications & Verification Checklists
    ├── ARCHITECTURE.md   # Architectural blueprint and packet layout
    ├── DEVICE_TESTS.md   # Physical two-phone verification checklist
    ├── PRD.md            # Product Requirements Document
    └── PROGRESS.md       # Milestone tracking & measured results
```

---

## 🛠️ Build & Test

### Prerequisites
- JDK 17+
- Android SDK (API 34)

### Running JVM Unit Tests
```bash
./gradlew :core:test
```
*Executes all 55 tests covering golden vectors, byte tampering, Bloom FPR, Router state transitions, and 60-second multi-node simulations.*

### Building the Android APK
```bash
./gradlew :app:assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
Open source for emergency disaster relief research and development.
