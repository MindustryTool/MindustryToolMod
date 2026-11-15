## Context

The recent Team Resource Overlay overhaul introduced `SplitBar` (a unified power battery bar) and continuous state updating in `TeamResourceState`. Profiling and code analysis revealed rapid heap allocation during steady-state gameplay:
1. `SplitBar.draw()` runs 60 times/sec, generating 3+ `String` objects, string concatenations, and invoking Arc's `GlyphLayout.setText(...)` for each power graph every frame. When no power exists, it looks up bundle strings per frame.
2. `TeamResourceState.update()` runs 60 times/sec in `hud.element().update()`, using enhanced for-loops over Arc's `Seq<Item>` and `Seq<UnitType>`, which allocate `SeqIterator` instances on each pass.
3. Tooltip labels in `TeamResourceHudView` and `SplitBar` use `.update(...)` callbacks that continuously format strings even on frames where values have not changed.

Because this mod targets Android and desktop environments under Mindustry, continuous per-frame allocation creates garbage collection churn (sawtooth memory growth) and frame pacing stutter.

## Goals / Non-Goals

**Goals:**
- Eliminate all heap allocations in `SplitBar.draw()` during steady-state rendering (0 bytes allocated per frame).
- Eliminate all iterator allocations in `TeamResourceState.update()` by switching to indexed loops.
- Throttle item inventory and unit count scans to 30 ticks (0.5s), matching the existing rate and power update cycle.
- Retain exact visual layout, colors, compact formatting (`15k +1.2k`), and responsiveness of the HUD.
- Add an automated allocation test asserting zero bytes allocated during `SplitBar.draw()` and `TeamResourceHudView` steady-state updates.

**Non-Goals:**
- Removing inline text from `SplitBar` (user confirmed keeping the text layout).
- Restructuring the Solim reactive framework itself.
- Changing game logic or Mindustry's core data models.

## Decisions

### 1. Cached Precomputed Text & Layout in `SplitBar`

Instead of calling `getSectionText(graph)` and `layout.setText(...)` in `draw()` every frame:
- Maintain a local cache of formatted text or precomputed `GlyphLayout` per `PowerGraph`.
- In `SplitBar`, recompute the layout entries only when power metrics actually change (checked when `state` updates or on a 30-tick throttle), or compare integer/compact values before re-laying out.
- In `draw()`, iterate through cached layouts and invoke `font.draw(cachedText, ...)` or use pre-allocated buffers with zero heap allocation.

*Alternatives considered:*
- *Direct string generation in `draw()`*: Current implementation; causes high memory churn.
- *Removing text completely*: Simplifies draw loop, but loses immediate visibility of battery metrics requested by users.

### 2. Pre-layout Empty Network Message

Hoist the localized string `Core.bundle.get("team-resources.no-power", "No power network")` and pre-compute its `GlyphLayout` once (or on locale change / resize) rather than querying `Core.bundle` and calling `layout.setText()` every frame when `graphs.isEmpty()`.

### 3. Indexed Loops and Interval Gating in `TeamResourceState`

- Replace `for (Item item : Vars.content.items())` and `for (UnitType unit : Vars.content.units())` with standard indexed `for (int i = 0; i < items.size; i++)`.
- Move the item discovery scan and unit count change detection inside the existing 30-tick timer (`timer.get(0, 30f)`) rather than running full content scans 60 times a second.

*Alternatives considered:*
- *Event-driven item scan*: Requires subscribing to multiple world/building events which can be brittle or missed across game modes. Interval polling at 2Hz provides identical user-facing responsiveness with negligible overhead.

### 4. Tooltip Live Updating Throttling

In `createItemCard` and `SplitBar` tooltips, replace per-frame `.update()` string formatting with change-detected updates: only format strings when the tracked value (item amount, rate, power) actually changes, or bind to `state.tickSignal` instead of per-frame actor `act()` ticks.

### 5. Automated Verification via `ThreadMXBean`

Extend `RenderLoopAllocationTest` or add a dedicated `TeamResourceAllocationTest` using `ThreadMXBean.getThreadAllocatedBytes`. The test will simulate 60 frames of steady-state `SplitBar.draw()` and `TeamResourceHudView` updates and assert `0L` heap bytes allocated.

## Risks / Trade-offs

- **[Risk] Displayed power values might lag by up to 0.5s** → Mitigation: 0.5s (30 ticks) matches the existing rate update interval and human reading speed; compact numbers (`k`/`M`) do not change meaningfully faster than 2Hz anyway.
- **[Risk] Multiple power networks appearing or disconnecting** → Mitigation: Clear and rebuild cached layouts whenever `teamGraphs` changes or when graph counts differ.
