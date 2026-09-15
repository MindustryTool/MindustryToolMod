## Why

The legacy `ChatStateManager` (old/ folder) is the only component that publishes the player's game presence (`menu`, `server: X`, `player-connect: Y`, …) via `PUT /chats/users/state`. The rewritten chat stack has no presence sync at all — `MindustryTool.updateChatState()` exists but has zero callers — so every new client appears permanently offline/stale to other users.

## What Changes

- Reintroduce outbound presence sync in the new Signal architecture, keeping the legacy wire strings byte-identical (no backend change).
- Hold presence in `ChatSession` as reactive signals: current presence plus sticky last-non-menu presence that survives transient `menu` flicker (pause menu, dialogs).
- Decouple PlayerConnect from Chat via Mindustry events (`PcRoomOpened` / `PcRoomClosed` fired by `PlayerConnectFeature`, observed by the chat-side sync; no cross-feature imports).
- Coalesce rapid changes with 1s debounce (last-write-wins) plus distinct-until-changed; resolve out-of-order `pingHost` replies with an intent generation counter.
- Keep a 5-minute heartbeat re-PUT to heal silent server-side drift.
- Gate sync on logged-in + opt-in only (REST works without the SSE stream); stream state is not required.
- Add a `share-presence` opt-out setting in chat settings, enabled by default; presence sync runs whenever logged in, even if the Chat feature/overlay is disabled.
- On opt-out or logout, stop PUTs without sending a final state (accepted: server keeps the last known presence).

## Capabilities

### New Capabilities

- `chat-presence`: outbound game-presence publishing (sticky state resolution, event-driven intents, debounced + heartbeat sync, opt-out setting).

### Modified Capabilities

- None. The existing `chat` spec covers overlay/store/messaging; presence is a separate capability.

## Impact

- `features/chat/state/ChatSession.java`: new presence signals.
- Chat-side sync listener (lifecycle owner TBD in design): Mindustry event observers, debounce timer, `MindustryTool.updateChatState()` reuse.
- `features/playerconnect/PlayerConnectFeature.java`: fire `PcRoomOpened` / `PcRoomClosed` with resolved room name; no Chat imports.
- `features/chat/ChatFeature.java` config + `ChatSettingsView` + bundle keys for the opt-out toggle.
- Backend contract unchanged (`PUT /chats/users/state`); wire strings unchanged.
