## MODIFIED Requirements

### Requirement: Reactive Browser State Management
The system SHALL maintain a reactive BrowserState<T> holding search query, selected tags, sort option, current page index, total items, dynamic page size, loading flag, and error message. BrowserState SHALL internally use `Query<T>` for its fetch lifecycle — delegating loading state, error handling, thread marshaling, and stale-while-revalidate behavior to the Query primitive. BrowserState's public API (signal accessors, pagination methods) SHALL remain unchanged.

#### Scenario: Update search query triggers reload
- **WHEN** user modifies the search query text field
- **THEN** the state SHALL debounce the change and the internal Query SHALL auto-refetch due to reactive dependency tracking on the query signal

#### Scenario: Select or unselect tag
- **WHEN** user toggles a tag category filter
- **THEN** the state SHALL reset to page 0 and the internal Query SHALL auto-refetch due to reactive dependency tracking on the selectedTags signal

#### Scenario: Dynamic page size update
- **WHEN** the browser viewport capacity is determined or changes
- **THEN** the state SHALL update its pageSize signal clamped between 20 and 100 and reset the page to 0, and the internal Query SHALL auto-refetch

#### Scenario: Handle API failure
- **WHEN** network request fails or returns non-200
- **THEN** the internal Query SHALL manage error state and retry with backoff, and BrowserState's `error()` signal SHALL reflect the Query's error state

#### Scenario: BrowserState start/stop lifecycle
- **WHEN** `start()` is called on BrowserState
- **THEN** the internal Query SHALL be activated (enabled), and when `stop()` is called, the Query SHALL be deactivated

#### Scenario: Stale data during refetch
- **WHEN** a filter parameter changes while data is already loaded
- **THEN** BrowserState's `items()` SHALL retain the previous data while the internal Query refetches, and `loading()` SHALL remain false while `fetching()` is true
