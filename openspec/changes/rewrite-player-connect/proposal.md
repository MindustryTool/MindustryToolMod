## Why

The current `PlayerConnect` implementation is legacy code in `src/old/mindustrytool/features/playerconnect` relying on deprecated static state, raw `Http.get()` requests (violating project HTTP rules), imperative Arc dialogs, fragile direct tree manipulation in `Vars.ui.join`, and disruptive modal confirmation dialogs when players join. 

Rewriting `PlayerConnectFeature` brings it to modern Solim UI standards, establishes clean lifecycle and threading boundaries, adds real-time room discovery via a new Server-Sent Events (SSE) stream (`GET /player-connect/sse`), integrates non-intrusively with Mindustry's pause and join screens, and provides full internationalization.

## What Changes

- **Core Feature Lifecycle & State**:
  - Replace the static God-class `PlayerConnect` with `PlayerConnectFeature` owning reactive signals (`Signal<HostingState>`, `Signal<Integer> ping`, `Signal<List<PlayerConnectRoom>> rooms`, `Signal<List<PlayerConnectProvider>> providers`).
  - Clean lifecycle management: relay threads, pinger threads, and SSE streams cleanly stop and dispose when the feature is disabled or the app exits.

- **Real-Time Room Directory & SSE**:
  - Add typed `MindustryTool.playerConnectStream()` for `GET /player-connect/sse` streaming room directory updates in the background.
  - Add fallback/initial REST fetch `MindustryTool.getPlayerConnectRooms()` and `MindustryTool.getPlayerConnectProviders()`.
  - Support public providers, localhost (`localhost:11010`), and user-saved custom relay addresses.

- **Networking Engine (CLA-J Relay Protocol v159)**:
  - Migrate and clean `NetworkProxy`, `Packets`, `ProxyProvider`, and `PlayerConnectLink` to modern Java 8 compatibility and explicit import hygiene.
  - Wrap Mindustry's `ArcNetProvider` cleanly when hosting; unwrap on room closure or exit.
  - Retain room-join packet injection on the client side when connecting via `player-connect://` URLs.
  - Automatic unbanning of proxy relay server IPs to prevent host accidental bans of the relay server.

- **Declarative Solim UI**:
  - **Join Dialog Integration**: Seamlessly inject a Solim room browser section into `Vars.ui.join` with real-time SSE updates, mod conflict badges, and a "Join via Link" button.
  - **Host & Manage Dialogs**: Replace imperative `CreateRoomDialog` with declarative `HostRoomDialog` (server & config selection) and `ManageRoomDialog` (active room management, copy link, close room).
  - **Non-Intrusive Join Approval HUD**: Replace blocking modal confirmation dialog with a compact, playable Solim HUD banner at the top of the host's screen (`[Player "X" wants to join] [Accept] [Reject]`).
  - **In-Game Ping HUD**: Solim-driven HUD ping indicator when hosting.
  - **Pause Menu Button**: Clean integration with `Vars.ui.paused` for "Host Room" / "Manage Room".

- **Internationalization**:
  - Add complete translation keys under `feature.player-connect.*` in `assets/bundles/bundle.properties` with descriptive comments.

## Capabilities

### New Capabilities
- `player-connect`: Complete peer-to-peer relay networking (CLA-J v159), real-time SSE room directory synchronization, declarative Solim host/join dialogs, and non-intrusive host approval banner.

### Modified Capabilities
<!-- None -->

## Impact

- **Code Location**: `mod/src/mindustrytool/features/playerconnect/` replacing the stub in `mindustrytool/features/playerconnect/PlayerConnectFeature.java`.
- **API Services**: Adds `playerConnectStream()`, `getPlayerConnectRooms()`, and `getPlayerConnectProviders()` to `mindustrytool.services.MindustryTool`.
- **UI & Game Hooks**: Hooks into `Vars.ui.join` (room browser & link join), `Vars.ui.paused` (host button), and `Vars.ui.hudGroup` (ping & approval banner).
- **Network**: Handles custom packet serialization on client and server proxy channels.
