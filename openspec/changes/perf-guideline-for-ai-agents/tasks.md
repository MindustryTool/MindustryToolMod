## 1. Phase 1: Fix the clear leaks

- [ ] 1.1 Fix the `SettingKeybindRow` listener leak in `mod/src/mindustrytool/components/SettingKeybindRow.java` — register the `Trigger.update` handler once (at construction or first activation) and gate on `listeningActive`, or remove it in `cancelListening()` and `dispose()`, so repeated start/stop does not accumulate live handlers
- [ ] 1.2 Add a bounded LRU eviction policy to `ChatMessageParser.CACHE` in `mod/src/mindustrytool/features/chat/ChatMessageParser.java` — Java 8 `LinkedHashMap` with `removeEldestEntry`, or an equivalent bounded structure; preserve the existing `clearCache()` signature
- [ ] 1.3 Add a bounded LRU eviction policy to `ChatMessageHeightCalculator.HEIGHT_CACHE` in `mod/src/mindustrytool/features/chat/ChatMessageHeightCalculator.java`; preserve `clearCache()`
- [ ] 1.4 Add a bounded LRU eviction policy to the in-memory `NetworkImage.cache` in `solim-core/src/solim/graphics/NetworkImage.java` — eviction drops the Java-side reference and MUST NOT dispose the underlying texture; preserve `clearCache()`
- [ ] 1.5 Retain the `Timer.Task` handle for the `ChatPresence` heartbeat in `mod/src/mindustrytool/features/chat/ChatPresence.java` and cancel it when presence sharing stops or the owning feature is disabled
- [ ] 1.6 Release `PlayerConnectFeature`'s `joinInjector` in `onDisable()` — null the field and remove its `ResizeEvent` listener so re-enabling does not stack injectors
- [ ] 1.7 Add a test asserting each bounded cache stays at its maximum after over-insert, that eviction preserves recently used entries, and that `SettingKeybindRow` start/stop cycles leave no additional live `Trigger.update` handlers

## 2. Phase 2: Write the agent guideline in `AGENTS.md`

- [ ] 2.1 Add a `Performance & Memory Discipline` section to `AGENTS.md`, placed with the other mandatory-rule sections, covering: disabled-feature gating for frame triggers; listener and timer ownership with the three prescribed teardown options; the HUD mount/unmount lifecycle including null-before-post; bounded collections; and zero-allocation frame loops with the `Tmp`/pre-allocated-field convention
- [ ] 2.2 State explicitly that constructor-time listener registration is permitted for process-lifetime objects provided the handler still gates on `isEnabled()`, so agents do not treat the rule as a ban
- [ ] 2.3 Document that caches preventing per-frame allocation must be bounded, not removed
- [ ] 2.4 Reference the existing zero-allocation test (`RenderLoopAllocationTest`) as the standard for new frame-loop features
- [ ] 2.5 Append a `Performance & Memory` checklist group to `Before Completing Any Task` covering disabled-feature gating, listener/timer teardown, bounded collections, allocation-free frame loops, and HUD dispose on disable
- [ ] 2.6 Include the Known Debt table so a contributor can audit the codebase against the constraints and see current outstanding violations with file:line references

## 3. Phase 3: Verify

- [ ] 3.1 Run `:mod` build and the new leak tests
- [ ] 3.2 Confirm no `java.util.function` or Java 9+ collection factory was introduced by the eviction changes (existing `AGENTS.md` Java 8 rule)
- [ ] 3.3 Confirm no `bundle.properties` key was added or removed (no user-visible text changed)
- [ ] 3.4 Confirm `clearCache()` signatures on all three caches are unchanged

## Known Debt (recorded, not fixed by this change)

| ID | Site | Issue |
| --- | --- | --- |
| D7 | ~68 constructor-registered listeners across `mod/` | Dispatch continues on every frame while the feature is disabled |
| D8 | `features/camerazoom/CameraZoomFeature.java:117-125` | `Float` boxing ×2 plus `Core.settings.getFloat` ×2 per frame |
| D9 | `features/blockstats/BlockStatsDisplayFeature.java:276-278` | `Float` boxing ×3 per frame |
| D10 | `features/togglerendering/ToggleRenderingFeature.java:127` | Lambda capture, reflection, and `removeAll` per frame |
| D11 | `features/autoplay/AutoplayFeature.java:483` | String concatenation per frame |
| D12 | `features/freecamera/FreeCameraFeature.java:120` | `Core.bundle.get(...)` per frame |
| D13 | `features/music/MusicFeature.java:87-101` | Frame work with no `isEnabled()` guard |
| D14 | `features/logicvisualizer/LogicVisualizerFeature.java:104` | Iterates all units with no frustum culling |
| D15 | `features/chat/state/ChatMessages.messages`, `ChatUsers.users` | Grow without trimming |