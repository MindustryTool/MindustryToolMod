## Why

When the chat overlay is collapsed into badge mode or the game window runs in the background, incoming messages from other players fail to be received or rendered in real time. The SSE stream stalls on half-open/dropped TCP connections without timing out, reconnection does not backfill missed messages, and expanding the chat badge does not trigger a synchronization with the server, forcing users to manually click the Refresh button to see new messages.

## What Changes

- **SSE Heartbeat Watchdog**: Add an active 45-second liveness watchdog timer to `ChatService` that tracks incoming data and heartbeat lines, terminating stalled zombie connections and triggering automatic reconnection if silent socket drops occur.
- **Accurate Connection State**: Only set `store.session().setConnected(true)` once a live heartbeat or event is received from the server, rather than prematurely before connection handshake completion.
- **Catch-Up Synchronization on Reconnect**: When the SSE stream reconnects after a disconnect, automatically trigger a background fetch of recent messages and channel status so messages sent during downtime are not lost.
- **Silent Background Catch-Up on Uncollapse**: When transitioning from collapsed to expanded state, automatically trigger a silent background fetch for the active channel's latest messages and channel metadata to ensure immediate freshness without jarring loading flickers.
- **Resilient Manual Refresh**: Ensure manual refresh performs a connection health check and reconnects the SSE stream if stalled or disconnected, in addition to reloading active messages.

## Capabilities

### New Capabilities
<!-- No new capabilities introduced -->

### Modified Capabilities
- `chat`: Introduce SSE heartbeat watchdog, auto-sync upon uncollapsing, and catch-up synchronization on reconnection to ensure reliable message delivery in background and collapsed states.

## Impact

- `mindustrytool.features.chat.ChatService`: Watchdog timer implementation, last-event timestamp tracking, catch-up REST synchronization on stream reconnect.
- `mindustrytool.features.chat.ChatFeature`: Trigger background catch-up fetch when expanding from collapsed state.
- `mindustrytool.features.chat.ChatOverlayHudView`: Ensure manual refresh and uncollapse flows invoke robust sync logic.
