## ADDED Requirements

### Requirement: Standardized Fixed Header Height
The system SHALL lock the search bar card container and adjacent action buttons (`refresh`, `filter`) in `BrowserSearchHeader` to a standardized fixed height (`unit(10)` / 40px) with vertically centered icons and text field, ensuring pixel-perfect baseline alignment across the header bar.

#### Scenario: Uniform header control heights
- **WHEN** `BrowserSearchHeader` is rendered
- **THEN** both the search container and the adjacent buttons SHALL have equal height of `unit(10)`

### Requirement: Dynamic Filter Chip Bar Rendering
The system SHALL render the active filter chips row using `dynamic(...)` conditioned on active tag selection. When no tags are selected, the system SHALL emit no layout elements, preventing empty table space or residual padding.

#### Scenario: No active filters produces no empty space
- **WHEN** `state.selectedTags()` is empty
- **THEN** the filter chip bar SHALL NOT emit any table row or container element

#### Scenario: Active filters render chips
- **WHEN** tags are selected
- **THEN** the filter chip bar SHALL render chips with delete icons and a clear-all button
