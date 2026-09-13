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
The system SHALL display an interactive Solim modal dialog showing sort options and categorized tags fetched dynamically from MindustryTool.getTags(). The dialog SHALL reactively filter visible tags when search input or planet selection changes, and wrap tag buttons into multi-column rows to prevent horizontal screen overflow.

#### Scenario: Render tag categories
- **WHEN** filter dialog is opened
- **THEN** it SHALL display categories as distinct visual groups with toggleable badge buttons wrapped into responsive multi-column rows

#### Scenario: Live search filter update
- **WHEN** user types into the filter dialog search field
- **THEN** the visible tags within each category SHALL update reactively without requiring dialog reopen or severed signal snapshots

#### Scenario: Planet selection filter update
- **WHEN** user toggles a planet filter in the map filter dialog
- **THEN** only tags matching the selected planets SHALL be visible across categories

#### Scenario: Clear active filters
- **WHEN** user clicks the remove icon on an active filter chip in the search header
- **THEN** the corresponding filter SHALL be removed and the search results re-fetched

### Requirement: Chat-Styled Search Header Input
The system SHALL display the search text field inside a card container with rounded corners, padding, and a dark border (`card(Styles.black5)` with `rounded(unit(5))` and `border(1.5f, Color.darkGray)`), matching the Chat input visual style while maintaining adjacent Refresh and Filter buttons styled with `WebStyles`.

#### Scenario: Render search field
- **WHEN** the browser search header is rendered
- **THEN** the search text field SHALL be enclosed inside a rounded dark container with border, and the Refresh and Filter buttons SHALL be adjacent standalone buttons styled with WebStyles

#### Scenario: Maintain focus styling without square corners
- **WHEN** user focuses the search text field
- **THEN** the focused background SHALL preserve rounded corner styling

