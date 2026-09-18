## MODIFIED Requirements

### Requirement: Reactive Browser State Management
The system SHALL maintain a reactive BrowserState<T> holding search query, selected tags, sort option, current page index, total items, dynamic page size, loading flag, and error message.

#### Scenario: Update search query triggers reload
- **WHEN** user modifies the search query text field
- **THEN** the state SHALL debounce the change and fetch the first page from the API

#### Scenario: Select or unselect tag
- **WHEN** user toggles a tag category filter
- **THEN** the state SHALL reset to page 1 and execute a search query including the updated tag list

#### Scenario: Dynamic page size update
- **WHEN** the browser viewport capacity is determined or changes
- **THEN** the state SHALL update its pageSize signal clamped between 20 and 100 and reset the page to 0, triggering a reload if active

#### Scenario: Handle API failure
- **WHEN** network request fails or returns non-200
- **THEN** the state SHALL set error message and clear loading state, prompting user with retry option

### Requirement: Responsive Card Grid Layout
The system SHALL dynamically compute column count, row count, card sizing, and viewport capacity based on viewport dimensions (`dvw`, `dvh`).

#### Scenario: Screen orientation change on mobile
- **WHEN** mobile screen orientation changes from portrait to landscape
- **THEN** the grid SHALL recalculate column count from 1-2 columns to 2-3 columns without rebuilding unaffected card elements

#### Scenario: Viewport capacity determines query page size
- **WHEN** the browser dialog is displayed on screen
- **THEN** the system SHALL compute total card capacity from columns multiplied by rows and set query page size clamped between 20 and 100

#### Scenario: Touch-friendly targets on mobile
- **WHEN** rendered on mobile devices
- **THEN** all clickable buttons and card action triggers SHALL have a minimum touch target size of 40 units
