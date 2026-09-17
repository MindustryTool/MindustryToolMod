# emoji-dialog Specification

## Purpose
TBD - created by archiving change add-emoji-dialog. Update Purpose after archive.
## Requirements
### Requirement: Emoji glyph source from Iconc reflection

The system SHALL build the emoji list by reflecting over `mindustry.gen.Iconc` declared fields, keeping fields whose value is a `Character`, pairing each with its field name, and caching the result once for reuse.

#### Scenario: Dialog opens with full glyph list

- **WHEN** the user opens the Emoji dialog
- **THEN** the system shows one entry per `Iconc` character field, each entry carrying its field name and glyph char.

#### Scenario: Reflection tolerates unexpected fields

- **WHEN** an `Iconc` field cannot be read or is not a character
- **THEN** the system skips that field and still shows all readable glyph entries.

### Requirement: Search filter by field name

The system SHALL filter emoji entries by case-insensitive substring match on the field name as the user types, showing all entries when the query is blank.

#### Scenario: Typing narrows results

- **WHEN** the user types text into the search field
- **THEN** only entries whose field name contains the query (case-insensitive) are shown.

#### Scenario: Clearing restores full list

- **WHEN** the user clears the search field
- **THEN** all emoji entries are shown again.

#### Scenario: No matches show empty state

- **WHEN** no field name matches the query
- **THEN** the system shows a localized empty-result message instead of chips.

### Requirement: Flowing glyph grid display

The system SHALL display filtered entries in a scrollable flowing layout that wraps to new lines, with each entry as a pressable chip showing the glyph and its field name.

#### Scenario: Entries flow and wrap

- **WHEN** filtered entries exceed one row of width
- **THEN** chips continue on the next line inside a vertically scrollable area.

#### Scenario: Chip shows glyph and name

- **WHEN** an emoji entry is rendered
- **THEN** the chip displays the glyph character alongside its `Iconc` field name.

### Requirement: Click to copy with feedback

The system SHALL copy the glyph character to the clipboard when the user clicks anywhere on its chip and show a localized confirmation toast.

#### Scenario: Clicking glyph copies

- **WHEN** the user clicks the glyph part of a chip
- **THEN** the glyph character is copied to the clipboard and a confirmation toast is shown.

#### Scenario: Clicking name copies

- **WHEN** the user clicks the field-name part of a chip
- **THEN** the glyph character is copied to the clipboard and a confirmation toast is shown.

### Requirement: Emoji dialog chrome and localization

The system SHALL present the browser as a Solim dialog with centered width-constrained content and fully localized strings for the title, search placeholder, empty state, copied toast, and feature name, description, and help.

#### Scenario: Dialog opens localized

- **WHEN** the user opens the Emoji dialog
- **THEN** the title, search placeholder, and content are shown using the active locale bundle strings.

#### Scenario: Feature entry is localized

- **WHEN** the user views the Emoji feature card or help
- **THEN** the feature name, description, and help text come from the locale bundle.

