# HoliDate — a proximity mesh dating app for Android

HoliDate lets phones running the app find and talk to each other **directly over the air** —
no servers, no internet account, no cell signal required. It is aimed at people on holiday or
looking for dates locally: open the app, and anyone else nearby with it installed shows up.

Messages don't just travel one hop. Every phone running HoliDate also **relays** messages for
everyone else, so a "like" or a chat can ripple across a beach, a festival, or a hostel by
hopping phone → phone → phone. The more people have the app, the further your reach.

> **Status:** working reference implementation / MVP. It builds as a standard Android Studio
> project. It has *not* been compiled in this environment (no Android SDK here) and has not yet
> been run on real hardware — treat it as a solid, reviewable starting point, not a shipped app.

---

## How it works

```
        Alice's phone                Bob's phone                 Carol's phone
   ┌───────────────────┐        ┌───────────────────┐       ┌───────────────────┐
   │  Compose UI        │        │  (relay only)     │       │  Compose UI        │
   │  ▲   swipe/chat    │        │                   │       │  ▲   swipe/chat    │
   │  │                 │        │                   │       │  │                 │
   │  Repository (Room) │        │  Repository       │       │  Repository (Room) │
   │  ▲                 │        │                   │       │  ▲                 │
   │  MessageRouter ────┼──BLE──▶│  MessageRouter ───┼─BLE──▶│  MessageRouter     │
   │  (sign/verify,     │        │  (dedup + relay)  │       │  (decrypt for me)  │
   │   encrypt/decrypt) │        │                   │       │                    │
   │  NearbyTransport   │◀───────│  NearbyTransport  │◀──────│  NearbyTransport   │
   └───────────────────┘        └───────────────────┘       └───────────────────┘
        Alice ── out of range of Carol ── but Bob carries the message between them
```

### 1. Transport — reaching the phones next to you
`NearbyMeshTransport` uses **Google Nearby Connections** with the `P2P_CLUSTER` strategy. That
strategy is an M-to-N topology: a phone advertises and discovers at the same time and holds
connections to many neighbours at once, automatically choosing the best radio available
(Bluetooth LE, Classic Bluetooth, or a Wi-Fi hotspot). This is the one-hop "cluster".

The transport is hidden behind the `MeshTransport` interface, so a pure-BLE or Wi-Fi Aware
transport (no Google Play Services) could be dropped in later without touching anything above it.

### 2. Router — reaching the phones further afield
`MessageRouter` turns that one-hop transport into a multi-hop, store-and-forward gossip network:

- **Flood + dedup.** Each envelope has a unique id; a phone processes an id once and drops
  repeats, so messages spread everywhere without looping forever.
- **TTL + path.** Every envelope carries a hop budget and the list of nodes it has passed
  through; relays decrement the budget and refuse to forward back where it came from.
- **Store-and-forward.** Recently seen envelopes are cached and replayed to any phone that
  connects *later*, so a message can reach someone who was out of range when it was sent — as
  long as some carrier phone eventually drifts near them.

### 3. Identity & security — so relays can't read your messages
`KeyManager` (built on **Google Tink**) gives every install two keypairs, stored encrypted by
the Android Keystore:

- an **Ed25519 signing** key — every envelope is signed, so forgeries and tampering are dropped,
  and a phone can't spoof someone else's node id (the id is derived from the signing key);
- an **X25519 / HPKE hybrid encryption** key — directed messages (likes, matches, chats) are
  **end-to-end encrypted** to the recipient, so the phones relaying them see only ciphertext.

Public profile beacons are deliberately plaintext — they're meant to be seen by everyone nearby.

### 4. The dating flow
Public **profile beacons** flood the mesh so you can discover people. Swiping right sends an
encrypted **LIKE**; when two people have liked each other a **MATCH** is recorded on both phones
and chat unlocks. **CHAT** messages are end-to-end encrypted and acknowledged with a delivery
receipt (**ACK**) that travels back through the mesh. All of this is persisted in **Room** so it
survives coming in and out of range.

---

## Project layout

