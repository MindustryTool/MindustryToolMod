## MODIFIED Requirements

### Requirement: Unified Multi-Grid Power Bar
The system SHALL consolidate power network metrics into a single unified bar representing stored battery energy relative to total capacity. If multiple distinct power networks exist, the bar SHALL partition into contiguous sub-bars proportioned by network capacity. Segment text labels and font layouts SHALL be cached and updated on the power refresh cycle, allocating zero heap bytes during steady-state per-frame `draw()` execution.

#### Scenario: Multi-network partitioning
- **WHEN** a team has two separate power grids (Grid A with 80k capacity, Grid B with 20k capacity)
- **THEN** the bar displays two segments proportioned 80% and 20% separated by a subtle divider

#### Scenario: Power segment labeling
- **WHEN** a power segment width is sufficient to accommodate text
- **THEN** the segment renders `<Stored> ± <Net Balance>` (e.g. `15k +2.4k` or `20k -500`)

#### Scenario: Adaptive label suppression on narrow segments
- **WHEN** a power segment's pixel width is narrower than its text label plus padding
- **THEN** the text label is omitted for that segment while the fill color remains visible

#### Scenario: Discharge color feedback
- **WHEN** a power network's net balance is negative (discharging stored batteries)
- **THEN** the fill color for that network's sub-bar displays red (`Pal.remove`) instead of the standard power bar color

#### Scenario: Zero-allocation per-frame draw
- **WHEN** `SplitBar.draw()` executes during steady-state gameplay
- **THEN** it renders using cached layouts and strings without allocating heap memory

## ADDED Requirements

### Requirement: Zero-Allocation State Updates
The system SHALL scan core items and unit counts using iterator-free indexed loops throttled to a periodic interval, guaranteeing that steady-state update execution does not allocate heap memory or iterators.

#### Scenario: Throttled iterator-free update
- **WHEN** `TeamResourceState.update()` executes during gameplay
- **THEN** item inventory and unit counts are evaluated using indexed loops on a periodic timer without allocating `SeqIterator` instances on the heap
