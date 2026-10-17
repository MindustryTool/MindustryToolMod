## Context

Solim exposes sizing two ways:

- **Element-kind** props (`width`, `height`, `size`) resolve to `ELEMENT` props that call `element.setWidth(...)`. `Element.setWidth` writes the raw scene-pixel field.
- **Cell-kind** props (`prefWidth`, `prefHeight`, `minWidth`, `maxWidth`, …) resolve to `CELL` props recorded in `PendingCellConfig` and applied to the Arc `Cell`, whose `width`/`height`/`minWidth`/`maxWidth` apply `Scl.scl()`.

Inside a `Table`, `Table.layout()` clamps the cell constraint and then `setBounds(...)` stamps the element, overwriting whatever `setWidth` wrote. The cell half therefore wins and the element half is at best decorative, at worst a silent no-op: `width(232f)` alone renders at the element's content preferred width, while `width(232f) + prefWidth(232f)` renders `232 * Scl.scl()`.

Mindustry wires the UI scale into `Scl`: `Scl.setProduct(uiscale / 100f)` (default `uiscale = 100` → `1.0`; the reporter runs at `70` → `0.7`). That is why `unit(58f) = 232` design units appears as `162px` on screen (`232 * 0.7 = 162.4`), a divergence already recorded by the `card-height-contract` `49 vs 70` probe. `Units.unit(x)` and `Units.screenWidth()` are design-unit helpers (`graphics.getWidth() / Scl.scl()`); nothing in the codebase applies `Scl` itself.

The `schematic-preview-bounds` capability currently *mandates* the widget-owned `getPrefWidth()` override, so any units fix must keep that shape and only correct its units.

Constraints: Java 8 runtime; no new public API for reading rendered size (documentation only); Mindustry UI scale must keep applying to the mod UI.

## Goals / Non-Goals

**Goals:**

- Make `width`/`height`/`size` drive the same fixed parent-cell constraint as `prefWidth`/`prefHeight`, so element and cell can never disagree and `width(x)` alone is sufficient.
- Keep a single, documented unit contract: design units in, `Scl.scl()` applied by Arc at the cell.
- Keep standalone (cell-less) hosts such as `Hud` roots sizing correctly.
- Fix the affected feature call sites and the preview bound to that contract.

**Non-Goals:**

