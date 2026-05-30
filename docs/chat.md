# Chat (Global, Pretty, Translation) Audit

## Findings

### Security

- **CRITICAL** `ChatStreamClient.java:82-86` — SSE stream uses `AuthService.getInstance().refreshTokenIfNeeded().get()` which blocks the stream thread; if auth fails, entire chat stream fails
- **CRITICAL** `GeminiTranslationProvider.java` — API key stored in `Core.settings` as plain string (`GEMINI_API_KEY`)
- **CRITICAL** `DeepLTranslationProvider.java` — API key stored in `Core.settings` as plain string (`DEEPL_API_KEY`)
- **HIGH** `ChatTranslationFeature.java:56-57` — Packet replacement: `SendMessageCallPacket` and `SendMessageCallPacket2` are replaced with translated variants; all outgoing messages go through translation provider — if provider fails, messages may be blocked or sent without translation
- **MEDIUM** `ChatApiClient.java:76` — No input sanitization on message content before sending to API
- **MEDIUM** `ChatConfig.java:80-83` — `x()/y()` methods use `collapsed()` value to decide which key to read — but `collapsed()` itself reads from settings, creating indirect coupling

### Concurrency

- **HIGH** `ChatService.java:50` — `Timer.schedule(this::connectStream, 0, 60)` — reconnects every 60 seconds regardless of connection state; if already connected, creates duplicate stream threads
- **HIGH** `ChatStreamClient.java:55-60` — `disconnect()` sets `isStreaming` flag and interrupts thread; race between `streamThread.interrupt()` and `runStreamLoop` re-checking the flag
- **MEDIUM** `ChatStateManager.java:82` — `updateState` uses `updateState` method from API client which runs on HTTP thread; `previousState` and `currentState` are volatile-like but not synchronized
- **MEDIUM** `ChatService.java:60` — `fetchChannelsAndCurrentMessages` chains async operations; if channel fetch succeeds but messages fail, channel state may be partially applied

### Anti-Patterns

- **MEDIUM** `ChatConfig.java` — Static utility class with all methods static; no interface, hard to mock or test
- **MEDIUM** `ChatApiClient.java:112-114` — Uses arc's `Json` parser (not Jackson) for parsing chat API responses; `parseJson` creates new `Json()` instance per call instead of reusing
- **MEDIUM** `ChatTranslationFeature.java:56-57` — Packet replacement in `init()` may replace packets before `Main.init()` completes; timing-dependent
- **HIGH** `ChatTranslationFeature.java:45-50` — `handleClient()` in translated packets calls `NetClient.sendMessage()` directly — bypasses Mindustry's own message validation/queuing
- **MEDIUM** `PrettyChatFeature.java` — Entirely commented-out in `Main.java` registration; dead code still compiled and maintained
- **MEDIUM** `ChatStreamClient.java:95-97` — SSE event parsing: dispatches last event even if connection closes mid-event
- **MEDIUM** `ChatStateManager.java:65-68` — Syncs state every 60 seconds + on state change; if state changes rapidly, queued syncs may fire outdated state

### Dead Code / Tech Debt

- **MEDIUM** `ChatApiClient.java:115` — `parseJson` uses arc's Json parser while most other code uses Jackson; inconsistent serialization
- **MEDIUM** `ChatTranslationFeature.java:52` — Providers list: `GeminiTranslationProvider` and `DeepLTranslationProvider` are added but may not work without explicit config; no graceful degradation when API key missing
- **LOW** `ChatStateManager.java` — Many state constants defined but some not used in all paths (`EDITOR_STATE` is handled in `handleWorldLoaded` but not in `handleStateChange`)

### Performance

- **MEDIUM** `ChatStreamClient.java` — SSE stream read loop blocks a dedicated thread; thread is daemon so doesn't prevent JVM shutdown, but reconnection delay is fixed 5s regardless of failure type
- **LOW** `ChatConfig.java` — Frequent `Core.settings.getFloat/Bool` calls on every read; no caching of frequently-accessed values
