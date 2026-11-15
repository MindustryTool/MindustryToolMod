## Why

The current Team Resource overlay UI is bulky and cluttered with individual card borders, excess padding, and separated power bars that consume substantial screen real estate. Furthermore, empty item tracking suffers from edge-case removal bugs when core amounts drop to zero, and players lack visual historical trends for production flow and power grid stability. Redesigning the overlay delivers a minimalist, borderless layout with compact number formatting, persistent core tracking, unified power visualization, and interactive 60-second sparkline hover graphs.

## What Changes

- **Minimalist Flow-Wrap Resource Display**: Remove card containers, individual item borders, and heavy backgrounds. Place item icons and amounts closely together in a flexible flow-wrap layout.
- **Compact Number Formatting**: Standardize amount displays to at most 3 significant digits with unit suffixes (`999`, `1.2k`, `15k`, `150k`, `1.2M`) for uniform horizontal footprint.
- **Subscript Rate Indicators**: Position rate-of-change ($\Delta$/s) indicators at the bottom-right of resource amounts in a smaller font. Color-code indicators (green for positive, red for negative) and omit display entirely when rate is zero / stagnant.
- **Ever-Existed Item Tracking**: Track items that have ever existed in the team core during the match (`amount > 0`). Persist these items with count `0` even when exhausted, rather than hiding or removing them.
- **Unified Multi-Grid Power Bar**: Consolidate split satisfaction and stored power displays into a single unified bar representing stored battery energy, partitioned into sub-bars per power graph. Format each section as `<Stored> ± <Net Balance>` with adaptive font hiding for narrow sections and dynamic color feedback (yellow for charging, red for discharging).
- **Minimalist Unit Display**: Render active unit counts without card borders, with count labels at the bottom-right of unit icons.
- **60-Second Hover History Graphs**: Display interactive sparkline tooltips when hovering over items or the power bar, showing 60-second historical trends (sampled at 1Hz using a pre-allocated ring buffer for zero-allocation performance).

## Capabilities

### New Capabilities
- `team-resource-overlay`: Minimalist, borderless flow-wrapped HUD displaying tracked team items, unit counts, unified multi-grid power status, and zero-allocation 60-second hover history charts.

### Modified Capabilities
<!-- None -->

## Impact

- Affected files: `TeamResourceHudView.java`, `TeamResourceState.java`, `SplitBar.java`, `TeamResourceFeature.java`, `bundle.properties`.
- Performance: Ring-buffer sampling runs at 1Hz (`Interval(1, 60f)`), eliminating heap allocations during per-frame rendering (`ThreadMXBean` zero-allocation compliant).
- Runtime compatibility: Pure Java 8 compliant APIs.