```
proximity-dating-app/
├── app/src/main/java/com/holidate/app/
│   ├── crypto/         KeyManager — identity, signing, E2E encryption (Tink)
│   ├── mesh/           MeshTransport / NearbyMeshTransport — radio layer
│   │                   MeshMessage / MeshPayloads — signed envelope + codecs
│   │                   MessageRouter — dedup, verify, decrypt, relay, store-and-forward
│   │                   MeshController / MeshForegroundService — lifecycle + background service
│   ├── data/           Room entities, DAOs, database, and the HoliDateRepository state machine
│   ├── viewmodel/      MainViewModel, ChatViewModel
│   ├── ui/             Compose theme, screens (onboarding, discover, matches, chat, profile),
│   │                   components, permissions gate, navigation
│   ├── AppContainer.kt Manual dependency wiring
│   └── HoliDateApp.kt  Application
└── app/src/main/res/   strings, theme, launcher icon
```

## Build & run

Requirements: **Android Studio** (Ladybug or newer) and the **Android SDK** (compileSdk 34).

```bash
# From this directory, with the Android SDK available (set sdk.dir in local.properties
# or ANDROID_HOME), assemble the debug APK:
./gradlew :app:assembleDebug

# Or just open the proximity-dating-app/ folder in Android Studio and press Run.
```

### Release (signed) build

The release build is signed only when signing details are supplied via environment variables —
nothing secret is ever committed:

```bash
RELEASE_STORE_FILE=/path/to/your.jks \
RELEASE_STORE_PASSWORD=... \
RELEASE_KEY_ALIAS=... \
RELEASE_KEY_PASSWORD=... \
./gradlew :app:assembleRelease
```

Without those variables, `assembleRelease` still succeeds but produces an *unsigned* APK. CI
builds a signed release APK using a throwaway key generated on the runner and publishes it as the
`holidate-release-apk` artifact; for a real Play Store release, sign with your own upload key.

Install the resulting APK on **two or more physical Android phones** (Nearby Connections needs
real Bluetooth/Wi-Fi radios — the emulator can't do proximity), grant the nearby-device
permissions, create a profile on each, and they will discover each other within range.

> The Gradle wrapper is committed. Dependencies (AGP, Compose, Tink, Nearby, Room) download from
> Google's and Maven Central's repositories on first build.

## Permissions

Nearby Connections needs different permissions across Android versions; `PermissionsGate`
requests the right set at runtime:

| Android version | Runtime permissions requested |
|---|---|
| 12+ (API 31)  | `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` |
| 13+ (API 33)  | + `NEARBY_WIFI_DEVICES`, `POST_NOTIFICATIONS` |
| below 13      | `ACCESS_FINE_LOCATION` (required for BLE discovery) |

A foreground service (`MeshForegroundService`) keeps the mesh alive — discovering and relaying —
while the app is backgrounded, with an ongoing notification as Android requires.

## Known limitations / roadmap

This is an MVP focused on proving out the mesh. Deliberately not yet done:

- **Photos in beacons.** Profiles support a photo thumbnail, but onboarding currently sends none.
  Nearby `BYTES` payloads cap at 32 KB, so real photos should move to chunked/`STREAM` payloads.
- **Age verification & safety.** Onboarding enforces 18+ locally only. A real dating app needs
  proper verification, blocking, reporting, and abuse mitigation before any public release.
- **Spam / Sybil resistance.** Signatures stop impersonation, but nothing yet rate-limits a
  flood of freshly-generated identities. Proof-of-work or reputation would help.
- **Location/distance.** "Nearby" is currently radio range, not GPS distance. A coarse,
  privacy-preserving distance hint could be added.
- **Delivery guarantees.** Gossip + store-and-forward is best-effort; there's no end-to-end
  retry/queue for messages to people who never come back in range.
- **Tests.** Unit tests for the router (dedup, TTL, relay) and crypto round-trips are the
  obvious next addition.

## Privacy note

By design, HoliDate keeps everything peer-to-peer: profiles and messages travel only between
phones over local radios, never through a server. Private messages are end-to-end encrypted so
relaying phones cannot read them. Profile beacons, however, are public to everyone in range —
don't put anything in a beacon you wouldn't want a stranger nearby to see.
