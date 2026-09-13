## Context

In the Mindustry Tool browser, `SchematicCard` and `MapCard` are laid out inside `ReactiveGrid`. The grid reflows dynamically between 1 and 6 columns based on the window viewport width. However, cards previously had a hardcoded height (`unit(58)` = 232px). Because the grid cell width is fluid, the card preview was only square at a single specific screen width and became stretched or squished otherwise.

## Goals / Non-Goals

**Goals:**
- Provide `GridItemContext` in `solim-core` that exposes `itemWidth()` and `columnCount()` as reactive `Readable` signals.
- Automatically track `ReactiveGrid` table width and compute usable item cell width without requiring manual viewport calculations.
- Overload `ReactiveGrid` and `solim.UI` builders to accept `BiFunction<T, GridItemContext, Component>` while remaining fully backward compatible with `Function<T, Component>`.
- Update `SchematicCard` and `MapCard` to bind their preview container height to the provided `itemWidth()` signal.

**Non-Goals:**
- Implement generic CSS-style `aspect-ratio` for arbitrary non-grid widgets in this change.
- Force all cards to be square if a static height is explicitly requested (support fallback to default height when no context signal is passed).

## Decisions

### 1. Dedicated `GridItemContext` Interface vs. Passing Raw Signal
- **Decision**: Create `public interface GridItemContext { Readable<Float> itemWidth(); Readable<Integer> columnCount(); }`.
- **Rationale**: An interface encapsulates current and future grid metrics (e.g. column count, item index) without breaking method signatures when new context properties are added.
- **Alternatives Considered**: Passing `Readable<Float>` directly into `BiFunction<T, Readable<Float>, Component>`. This is less extensible if other contextual properties are needed later.

### 2. Backing Width Detection in `ReactiveGrid`
- **Decision**: Track `table.getWidth()` by monitoring width changes in `table.update()` and `layout()`, with a change threshold of `0.5f` to prevent redundant reactive dispatches.
- **Rationale**: The grid table's width is dictated by the parent container (scroll pane / screen) and is independent of its children's height, preventing layout cycles.
- **Fallback**: Before the first layout pass (`table.getWidth() == 0`), calculate a sensible fallback from `Units.calcDvw()` so initial rendering does not collapse to zero height.

### 3. Usable Width Calculation
- **Decision**: `itemWidth = Math.max(0f, (tableWidth / cols) - gap)`.
- **Rationale**: In `ReactiveGrid`, each column cell has `pad(gap / 2f)` on both sides, totaling `gap` padding per column. Therefore, the content width available to the child component inside the cell is `(tableWidth / cols) - gap`.

## Risks / Trade-offs

- **[Risk] Initial frame zero-width**: Before the first layout pass, `table.getWidth()` returns 0.
  → **Mitigation**: Fall back to `Math.max(50f, (Units.calcDvw() - 32f) / cols - gap)` until the first layout pass updates `gridWidth`.
- **[Risk] Floating-point layout jitter**: Subpixel resizing could fire unnecessary reactive updates.
  → **Mitigation**: Guard signal dispatch with `Math.abs(w - currentWidth) > 0.5f`.
