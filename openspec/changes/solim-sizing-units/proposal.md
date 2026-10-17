## Why

Solim's element-kind sizing props (`width`, `height`, `size`) write the element's raw dimension, which is in Arc scene/scaled units, while the cell-kind props (`prefWidth`, `prefHeight`) route through Arc `Cell.width/height`, which apply `Scl.scl()`. Inside a `Table` the cell constraint wins and `Table.layout()` stamps the element, so the two halves silently disagree: a card configured with `width(unit(58f)) = 232` renders at `232 * Scl.scl()` (162px at Mindustry's default 70% UI scale) while the bound signal still reads 232. Every caller therefore has to pair `width(x)` with `prefWidth(x)` and guess which number is authoritative, and `width(x)` alone is a silent no-op for a table-backed element in a cell.

## What Changes

- **BREAKING**: `width(float|Readable<Float>)`, `height(...)`, and `size(...)` become **cell-driving**: they record the same fixed parent-cell constraint (`prefWidth`/`prefHeight`, i.e. Arc `min = max`) that `prefWidth`/`prefHeight` already record, so element and cell can no longer disagree. `width`/`size` become synonyms of `prefWidth`/`prefHeight`.
- Standalone hosts (a `Hud` root with no parent `Table`) keep working: the cell-sizing path gains an **`Scl`-aware element fallback**, writing `element.setWidth(Scl.scl(value))` only when there is no parent cell, so a `width(x)`-only host still sizes correctly.
- Length values stay **design units** and Arc keeps applying `Scl.scl()` in cells (honor Mindustry UI scale). The design-unit contract is documented: a design length `v` renders at `v * Scl.scl()` pixels; `element.getWidth()` is the rendered truth (no new API).
- Affected feature call sites drop their redundant `width(x) + prefWidth(x)` / `size(x) + prefWidth(x) + prefHeight(x)` pairs down to the cell vocabulary (`prefWidth`/`prefHeight`).
- `BoundedSchematicImage` keeps its widget-owned `getPrefWidth()`/`getPrefHeight()` override but returns the bound in scene units (`Scl.scl(bound)`) so the preview box matches its cell-scaled container.
- `NetworkImage.size(...)` is routed through the same cell-driving, `Scl`-aware sizing path instead of writing the element raw.
- Out of scope (separate follow-up): `Hud.keepInScreen()` clamping against `Core.scene.getWidth()` in pixels while the mod passes design units, and chat drag-position units.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-layout`: `ElementConfig provides size mutations` changes — `width`/`height`/`size` SHALL drive the fixed parent cell in design units and fall back to an `Scl`-scaled element write only when no parent cell exists. The `PendingCellConfig` sizing appliers gain that `Scl`-aware standalone fallback, and the `Height vs min-max clarification probes` requirement resolves to `height()` = fixed cell height. The viewport/units requirements are clarified to state that `dvw`/`dvh`/`Units.width()`/`height()` are design units that scale by `Scl.scl()` at render.
- `schematic-preview-bounds`: the picker/slot preview bound SHALL be expressed in scene units so the thumbnail box matches its `Scl`-scaled container.
- `card-height-contract`: the probes that recorded `49 vs 70` SHALL assert the resolved fixed-height semantics instead of "no fixed assertion until semantics are decided".

## Impact

- `solim-core`: `solim.modifier.Values` (`width`/`height`/`size`), `solim.modifier.PendingCellConfig` (`applySize`/`applySizeToParentCell` fallback), `solim.display.NetworkImage` (`size` overloads).
- `mod`: call-site cleanup in `features/browser/schematic/SchematicCard`, `features/browser/map/MapCard`, `features/chat/ChatOverlayHudView`, `features/schematicgrid/BoundedSchematicImage` (and the quick-schematic-grid views that mount it).
- Docs: the design-unit/scene-unit relationship is documented (jsdoc where the props are declared, plus `docs/`).
- Tests: `solim-core` sizing/prop/disposal tests that assert raw element width or assume `width()` is element-only must be updated to the new fixed-cell contract; `card-height-contract` probe assertions are finalized.
