## Context

`ProgressDisplayFeature` is registered in `Main.java` but is an inert placeholder guarded by `development(true)`. The legacy implementation in `old/mindustrytool/features/display/progress/` used global mutable statics (`ProgressConfig`), raw `Core.settings` reads on every draw call, and an imperative `BaseDialog` settings UI. The mod now requires:

- Feature-oriented architecture matching `HealthBarFeature` and `BridgeVisualizerFeature`
- Reactive config via `ConfigGroup` / `ConfigValue<T>`
- Declarative Solim settings UI
- Zero-allocation draw path at 60 FPS

The draw loop runs every frame via `Events.run(Trigger.draw, ...)`. Per-building block-enabled checks must be O(1) with no boxing, hashing, or settings reads.

## Goals / Non-Goals

**Goals:**
- Functional progress countdown text overlay for unit-production and crafter buildings
- Per-block opt-out via `BitSet` indexed by `block.id`
- Per-block settings persisted to `Core.settings`; loaded into the `BitSet` at init and on settings change
- Reactive sliders for zoom threshold, opacity, scale
- Grouped Solim settings view (Unit Factories / Reconstructors / Unit Assemblers / Crafters) with `wrap()` for the large crafter section
- `enabledByDefault(true)`, `development` flag removed

**Non-Goals:**
- No visual fill bar — text countdown only
- No support for buildings outside the four supported categories
- No per-building instance state (all state derives from block type)

## Decisions

### D1 — BitSet for per-block enabled lookup

**Decision:** Maintain a `BitSet blockEnabled` sized to `Vars.content.blocks().size` at init time, indexed by `block.id`.

**Rationale:** A `HashMap<Block, Boolean>` would box every lookup and involve hashing. `Core.settings.getBool(...)` inside the draw loop reads from a `Settings` map on every invocation. `BitSet.get(id)` is a single array bounds-check + bit mask — zero allocations, branch-predictor friendly.

**Alternatives considered:**
- `boolean[]` — equivalent performance, slightly more memory; `BitSet` is idiomatic Java and self-documenting.
- `Core.settings.getBool` per frame — unacceptable; reads a HashMap and may flush to disk.

**BitSet lifecycle:**
1. Constructed in the feature constructor after content is loaded (post-`ClientLoadEvent` via `onEnable()` or lazy on first `draw()`).
2. Reconstructed via `rebuildBitSet()` whenever a block toggle changes in the settings view.
3. `rebuildBitSet()` iterates all blocks of the four supported types, reads `Core.settings.getBool(blockSettingKey(block), true)`, and sets the corresponding bit.

### D2 — Per-block config key pattern

**Decision:** `mindustrytool.features.progress-display.block.<block.name>`

**Rationale:** Consistent with the `ConfigGroup` namespace (`mindustrytool.features.<id>`). `block.name` is Mindustry's stable internal name (e.g., `silicon-smelter`), not the localized display name.

### D3 — Remaining time computation per building type

| Type | Progress field | Total time |
|---|---|---|
| `UnitFactory.UnitFactoryBuild` | `b.progress` | `block.plans.get(b.currentPlan).time` |
| `Reconstructor.ReconstructorBuild` | `b.progress` | `block.constructTime` |
| `UnitAssembler.UnitAssemblerBuild` | `b.progress` | `b.plan().time` |
| `GenericCrafter.GenericCrafterBuild` | `b.progress` | `block.craftTime` |

Remaining seconds = `(totalTime - progress) / 60f / b.timeScale()`.
Skip drawing if `totalTime <= 0`, `fraction < 0.01f`, or plan is invalid.

### D4 — Frame-scratch cache for scalar configs

**Decision:** Mirror `HealthBarFeature`'s pattern — peek scalar config values once at the top of `draw()` into `float frameZoom`, `float frameOpacity`, `float frameScale`. The per-building loop reads only these primitives and the `BitSet`.

**Rationale:** Avoids repeated `Signal` peek overhead across potentially hundreds of buildings per frame.

### D5 — Settings view block list populated at build time

**Decision:** The settings view iterates `Vars.content.blocks()` inside `build()`, filtering by instanceof the four block supertypes. Blocks are stable after content load; no dynamic refresh needed.

**Rationale:** Dialogs are opened during gameplay, after `ClientLoadEvent`. Content is fully loaded at that point.

## Risks / Trade-offs

- **BitSet stale after settings change** → `rebuildBitSet()` is called immediately whenever a block checkbox is toggled in the settings view. Risk is low.
- **Block content not loaded at feature construction** → `BitSet` is built lazily on first `draw()` call (or on `onEnable()`). Guard with a null/size check.
- **GenericCrafter subclass coverage** → The predicate uses `instanceof GenericCrafter.GenericCrafterBuild`. Any crafter that does not extend this (e.g. custom mod blocks) will be silently skipped. Acceptable for vanilla content coverage.
- **Remaining time display inaccuracy** → `timeScale()` is sampled once when drawing. For buildings with fluctuating time scale, the countdown may drift slightly. Acceptable visual approximation.
