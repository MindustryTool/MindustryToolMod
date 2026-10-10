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
The system SHALL provide a detail dialog allowing the user to view full patch metadata and scroll through the raw HJSON patch text.

#### Scenario: Open patch detail
- **WHEN** user clicks on a patch card or clicks "View Code"
- **THEN** the system opens a dialog showing full patch details and a scrollable mono-font code view of the patch HJSON content

### Requirement: Copy patch to clipboard
The system SHALL allow copying the raw HJSON patch content to the system clipboard.

#### Scenario: Copy patch content
- **WHEN** user clicks the copy button for a content patch
- **THEN** the system fetches the patch data if not already cached, copies the raw text to the clipboard, and displays a success toast notification

### Requirement: Save patch to local file
The system SHALL allow saving the content patch as a `.hjson` file in the Mindustry data directory.

#### Scenario: Save patch to file
- **WHEN** user clicks the save to file button
- **THEN** the system writes the patch text to `data/patches/<sanitized-name>.hjson` and notifies the user with a confirmation toast

