# content-patch-browser Specification

## Purpose
TBD - created by archiving change content-patch-browser. Update Purpose after archive.
## Requirements
### Requirement: Browse content patches
The system SHALL provide a browser dialog displaying content patches fetched from the Mindustry Tool API with pagination, search, sort options, and tag filtering.

#### Scenario: Display patch list
- **WHEN** user opens the Content Patch Browser dialog
- **THEN** the system queries the API and displays patch cards matching the current filter state in a reactive grid

#### Scenario: Search and filter patches
- **WHEN** user inputs a search query or selects a tag/sort option
- **THEN** the browser resets to page 0, re-queries the API with the new parameters, and renders the updated results

### Requirement: Content patch card display
The system SHALL display each content patch with its name, type badge (DATA or DATAPACK), description, category tags, and download/like metrics.

#### Scenario: Card rendering
- **WHEN** a patch is rendered in the browser grid
- **THEN** the card displays the patch's title, type badge, description excerpt, category chips, and stats badge

### Requirement: View patch detail and code
The system SHALL provide a detail dialog allowing the user to view full patch metadata and inspect the patch content with formatted HJSON syntax highlighting, a line number gutter, and two-dimensional scrolling.

#### Scenario: Open patch detail
- **WHEN** user clicks on a patch card or clicks "View Code"
- **THEN** the system opens a dialog showing full patch details and a scrollable mono-font code view with normalized HJSON indentation, line numbers, and syntax color highlighting

#### Scenario: 2D scroll code without line wrapping
- **WHEN** patch code lines exceed the visible width of the code section
- **THEN** the system maintains full line structure and indentation without soft-wrapping, enabling horizontal and vertical scrolling across both the line numbers and highlighted code

### Requirement: Copy patch to clipboard
The system SHALL allow copying the raw HJSON patch content to the system clipboard asynchronously without freezing the game.

#### Scenario: Copy patch content
- **WHEN** user clicks the copy button for a content patch
- **THEN** the system fetches the raw patch data via the download endpoint without blocking the main game thread, copies text to the clipboard, and displays a success toast notification

### Requirement: Save patch to local file
The system SHALL allow saving the content patch as a `.hjson` file in the Mindustry data directory using background file I/O.

#### Scenario: Save patch to file
- **WHEN** user clicks the save to file button
- **THEN** the system downloads the raw patch data and writes the file in the background without blocking the game rendering thread, then notifies the user on the main thread

