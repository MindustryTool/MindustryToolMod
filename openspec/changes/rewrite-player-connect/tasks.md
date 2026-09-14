## 1. API Services & Models

- [ ] 1.1 Add `MindustryTool.playerConnectStream()` returning SSE lines publisher for `GET /player-connect/sse`
- [ ] 1.2 Add `MindustryTool.getPlayerConnectRooms()` and `MindustryTool.getPlayerConnectProviders()`
- [ ] 1.3 Verify and extend `PlayerConnectRoom` and `PlayerConnectProvider` response models

## 2. Networking Engine (CLA-J v159)

- [ ] 2.1 Migrate and clean `Packets.java` with explicit imports and Java 8 compatibility
- [ ] 2.2 Migrate `PlayerConnectLink.java` for URI parsing and validation
- [ ] 2.3 Implement instantiable `NetworkProxy` relay client with lifecycle management and ping tracking
- [ ] 2.4 Implement `ProxyProvider` wrapping Mindustry's `NetProvider` for virtual connections
- [ ] 2.5 Implement client-side connection hook for joining `player-connect://` targets with custom packet serialization
- [ ] 2.6 Implement automatic proxy IP unban handler on player kick/ban events

## 3. Core Feature State & Background Services

- [ ] 3.1 Implement `PlayerConnectFeature` with reactive signals (`state`, `ping`, `rooms`, `providers`) and configuration
- [ ] 3.2 Implement background continuous SSE room synchronization with retry and fallback
- [ ] 3.3 Implement provider management (public API providers, localhost, user-saved custom providers)
- [ ] 3.4 Implement join request queue and auto-accept logic
- [ ] 3.5 Implement pause menu button injection in `Vars.ui.paused`
- [ ] 3.6 Implement HUD ping display overlay when hosting

## 4. Declarative Solim UI Views & Dialogs

- [ ] 4.1 Implement `HostRoomDialog` for room setup and provider selection with live ping
- [ ] 4.2 Implement `ManageRoomDialog` for active room management and link copying
- [ ] 4.3 Implement `JoinRoomDialog` for link-based manual joining and password entry
- [ ] 4.4 Implement `JoinWarningDialog` for mod mismatch detection and mod disabling
- [ ] 4.5 Implement `JoinApprovalHudView` non-intrusive top-screen banner for host join approvals
- [ ] 4.6 Implement `RoomBrowserView` Solim component for reactive room browsing and cards
- [ ] 4.7 Inject `RoomBrowserView` and "Join via Link" button into Mindustry's `Vars.ui.join`

## 5. Internationalization & Final Integration

- [ ] 5.1 Add all `feature.player-connect.*` translation keys and comments to `assets/bundles/bundle.properties`
- [ ] 5.2 Remove development flag from `PlayerConnectFeature`
- [ ] 5.3 Verify project builds cleanly with `./gradlew classes`
