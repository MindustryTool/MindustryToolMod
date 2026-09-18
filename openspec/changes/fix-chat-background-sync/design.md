## Context

The in-game chat feature relies on an SSE stream (`/chats/stream`) for real-time message updates and REST endpoints (`/chats?channelId=...` and `/chats/channels`) for loading message history and channel rosters. 

When the chat HUD is collapsed (`collapsedConfig` is true), `ChatOverlayHudView` swaps the entire expanded view (including `ChatMessageListView`) for `buildCollapsedBadge()` using Solim's `dynamic(...)`. If the game is minimized or left in the background, TCP connections often stall or drop silently without generating TCP reset packets. Because `Request` sets `conn.setReadTimeout(0)` for SSE streams, `BufferedReader.readLine()` blocks indefinitely on a dead socket. The client maintains `store.session().connected() == true` despite receiving no data. Even if a connection drop is detected, the reconnect loop only re-opens the stream without fetching missed messages. When uncollapsing, the UI merely marks the channel as read without fetching updates, leaving the user with stale messages until they click manual Refresh.

## Goals / Non-Goals

**Goals:**
- Detect zombie/stalled SSE stream connections within 45 seconds using an active heartbeat watchdog timer.
- Reliably terminate dead socket connections and trigger automatic reconnection.
- Automatically catch up on missed messages for the active channel and channel unread status whenever the stream reconnects.
- Silently synchronize the active channel's latest messages in the background when expanding from collapsed state, without showing full-page loader flickers.
- Provide accurate connection state indicators, only marking `connected = true` after the handshake/event arrives from the server.
- Ensure manual refresh performs a health check that revives dead streams.

**Non-Goals:**
- Changing server-side SSE protocols or heartbeat intervals.
- Replacing the SSE streaming mechanism with WebSockets or continuous aggressive REST polling.
- Adding complex multi-channel offline caching databases.

## Decisions

### Decision 1: 45-Second Heartbeat Watchdog in `ChatService`
- **Choice**: Track `lastEventTime = System.currentTimeMillis()` whenever any line (heartbeat comment `:heartbeat`, `event: heartbeat`, or data payload) is processed in `handleStreamLine`. Run a timer check every 5 seconds. If `System.currentTimeMillis() - lastEventTime > 45_000ms`, cancel the current stream request (`streamRequest.cancel(true)`), flag `connected = false`, and trigger `scheduleReconnect()`.
- **Rationale**: The server emits heartbeat events every 15–30 seconds. A 45-second window accommodates slight network jitter while detecting half-open dead sockets within 45 seconds rather than hanging forever.
- **Alternatives considered**:
  - *Setting `readTimeout(45000)` directly on `HttpURLConnection`*: May interfere with general `Request.BodyHandlers.ofLines()` implementations if other services use different streaming cadences. A client-side watchdog cleanly controls `CompletableFuture` cancellation.

### Decision 2: Catch-Up Synchronization on Reconnection
- **Choice**: When a stream connection is successfully established and the first event/heartbeat is received, trigger a catch-up fetch for the active channel (`loadMessages(activeId)`) and refresh channel metadata (`refreshChannels()`).
- **Rationale**: Any messages sent during the disconnection downtime (e.g. while sleeping, network blip, or reconnect delay) are immediately fetched and merged via `ChatMessages.append`/`ChatMessages.replace`.
- **Alternatives considered**:
  - *Passing a `since` or cursor parameter to SSE*: The server's `/chats/stream` endpoint does not support historic message replays; catch-up via existing REST endpoints is reliable and backward-compatible.

### Decision 3: Silent Background Catch-Up on Uncollapse
- **Choice**: In `ChatFeature`, when `collapsedConfig` transitions from `true` to `false`, call `service.syncActiveChannelSilently(activeId)`:
  - If `!store.session().connected().peek()`, trigger `connectStream()`.
  - Fetch messages via `MindustryTool.getChatMessages(activeId, null)`.
  - Do NOT set `loadingInitial = true` if messages are already present in memory, avoiding view flicker.
  - Merge the fetched messages and update unread status once rendered.
- **Rationale**: Guarantees that opening the chat always displays up-to-date messages even if network events were delayed while the player was focused on gameplay.

### Decision 4: True Connection Lifecycle Indication
- **Choice**: Move `store.session().setConnected(true)` from `connectStream()` to `dispatchCurrentEvent()`, specifically when `Connected`, `heartbeat`, or data arrives.
- **Rationale**: Setting `connected = true` before the HTTP connection even opens causes false-positive green status indicators when the connection fails during establishment.

## Risks / Trade-offs

- **[Risk] High network traffic from frequent uncollapsing** → *Mitigation*: Background catch-up only queries the single active channel (50 items) and channels metadata. A short debounce (e.g. skip if synced within the last 5 seconds) prevents redundant requests on rapid toggle.
- **[Risk] Reconnection flood if backend goes down** → *Mitigation*: Reconnection uses a 5-second backoff thread, avoiding tight retry loops.
- **[Risk] Race condition between SSE incoming message and catch-up REST response** → *Mitigation*: `ChatMessages.append()` and `ChatMessages.replace()` deduplicate messages by ID; REST responses preserve existing confirmed messages.
