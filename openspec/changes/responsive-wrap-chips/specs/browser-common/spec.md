## MODIFIED Requirements

### Requirement: Tag & Category Filtering Modal
The system SHALL display an interactive Solim modal dialog showing sort options and categorized tags fetched dynamically from MindustryTool.getTags(). The dialog SHALL reactively filter visible tags when search input or planet selection changes, and wrap tag buttons into responsive flow rows using `Wrap` with per-item width computed from `GlyphLayout` text measurement. All toggle buttons SHALL use `WebStyles.filterChipText()` styling. The "Clear all" button SHALL use `WebStyles.clearFiltersText()` styling. Tag and planet data SHALL be cached in static signals that persist across dialog open/close instances, fetching from the API only on first open.

#### Scenario: Render tag categories
- **WHEN** filter dialog is opened
- **THEN** it SHALL display categories as distinct visual groups with toggleable `WebStyles.filterChipText()` badge buttons wrapped into responsive flow rows using `Wrap`, where each chip width is `GlyphLayout.textWidth(Fonts.def, label) + 8f padding + 4f safety`

#### Scenario: Live search filter update
- **WHEN** user types into the filter dialog search field
- **THEN** the visible tags within each category SHALL update reactively without requiring dialog reopen or severed signal snapshots

#### Scenario: Planet selection filter update
- **WHEN** user toggles a planet filter in the map filter dialog
- **THEN** only tags matching the selected planets SHALL be visible across categories

#### Scenario: Clear active filters
- **WHEN** user clicks the "Clear all filters" button
- **THEN** all selected tags, sort option, planet selection, and filter text SHALL be reset

#### Scenario: Sort options use flow wrap
- **WHEN** sort options are rendered in the filter dialog
- **THEN** each sort button SHALL use `WebStyles.filterChipText()` with reactive `.checked()` binding, arranged in a `Wrap` flow layout where each chip width is computed from its label text width

#### Scenario: Persistent tag and planet cache
- **WHEN** the filter dialog is opened for the first time in a session
- **THEN** tag categories and planet data SHALL be fetched from the API and stored in static signals
- **WHEN** the filter dialog is opened on subsequent occasions
- **THEN** the cached data SHALL be read directly without additional network requests