- Any scale-independent/pixel unit policy (the mod keeps honoring Mindustry's UI scale).
- A new public "rendered size" or design→scene conversion API (documentation only).
- `Hud.keepInScreen()` pixel-vs-design clamping and chat drag-position units — separate follow-up.
- Changing Arc's soft `prefWidth` semantics or the behavior of the Mindustry UI scale setting.

## Decisions

### 1. Honor UI scale; length values are design units

Solim length values stay design units and Arc keeps applying `Scl.scl()` in cells. A design length `v` renders at `v * Scl.scl()` px; `element.getWidth()` is the rendered truth.

- *Alternative — scale-independent pixels* (bake `Scl` into `unit()` and make `Units.screenWidth()` return pixels): rejects Mindustry's UI scale for mod surfaces, reinterprets every layout constant, and rewrites several specs. Rejected.
- *Alternative — keep rendering, add a scaled readout signal*: adds state and public surface to reconcile a number that is already observable via `element.getWidth()`. Rejected in favor of documentation.

### 2. `width`/`height`/`size` become cell-driving synonyms of `prefWidth`/`prefHeight`

`width(v)` ≡ `prefWidth(v)`, `height(v)` ≡ `prefHeight(v)`, `size(w, h)` ≡ `prefWidth(w)` + `prefHeight(h)`, all fixed (`min = max`, Arc `Cell.width`/`Cell.height`). They become `CELL` props recorded in `PendingCellConfig`, so they are recorded immediately like `prefWidth` and carried to the cell by the attachment configurator.

- *Alternative — soft `prefWidth` (Arc `Cell.prefWidth`, grow-friendly) and fixed `width`*: introduces a real distinction, but changes the behavior of every existing `prefWidth(...)` call site. Rejected.
- *Alternative — deprecate `width`/`size` for cell hosts*: contradicts calling them synonyms and forces churn across many call sites. Rejected.

### 3. Standalone hosts fall back to an `Scl`-scaled element write

`PendingCellConfig` applies a sizing constraint to the element itself only when there is no parent cell: `element.setWidth(Scl.scl(value))` / `setHeight`, plus `invalidateHierarchy`. When a parent cell exists the write is skipped, leaving `Table.layout()` authoritative exactly as today. This can live in the existing `applySize(...)` helper behind the `parentCell(element) == null` branch.

- *Alternative — a `Prop` that targets both the config and the element*: duplicates the cell/units logic at the prop layer and bypasses the single applier path. Rejected.
- *Alternative — keep `width` as an `ELEMENT` prop that also records into the config* (like `alignProp`): workable but leaves two code paths with subtly different record timing. Rejected.

### 4. `BoundedSchematicImage` keeps its widget-owned preferred size, made `Scl`-aware

`getPrefWidth()`/`getPrefHeight()` continue to return `bound()`, but `bound()` returns `Scl.scl(rawBound)` so the scene-unit preferred size matches the `Scl`-scaled cell constraints of its card/button container. This preserves the `schematic-preview-bounds` requirement that sizing is the widget's own preferred size, not reactive parent-cell mutation.

- *Alternative — convert previews to cell-driven sizing*: would retire that requirement and rework the picker/grid call sites. Rejected as unnecessary for unit correctness.

### 5. Call-site consolidation to the cell vocabulary

In the affected feature files, redundant `width(x) + prefWidth(x)` and `size(x) + prefWidth(x) + prefHeight(x)` pairs collapse to `prefWidth`/`prefHeight` (matching the cell-driving model). Non-affected pairs (e.g. settings dialogs) are left untouched — once the props are synonyms they are harmless duplication, and keeping the change surface tight reduces regression risk.

### 6. `NetworkImage.size(...)` routes through the unified path

The `size` overloads set `constraints.prefWidth`/`prefHeight` and let the shared `applySize` fallback write the element in scene units, instead of writing the element raw. The decode/mask target size keeps using the design-unit value (it only sets mask resolution; `RoundedDrawable` scales the mask at draw time), so it is left unchanged.

### 7. Documentation, not a new API

The design-unit → scene-unit relationship (`value * Scl.scl()`, `element.getWidth()` as the rendered truth) is documented where the sizing props are declared and in `docs/`. No new public accessor is introduced.

## Risks / Trade-offs

- **Silent visual change where a caller passed different values to `width(a)` and `prefWidth(b)`** (previously element `a`, cell `b`; now cell `b` wins for both) → audit the affected call sites and the wider grep for mismatched pairs before landing; consolidate to a single value.
- **`width`/`size` record earlier than before** (immediately at `props(...)` rather than during the ownership window) because they are now `CELL` props → matches `prefWidth` timing, but verify ordering-sensitive tests that mix `width` with `align`/`growX`.
- **Existing `solim-core` tests assert raw element width after `width(x)`** → update them to the fixed-cell contract (`width(x)` == `prefWidth(x)`; standalone fallback is `Scl.scl(x)`).
- **`cell-height-contract` probe assertions become real assertions** → they must pin `Scl.scl(v)`; run under a fixed test `Scl` so the assertion is deterministic.
- **`Hud` roots with no cell** now depend on the fallback branch → covered by a dedicated `Hud`/standalone sizing test.
- **Scope creep into the deferred unit-mixing bugs** (`keepInScreen`, chat drag) → explicitly out of scope; tracked as a follow-up change.

## Migration Plan

1. Change `Values.width`/`height`/`size` to `CELL` props and add the standalone fallback to `PendingCellConfig.applySize`; run the `solim-core` suite and fix the sizing/prop/disposal tests.
2. Route `NetworkImage.size(...)` through the unified path; keep the design-unit decode target.
3. Consolidate the affected feature call sites (`SchematicCard`, `MapCard`, `ChatOverlayHudView`).
4. Make `BoundedSchematicImage.bound()` `Scl`-aware.
5. Update `docs/` and the sizing-prop jsdoc; finalize the `card-height-contract` probe assertions.

Rollback: revert the change directory and its code diff; the design-unit contract was already the de facto behavior, so no data migration is involved.

## Open Questions

None.
