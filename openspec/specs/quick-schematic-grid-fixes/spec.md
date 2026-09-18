# quick-schematic-grid-fixes Specification

## Purpose
TBD - created by archiving change quick-schematic-grid-fixes. Update Purpose after archive.
## Requirements
### Requirement: Bounded Grid Thumbnails
The system SHALL render each schematic preview strictly inside its button bounds with aspect preserved, in the HUD grid, the popup grid, and the picker cards.

#### Scenario: Large schematic stays inside button
- **WHEN** a grid button renders a schematic whose native preview exceeds the button size
- **THEN** the whole preview is visible, centered and letterboxed, with nothing drawn outside the button

#### Scenario: Button size change re-bounds previews
- **WHEN** the user adjusts the button size slider
- **THEN** all visible previews resize to the new button bounds without overflow

#### Scenario: Picker cards contain previews
- **WHEN** the schematic picker displays cards
- **THEN** each preview stays inside its card preview area

### Requirement: Visible Reorder Controls
The system SHALL render always-visible reorder buttons on each settings entry row, with up moving the entry earlier and down moving it later.

#### Scenario: Reorder buttons are visible
- **WHEN** the settings entry list renders
- **THEN** every row shows an up control and a down control regardless of font glyph coverage

#### Scenario: Up moves entry earlier
- **WHEN** the user activates the up control on an entry that is not first
- **THEN** the entry swaps positions with the preceding item and the order is saved

#### Scenario: Down moves entry later
- **WHEN** the user activates the down control on an entry that is not last
- **THEN** the entry swaps positions with the following item and the order is saved

### Requirement: Settings Row Slot Visuals
The system SHALL display a leading slot visual on every settings entry row: the custom icon when one is set, otherwise a mini schematic preview.

#### Scenario: Customized slot shows its icon
- **WHEN** a settings row renders for an entry with a custom icon
- **THEN** the row leading visual is that icon

#### Scenario: Plain slot shows mini preview
- **WHEN** a settings row renders for an entry without a custom icon and a resolvable schematic
- **THEN** the row leading visual is a contained miniature of the schematic preview

#### Scenario: Missing schematic keeps warning
- **WHEN** a settings row renders for an entry whose schematic cannot be resolved
- **THEN** the row keeps its warning presentation

