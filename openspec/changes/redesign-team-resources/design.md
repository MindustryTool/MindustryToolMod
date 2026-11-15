## Context

The current `TeamResourceHudView` constructs heavy card containers (`Card`) for each resource and unit with backgrounds, padding, borders, and margins. It also renders power satisfaction and stored power as two distinct blocks, consuming excessive screen space. In `TeamResourceState`, item tracking removes exhausted items when `hideZeroItems` is enabled, losing visibility of previously stockpiled resources. Furthermore, players cannot view production or storage trajectory over time.

This redesign introduces a compact, borderless, flow-wrapped overlay with 3-significant-digit formatting, subscript delta indicators with zero-suppression, a unified battery power bar, and interactive 60-second history sparkline tooltips.

## Goals / Non-Goals

**Goals:**
- Eliminate individual card borders, backgrounds, and margins in favor of a tightly packed flow-wrap layout.
- Format all numeric values with at most 3 significant digits + SI suffix for stable, uniform layout width.
- Render rate of change ($\Delta$/s) as a subscript at the bottom-right of amounts, colored green/red, and hidden when zero.
- Track items that have ever entered the team core during a match; retain them with `0` count if exhausted.
- Unify power indicators into a single multi-grid battery bar with `<Stored> ± <Net Balance>` labeling and auto-label suppression on narrow segments.
- Provide interactive 60-second sparkline history tooltips for resources and power networks without heap allocation during sampling.

**Non-Goals:**
- Modifying team core storage mechanics or Mindustry game rules.
- Persistent cross-match database logging or telemetry export.
- Full-screen analytics dashboards (this change focuses on the live HUD overlay and hover tooltips).

## Decisions

### 1. Bounded RingBuffer for History Sampling (Zero-Allocation)
- **Decision**: Use a fixed-size `float[60]` circular buffer per tracked resource and power network, indexed by a rotating head pointer.
- **Rationale**: Sampling occurs at 1Hz (`Interval(1, 60f)`). Updating an array in-place generates zero garbage collection pressure, strictly adhering to `AGENTS.md` zero-allocation frame loop constraints.
- **Alternative considered**: Storing timestamped `List<Sample>` objects. Rejected due to continuous per-second heap allocations and GC overhead on Android.

### 2. Flow-Wrap Layout for Items and Units
- **Decision**: Render item indicators using Solim's `wrap()` container (or row with flow wrapping) where items sit directly adjacent with minimal micro-gap (`unit(0.5f)` to `unit(1f)`).
- **Rationale**: Flexibly adapts to any HUD width and screen scale, avoiding rigid multi-column table gaps and wasted space.
- **Alternative considered**: Fixed-column grid. Rejected because varying item counts create uneven columns or jagged empty spaces.

### 3. Number Formatting: 3 Significant Digits + Suffix
- **Decision**: Implement a compact formatting helper:
  - `< 1,000`: raw integer (e.g. `0` to `999`).
  - `1,000` to `9,999`: `X.Xk` (e.g. `1.2k`, `8.0k`).
  - `10,000` to `999,999`: `XXk` / `XXXk` (e.g. `15k`, `150k`).
  - `≥ 1,000,000`: `X.XM` (e.g. `1.2M`).
- **Rationale**: Guarantees horizontal width stability across fluctuating inventory counts.
- **Alternative considered**: Full unformatted numbers. Rejected as long numbers cause jitter in layout widths.

### 4. Consolidated Unified Battery Bar (`SplitBar`)
- **Decision**: Refactor `SplitBar` to represent stored battery power as the primary fill fraction (`lastPowerStored / lastCapacity`), partitioned horizontally across all active power graphs weighted by capacity. Fill color dynamically switches to `Pal.remove` (red) if `graph.getPowerBalance() < 0`. Text displays `<Stored> ± <Net>` and is automatically suppressed via `GlyphLayout.width` checks if the segment is narrower than label width + padding.
- **Rationale**: Merges two disparate UI elements into one high-density, informative bar.
- **Alternative considered**: Retaining two separate bars with reduced height. Rejected because it still doubles vertical consumption.

### 5. Sparkline Tooltip Rendering
- **Decision**: Implement a dedicated `SparklineTooltip` component using Arc's `Tooltip(Cons<Table>)` and a custom drawing element (`Lines.line` / `Fill.quad`) to draw the min/max range and normalized history line.
- **Rationale**: Integrates natively with Mindustry/Arc scene graph hover detection (`InputListener.enter/exit`).
- **Alternative considered**: Opening a modal dialog on click. Rejected because hovering provides rapid, non-intrusive inspection during gameplay.

## Risks / Trade-offs

- **[Risk]** Memory footprint from history buffers if map contains many items/graphs.
  - **Mitigation**: Mindustry has only ~20 standard items. `float[60]` is 240 bytes per item; total memory for 30 items is < 10 KB, negligible on all devices.
- **[Risk]** Mobile touch devices cannot hover.
  - **Mitigation**: Support short tap/toggle on mobile to pin/show the inspection tooltip without interfering with world controls.
- **[Risk]** Text overflow in multi-grid power bar when power networks are fragmented into many tiny grids.
  - **Mitigation**: Existing `GlyphLayout` width checks unconditionally hide the label when the segment is narrower than the text width plus 4px padding.
