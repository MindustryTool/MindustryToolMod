# Design: Performance & Memory Discipline for AI Agents

## Context

The mod runs inside Mindustry, often on Android under a software GL backend. Two costs dominate: per-frame dispatch overhead and per-frame heap allocation. Both are invisible in a code review that only asks "does it work", and both accumulate across ~20 features that are individually reasonable.

`AGENTS.md` currently has strong mandatory sections for i18n, HTTP, Java 8 compatibility, nullability, imports, and ternary usage — but nothing about runtime cost. The "Before Completing Any Task" checklist covers translation, Java, architecture, and UI cleanup, but not performance.

The codebase itself is a mix. The HUD lifecycle is disciplined and consistent across 12 features. The frame-loop work is mostly well-written (three features already pass a zero-allocation test). The listener lifecycle is effectively ungoverned, and three caches grow without bound.

## Goals

- Make the existing good patterns (HUD lifecycle, zero-alloc draw) *stated constraints* rather than folklore.
- Make the bad patterns (unreleased listeners, unbounded caches) either impossible to repeat or visibly catalogued.
- Fix the defects that are unambiguously defects, so the guideline is true when an agent reads it.
- Do not require a risky refactor of working code to land.

## Non-goals

- Not a performance optimization pass. Boxing removal in `CameraZoomFeature` and `BlockStatsDisplayFeature` is catalogued, not fixed.
- Not a rewrite of listener registration. The ~68 constructor-registered listeners stay.
- Not a frame-time budget system. No per-feature millisecond targets.

## Decisions

### D1: Prescriptive patterns, not prohibitions

A rule of the form "never register `Events.on` outside `onEnable`/`onDisable`" would be violated in ~40 existing places on day one. An agent reading a rule that the codebase itself ignores learns that the rules are aspirational, which is worse than having no rule.

Instead, each requirement names the correct pattern for a situation and leaves legitimate room:

| Situation | Prescribed pattern |
| --- | --- |
| Listener a disabled feature should not receive | `onEnable` + `onDisable` |
| Listener owned by a Solim component | `Component.listen(...)` — removal is automatic |
| Process-lifetime listener on a once-constructed object | Constructor is acceptable; must still gate on `isEnabled()` |

**Rationale:** the third row matters. Constructor registration genuinely is correct for objects created once from `Main.init()`. A ban would push agents toward worse code.

### D2: Known Debt is a first-class artifact

The change ships a table of current violations:

| ID | Site | Class | In this change? |
| --- | --- | --- | --- |
| D1 | `SettingKeybindRow.java:124` | Listener leak, grows per click | **Fixed** |
| D2 | `ChatMessageParser.java:36` | Unbounded static cache | **Fixed** |
| D3 | `ChatMessageHeightCalculator.java:50` | Unbounded static cache | **Fixed** |
| D4 | `NetworkImage.java:37` | Unbounded texture cache | **Fixed** |
| D5 | `ChatPresence.java:76` | Uncancellable repeating timer | **Fixed** |
| D6 | `PlayerConnectFeature` `joinInjector` | Listener not released on disable | Fixed |
| D7 | ~68 constructor-registered listeners | Dispatch continues while disabled | Deferred |
| D8 | `CameraZoomFeature.java:117-125` | Boxing ×2 + settings read per frame | Deferred |
| D9 | `BlockStatsDisplayFeature.java:276-278` | Boxing ×3 per frame | Deferred |
| D10 | `ToggleRenderingFeature.java:127` | Lambda capture + reflection per frame | Deferred |
| D11 | `AutoplayFeature.java:483` | String concat per frame | Deferred |
| D12 | `FreeCameraFeature.java:120` | `Core.bundle.get` per frame | Deferred |
| D13 | `MusicFeature.java:87-101` | Frame work with **no** enabled guard | Deferred |
| D14 | `LogicVisualizerFeature.java:104` | Iterates all units, no frustum culling | Deferred |
| D15 | `ChatMessages.messages`, `ChatUsers.users` | Grow without trim | Deferred |

The spec requires this table to be enumerable and forbids *new* violations, but does not require the count to reach zero. That makes progress measurable without making the change blocking.

**Rationale:** D13 and D15 are genuine defects, not optimizations, but they sit outside "the clear leaks" the user scoped to. They are recorded so a future change can pick them up cleanly.

### D3: Bound the caches, do not delete them

All three caches are load-bearing. `ChatMessageHeightCalculator.HEIGHT_CACHE` is what stops `ChatMessageListView` from re-measuring glyph layouts on every scroll frame; deleting it would be a straight performance regression that happens to look like a memory fix.

Each already has a `clearCache()` that nothing calls. The fix is eviction, and eviction preserves the cache's purpose.

**Risk on `NetworkImage.cache`:** evicting drops the Java reference but must not dispose the texture. Arc may still be drawing it. This matches the existing posture for `StyleCache`, `RoundedCache`, and `Drawables.scalableCache`, all of which are excluded from static clearing in `disposal-verification/spec.md`.

### D4: No culling rule

Frustum culling is genuinely valuable — `LogicVisualizerFeature` iterating every unit is a real cost — but it is a *rendering quality* concern rather than a *memory* one, and `RenderFrustumCullingTest` already establishes the pattern for one feature. Adding a blanket culling mandate would expand the change well past its purpose. Recorded as D14 instead.

### D5: AGENTS.md section placement

The section goes with the other mandatory rules (after Java Compatibility, before Nullability or after Architecture), not in `docs/`. `AGENTS.md` is the file an agent loads at task start; `docs/solim_internals.md` is reference material it loads only when already stuck. The note that its `Disposable`/`Component` contract section is stale makes this more pressing, not less.

Checklist items are appended to the existing `Before Completing Any Task` groups rather than a new group, so the document's shape stays stable.

## Risks

| Risk | Mitigation |
| --- | --- |
| Agents treat the guideline as optional because most rules are unfollowed | Known Debt table makes non-compliance visible rather than hidden |
| Cache eviction causes a visible hitch when an evicted image is re-requested | Bounds set generously (chat caches: thousands; images: hundreds). Slower than unbounded, not slower than a memory-pressure death |
| `SettingKeybindRow` fix changes keybind-capture feel | The handler is `Trigger.update` gated on `listeningActive`; registering once at construction and gating preserves behavior exactly while removing the growth |
| Docs drift from code | Spec requires the table to be enumerable with file:line, so a stale entry is a visible defect |

## Migration Plan

Single change, no staging. The guideline lands with the leak fixes in the same change so the document is accurate immediately.

## Open Questions

- Should `RenderLoopAllocationTest` be generalized from three hardcoded features into a parameterized harness so new frame-loop features are covered by default? Deferred; would be a separate change.
- Should perf tooling (`Perf`, `UiProfiler`) get an in-game toggle? Currently reachable only from tests and the MCP `get_ui_perf_metrics` tool. A player-facing toggle needs i18n and a settings row, which is real scope.