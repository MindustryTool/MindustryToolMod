## 1. API Services & Models

- [x] 1.1 Add `MindustryTool.playerConnectStream()` returning SSE lines publisher for `GET /player-connect/sse`
- [x] 1.2 Add `MindustryTool.getPlayerConnectRooms()` and `MindustryTool.getPlayerConnectProviders()`
- [x] 1.3 Verify and extend `PlayerConnectRoom` and `PlayerConnectProvider` response models

## 2. Networking Engine (CLA-J v159)

- [x] 2.1 Migrate and clean `Packets.java` with explicit imports and Java 8 compatibility
- [x] 2.2 Migrate `PlayerConnectLink.java` for URI parsing and validation
- [x] 2.3 Implement instantiable `NetworkProxy` relay client with lifecycle management and ping tracking
- [x] 2.4 Implement `ProxyProvider` wrapping Mindustry's `NetProvider` for virtual connections
- [x] 2.5 Implement client-side connection hook for joining `player-connect://` targets with custom packet serialization
- [x] 2.6 Implement automatic proxy IP unban handler on player kick/ban events

## 3. Core Feature State & Background Services

- [x] 3.1 Implement `PlayerConnectFeature` with reactive signals (`state`, `ping`, `rooms`, `providers`) and configuration
- [x] 3.2 Implement background continuous SSE room synchronization with retry and fallback
- [x] 3.3 Implement provider management (public API providers, localhost, user-saved custom providers)
- [x] 3.4 Implement join request queue and auto-accept logic
- [x] 3.5 Implement pause menu button injection in `Vars.ui.paused`
- [x] 3.6 Implement HUD ping display overlay when hosting

## 4. Declarative Solim UI Views & Dialogs

- [x] 4.1 Implement `HostRoomDialog` for room setup and provider selection with live ping
- [x] 4.2 Implement `ManageRoomDialog` for active room management and link copying
- [x] 4.3 Implement `JoinRoomDialog` for link-based manual joining and password entry
- [x] 4.4 Implement `JoinWarningDialog` for mod mismatch detection and mod disabling
- [x] 4.5 Implement `JoinApprovalHudView` non-intrusive top-screen banner for host join approvals
- [x] 4.6 Implement `RoomBrowserView` Solim component for reactive room browsing and cards
- [x] 4.7 Inject `RoomBrowserView` and "Join via Link" button into Mindustry's `Vars.ui.join`

## 5. Internationalization & Final Integration

- [x] 5.1 Add all `feature.player-connect.*` translation keys and comments to `assets/bundles/bundle.properties`
- [x] 5.2 Remove development flag from `PlayerConnectFeature`
- [x] 5.3 Verify project builds cleanly with `./gradlew classes`
