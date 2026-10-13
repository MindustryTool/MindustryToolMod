## Why

`AGENTS.md` tells agents what to build but nothing about what it costs at runtime. An agent adding a feature has no reason to know that this mod targets Android under Mindustry's software renderer, or that a `Trigger.update` handler registered in a constructor keeps dispatching for the whole process lifetime even when the feature is off.

An investigation of `mod/src/mindustrytool` (2026-10-13) found:

**Listener lifecycle**

```
Grep: Events\.(off|remove|removeListener)  across mod/src/mindustrytool
  └─ features/godmode/MapPositionPicker.java:72      ← self-managed one-shot, correct
  └─ features/translation/ui/LanguageDropdown.java:207 ← inside onDispose(), correct
```

Two removals exist. Neither belongs to a `Feature`. Approximately 28 `Trigger.*` handlers and 40 `Events.on(...)` handlers are registered in constructors, which run once from `Main.init()` → `FeatureManager.register(...)`, and are never unregistered. Most guard their body with `if (!isEnabled()) return;`, so the work is skipped but the dispatch is not — on every frame, for every disabled feature, for the life of the process.

**Genuine leaks (unbounded growth, not just dispatch overhead)**

| Site | Growth |
| --- | --- |
| `components/SettingKeybindRow.java:124` | `Events.run(Trigger.update, this::onUpdate)` on every `startListening()`; `cancelListening()`/`dispose()` never remove it. Every keybind row click adds a permanently-dispatched handler. |
| `features/chat/ChatMessageParser.java:36` | `static ConcurrentHashMap<String, ParsedChatMessage> CACHE`, never evicted. `clearCache()` exists at `:141` and is called from nowhere. |
| `features/chat/ChatMessageHeightCalculator.java:50` | `static ConcurrentHashMap<String, Float> HEIGHT_CACHE`, never evicted. `clearCache()` exists at `:190` and is called from nowhere. |
| `solim-core/.../graphics/NetworkImage.java:37` | `static ConcurrentHashMap<String, TextureRegionDrawable>`, one entry per distinct url × radius × width, holding GL textures. `clearCache()` at `:67` is called from nowhere. |
| `features/chat/ChatPresence.java:76` | `Timer.schedule(this::heartbeat, 300f, 300f)` repeat task, handle discarded, never cancellable. |

The two chat caches and `NetworkImage.cache` exist *specifically to avoid per-frame allocation* — they are the reason `ChatMessageListView` does not re-measure and re-parse on every scroll. Removing them would be a performance regression. They need bounds, not deletion.

**Per-frame allocations that survive the enabled guard**

| Site | Allocation |
| --- | --- |
| `features/camerazoom/CameraZoomFeature.java:117-125` | `Float` boxing ×2 from `signal().peek()` plus `Core.settings.getFloat` ×2, every frame |
| `features/blockstats/BlockStatsDisplayFeature.java:276-278` | `Float` boxing ×3 per frame |
| `features/togglerendering/ToggleRenderingFeature.java:127` | lambda capture per frame; reflective field scan and `removeAll` per frame when blocks hidden |
| `features/autoplay/AutoplayFeature.java:483` | string concatenation (`"2.4s"`) per frame |
| `features/freecamera/FreeCameraFeature.java:120` | `Core.bundle.get(...)` dictionary lookup every frame |
| `features/music/MusicFeature.java:87-101` | `Vars.control.sound.getCurrent()` every frame with **no** `isEnabled()` guard |

The project already has the tooling to catch this class of bug — `RenderLoopAllocationTest` asserts zero heap allocation for `VisualizerFeature`, `RangeDisplayFeature`, and `HealthBarFeature` via `ThreadMXBean.getThreadAllocatedBytes`. That bar exists for three features and is unwritten down anywhere an agent would read it.

**Net effect:** the rules that would prevent these defects are implicit in three files (`RenderLoopAllocationTest`, `docs/solim_internals.md`, `disposal-verification/spec.md`) and are not stated as constraints. An agent has to rediscover them by reading implementation code.

## What Changes

- **A `Performance & Memory Discipline` section in `AGENTS.md`** — prescriptive patterns, placed alongside the existing mandatory-rule sections so it is read before code is written, not discovered afterward. Covers: disabled-feature gating for frame work, listener ownership (construction-time vs. `onEnable`/`onDisable` vs. Solim `Component.listen`), bounded collections, zero-allocation frame loops, and the canonical HUD mount/unmount lifecycle.
- **A `Known Debt` table inside the change** recording every current violation, so the guideline's "no new violations" clause is measurable and the count can only decrease.
- **Fix the clear leaks** so the guideline is true on day one rather than aspirational:
  - `SettingKeybindRow` — remove the `Trigger.update` handler on cancel/dispose, or register it once and gate on `listeningActive`.
  - `ChatMessageParser.CACHE` and `ChatMessageHeightCalculator.HEIGHT_CACHE` — bounded LRU eviction.
  - `NetworkImage.cache` — bounded LRU eviction over GL textures.
  - `ChatPresence` heartbeat — retain the `Timer.Task` handle and cancel it when presence sharing stops.
- **A `Performance & Memory` block in the existing `Before Completing Any Task` checklist**, so the rules are enforced at review time and not only at authoring time.

Explicitly **out of scope**: moving the ~68 constructor-registered listeners into `onEnable`/`onDisable`. That is a large, risky refactor for a benefit (skipping dispatch on disabled features) that is real but modest. It stays in Known Debt.

## Capabilities

### New Capabilities

- `agent-perf-discipline`: Behavioral constraints on feature lifecycle, listener ownership, frame-loop allocation, and collection bounding, stated for AI agents authoring or modifying features in this mod.

### Modified Capabilities

<!-- None -->

## Impact

- **Docs**: `AGENTS.md` gains one mandatory-rule section and two checklist blocks. No existing rule is modified or removed.
- **Mod runtime**: `SettingKeybindRow`, `ChatMessageParser`, `ChatMessageHeightCalculator`, `ChatPresence`, and `NetworkImage` change behavior. The cache changes are bounded-eviction only — entries still exist, they are now capped.
- **No public API change.** `clearCache()` methods keep their signatures and remain callable.
- **No user-visible text.** The changes are internal lifecycle and caching, so no `bundle.properties` keys are added.
- **Java 8 runtime**: eviction must use `LinkedHashMap` with `removeEldestEntry` access-order, or an existing included library. `java.util.function` and Java 9+ collection factories are already forbidden by `AGENTS.md` and remain so.
- **Risk**: `NetworkImage.cache` holds `TextureRegionDrawable` values. Eviction drops the Java-side reference; disposal of the underlying texture is deliberately **not** performed, matching current disk-cache and style-cache behavior, to avoid disposing a texture Arc may still be drawing.