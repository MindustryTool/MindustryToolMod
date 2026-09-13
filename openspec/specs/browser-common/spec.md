# browser-common Specification

## Purpose
TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.
## Requirements
### Requirement: Reactive Browser State Management
The system SHALL maintain a reactive BrowserState<T> holding search query, selected tags, sort option, current page index, total items, loading flag, and error message.

#### Scenario: Update search query triggers reload
- **WHEN** user modifies the search query text field
- **THEN** the state SHALL debounce the change and fetch the first page from the API

#### Scenario: Select or unselect tag
- **WHEN** user toggles a tag category filter
- **THEN** the state SHALL reset to page 1 and execute a search query including the updated tag list

#### Scenario: Handle API failure
- **WHEN** network request fails or returns non-200
- **THEN** the state SHALL set error message and clear loading state, prompting user with retry option

### Requirement: Responsive Card Grid Layout
The system SHALL dynamically compute column count and card sizing based on viewport dimensions (dvw) and screen orientation (isPortrait).

#### Scenario: Screen orientation change on mobile
- **WHEN** mobile screen orientation changes from portrait to landscape
- **THEN** the grid SHALL recalculate column count from 1-2 columns to 2-3 columns without rebuilding unaffected card elements

#### Scenario: Touch-friendly targets on mobile
- **WHEN** rendered on mobile devices
- **THEN** all clickable buttons and card action triggers SHALL have a minimum touch target size of 40 units

### Requirement: Paged Navigation Controls
The system SHALL provide balanced 3-section paged footer navigation with a custom Close/Back button on the left, Previous, Next, and Direct Page Jump controls centered, and an Upload action on the right, all styled with WebStyles.

#### Scenario: Previous page boundary
- **WHEN** user is on page 1
- **THEN** the Previous button SHALL be disabled with muted WebStyles styling

#### Scenario: Direct page jump
- **WHEN** user clicks on the page indicator
- **THEN** a numeric prompt dialog SHALL allow jumping directly to any valid page number

#### Scenario: Upload shortcut
- **WHEN** user clicks the upload button
- **THEN** the system SHALL open the external upload web portal URL in the default browser

#### Scenario: Custom close action
- **WHEN** user clicks the Close/Back button in the footer
- **THEN** the browser dialog SHALL be closed

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

### Requirement: Chat-Styled Search Header Input
The system SHALL display the search text field inside a card container with rounded corners, padding, and a dark border (`card(Styles.black5)` with `rounded(unit(5))` and `border(1.5f, Color.darkGray)`), matching the Chat input visual style while maintaining adjacent Refresh and Filter buttons styled with `WebStyles`.

#### Scenario: Render search field
- **WHEN** the browser search header is rendered
- **THEN** the search text field SHALL be enclosed inside a rounded dark container with border, and the Refresh and Filter buttons SHALL be adjacent standalone buttons styled with WebStyles

#### Scenario: Maintain focus styling without square corners
- **WHEN** user focuses the search text field
- **THEN** the field SHALL keep its chromeless per-instance `clearInput` style with no underline and no square focus ring, and the shared global textfield style SHALL remain unmodified

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

### Requirement: Chromeless Embedded Browser Fields
Textfields embedded inside custom containers SHALL use `WebStyles.clearInput()` instead of the default underline chrome: the `BrowserSearchHeader` search field, the `BrowserFilterDialog` filter field, the `FeatureSettingsView` search field, and the `ChatInputView` message field. Standalone labeled fields (API-key and test inputs) SHALL keep the default style. No code path SHALL mutate the shared global `TextFieldStyle`.

#### Scenario: Search field renders without underline
- **WHEN** `BrowserSearchHeader` is rendered in normal, focused, or disabled state
- **THEN** its textfield shows no underline chrome while other textfields in the mod still show the default underline

#### Scenario: No global style mutation exists
- **WHEN** the browser sources are inspected
- **THEN** no statement assigns into an object obtained from `getStyle()` on a textfield

