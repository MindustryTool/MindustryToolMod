## Context

The legacy `PlayerConnect` feature enabled Mindustry players to host and join multiplayer games through external proxy relay servers (using the CLA-J protocol version 159) without needing port forwarding. However, the original code in `old/` suffered from several issues:
- Massive static mutable state in `PlayerConnect` with background threads (`roomThread`, `pingerThread`, `worker`) created outside structured lifecycles.
- Direct use of `Http.get()`, violating `AGENTS.md` rules requiring `mindustrytool.services.Request` or `MindustryTool`.
- Imperative Arc UI dialogs (`BaseDialog`) and fragile mutation of Mindustry's `JoinDialog.hosts` table.
- Intrusive modal popups when guests joined while the host was playing.
- Lack of real-time room synchronization; rooms were only fetched via manual clicks or polling.

The backend now supports `GET /player-connect/sse` streaming real-time room changes. This rewrite establishes a clean Solim-based architecture, encapsulates the CLA-J relay network engine, connects to the SSE stream continuously in the background, and replaces intrusive modals with playable HUD overlays.

## Goals / Non-Goals

**Goals:**
- **Encapsulated Network Engine**: Maintain exact byte-level wire compatibility with CLA-J protocol v159 while isolating threads and socket lifecycles inside `net/` sub-packages owned by `PlayerConnectFeature`.
- **API & Real-Time Sync**: Add `MindustryTool.playerConnectStream()`, `getPlayerConnectRooms()`, and `getPlayerConnectProviders()`. Maintain a background SSE connection streaming room updates into a reactive `Signal<List<PlayerConnectRoom>>`.
- **Declarative Solim UI**:
  - Seamlessly inject a Solim room browser component into `Vars.ui.join` with real-time SSE updates, mod conflict badges, and link-join dialog.
  - Provide declarative `HostRoomDialog` and `ManageRoomDialog`.
  - Provide a non-intrusive top-of-screen Solim HUD banner for host join approvals.
  - In-game HUD relay ping indicator when hosting.
  - Pause menu integration for "Host Room" / "Manage Room".
- **i18n**: Fully translatable UI text with descriptive comments in `bundle.properties`.

**Non-Goals:**
- Modifying the remote CLA-J relay protocol or server implementation.
- Automatic installation or downloading of missing mods from mod repositories (out of scope; only detection and prompt to disable unneeded mods).
- Support for alternate relay protocols other than CLA-J v159.

## Decisions

### 1. Network Protocol Isolation & Lifecycle Ownership
- **Decision**: Keep the CLA-J v159 packet definitions (`Packets.java`) and `NetworkProxy.Serializer` for wire compatibility, but remove all static singletons.
- **Rationale**: The relay server expects exact packet IDs (e.g. `Packets.id = -4`) and structure. However, making `NetworkProxy` an instantiable, owned component managed by `PlayerConnectFeature` ensures clean teardown when the feature is disabled, games are reset, or the client connects to another server.
- **Alternatives Considered**: Rewriting protocol using WebSockets or raw TCP without CLA-J framing was rejected because the existing backend relay infrastructure relies on CLA-J v159.

### 2. Real-Time Room Directory via Continuous SSE Stream
- **Decision**: Connect to `GET /player-connect/sse` in the background when `PlayerConnectFeature` is enabled, streaming `{ rooms: List<PlayerConnectRoom> }` into `Signal<List<PlayerConnectRoom>>`. If SSE fails or disconnects, retry with exponential backoff and fallback to REST `GET /player-connect/rooms`.
- **Rationale**: Matches the user's decision to maintain background real-time presence. The reactive signal automatically updates any open Solim UI components without manual polling.
- **Alternatives Considered**: Polling on timer was rejected in favor of the new backend SSE capability. Connecting SSE only when `JoinDialog` is open was considered, but continuous background sync was specifically requested by the user.

### 3. Solim UI Injection into `Vars.ui.join`
- **Decision**: Cleanly attach a Solim `RoomBrowserView` into `Vars.ui.join.hosts` using an isolated container cell, rather than recursively manipulating and clearing all vanilla children.
- **Rationale**: Preserves vanilla server search and discovery while adding the collapsible PlayerConnect room section at the top, respecting Solim's declarative component model and reactive bindings.
- **Alternatives Considered**: Separate dedicated dialog was considered (Option B in exploration), but Option A was selected by user to maintain familiar access from the Join Game screen.

### 4. Non-Intrusive Solim HUD Join-Approval Banner
- **Decision**: Replace `Vars.ui.showCustomConfirm()` modal dialog with a top-screen Solim HUD banner `JoinApprovalHudView` when `autoAccept == false`.
- **Rationale**: An intrusive modal dialog interrupts active gameplay (e.g. while defending a wave). A HUD banner allows the host to keep controlling their unit and press `[Accept]` or `[Reject]` when safe.

### 5. Provider Management (Public, LocalHost, Custom)
- **Decision**: Fetch public providers via `MindustryTool.getPlayerConnectProviders()`, prepend/append `LocalHost (localhost:11010)`, and persist user-added custom providers in `Core.settings`.
- **Rationale**: Meets user requirement to maintain flexibility for self-hosted or private relay servers.

## Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PlayerConnectFeature                           │
│  - Signal<HostingState> state                                          │
│  - Signal<Integer> ping                                                │
│  - Signal<List<PlayerConnectRoom>> rooms                               │
│  - Signal<List<PlayerConnectProvider>> providers                       │
│  - Queue<JoinRequest> pendingRequests                                  │
└──────────────┬──────────────────────────┬──────────────────────────────┘
               │                          │
       ┌───────▼────────┐         ┌───────▼────────┐
       │ Network Engine │         │  API & Stream  │
       │                │         │                │
       │ - NetworkProxy │         │ - MindustryTool│
       │ - ProxyProvider│         │   .player-     │
       │ - Packets      │         │   ConnectStream│
       │ - Serializer   │         │ - SSE Listener │
       └───────┬────────┘         └───────┬────────┘
               │                          │
       ┌───────▼──────────────────────────▼────────┐
       │                Solim UI                   │
       │                                           │
       │ - RoomBrowserView (in Vars.ui.join)       │
       │ - HostRoomDialog & ManageRoomDialog       │
       │ - JoinRoomDialog & JoinWarningDialog      │
       │ - JoinApprovalHudView (HUD banner)        │
       │ - PingHudView (in-game ping indicator)    │
       └───────────────────────────────────────────┘
```

## Risks / Trade-offs

- **[Risk] Relay connection drops or timeout during game**:
  - *Mitigation*: `NetworkProxy` detects disconnect and emits `RoomCloseReason`. `PlayerConnectFeature` cleanly notifies the host, resets `HostingState` to `DISCONNECTED`, and restores vanilla `NetProvider`.
- **[Risk] SSE stream interruption or network blip**:
  - *Mitigation*: Reconnection with backoff; fallback initial REST fetch populates rooms immediately.
- **[Risk] Host accidentally IP-bans relay server**:
  - *Mitigation*: Retain `unbanProxyIp()` hook on ban events and room creation to ensure the shared relay address is never locked out.
- **[Risk] Vanilla UI updates or resizing breaks injection**:
  - *Mitigation*: Mount Solim component cleanly with `listen(ResizeEvent.class)` and attach to `Vars.ui.join.shown()` lifecycle.
