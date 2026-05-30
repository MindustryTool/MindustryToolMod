# God Mode Audit

## Findings

### Security

- **CRITICAL** `JSGodModeProvider.java:19-20` — Sends `/js` commands via `Call.sendChatMessage()`; this is visible in server chat to ALL players — exposes god mode commands publicly
- **CRITICAL** `JSGodModeProvider.java:41` — `Groups.player.find(p => p == "@")` — the JS format `"@".equals(p.name)` — this JS code compares player name with `"@"` literal which is wrong. The actual formatted string would be like `Groups.player.find(p => p.name == "PlayerName")` — potentially non-functional or could match wrong player
- **HIGH** `InternalGodModeProvider.java:46-48` — `tile.setNet(coreBlock, team, 0)` places core at any position without validation — can overwrite existing structures
- **HIGH** `InternalGodModeProvider.java:32-34` — `team.core().items.add(item, amount)` adds items directly to core without overflow checks
- **MEDIUM** `GodModeFeature.java:48-50` — Provider switching runs every 60 seconds via Timer AND on PlayEvent/StateChangeEvent — redundant periodic checks

### Concurrency

- **LOW** `GodModeFeature.java:57-60` — `provider` field updated from Timer thread; read in `rebuild()` which is called from UI thread

### Anti-Patterns

- **HIGH** `GodModeDialogs.java` — Many dialog classes with near-identical patterns (selection, config, confirmation); high code duplication
- **MEDIUM** `GodModeFeature.java:22-24` — `GodModeFeature extends Table implements Feature` — stateful UI mixed with feature logic
- **MEDIUM** `JSGodModeProvider.java:17` — `isAvailable()` checks `Vars.player.admin` — only works if player has admin on remote server; silently disables feature with no user feedback
- **MEDIUM** `PositionSelector.java` — Uses `TapListener.getInstance().select(...)` to capture click position; coupling to global tap listener service

### Dead Code / Tech Debt

- **MEDIUM** `GodModeFeature.java:34-36` — Provider switching in `switchProvider()` prefers JS over Internal — even in singleplayer where JS won't be available
- **LOW** `GodModeFeature.java:61-66` — `onEnable` inserts self into `waves/editor` UI element; if element is missing, silently logs error and continues
