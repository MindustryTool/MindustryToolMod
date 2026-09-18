## 1. SSE Stream Health & Watchdog in ChatService

- [ ] 1.1 Implement `lastEventTime` timestamp tracking in `ChatService.handleStreamLine` for all incoming stream lines (heartbeat `:heartbeat`, `event: heartbeat`, `Connected`, and data payloads)
- [ ] 1.2 Implement periodic 45-second heartbeat watchdog timer in `ChatService` that terminates stalled zombie `streamRequest` connections and triggers `scheduleReconnect()`
- [ ] 1.3 Update connection state lifecycle so `store.session().setConnected(true)` is only asserted once the server handshake or event is received, and set to `false` when stalled or disconnected
- [ ] 1.4 Add catch-up synchronization in `ChatService` on successful stream reconnection to fetch active channel messages and channel status

## 2. Background and Collapsed State Synchronization

- [ ] 2.1 Add silent background catch-up method (`syncActiveChannelSilently`) in `ChatService` that updates active messages and unread counts without setting `loadingInitial = true` if messages are already cached
- [ ] 2.2 Wire `collapsedConfig` transition from `true` to `false` in `ChatFeature` to trigger silent background catch-up and verify live stream connection
- [ ] 2.3 Update manual refresh in `ChatService.refresh` and `ChatOverlayHudView` to perform stream liveness verification and force reconnect if stalled

## 3. Verification & Testing

- [ ] 3.1 Add unit tests in `mod/src/test/java/mindustrytool/features/chat/` covering watchdog timeout triggers, reconnection catch-up, and silent sync behavior
- [ ] 3.2 Run test suite (`./gradlew test`) to ensure all tests pass
- [ ] 3.3 Verify Java 8 standard library API compatibility and project rule compliance
