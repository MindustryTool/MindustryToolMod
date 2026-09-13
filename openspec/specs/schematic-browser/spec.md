# schematic-browser Specification

## Purpose
TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.
## Requirements
### Requirement: Browse Online Schematics
The system SHALL provide a SchematicBrowserFeature and SchematicBrowserDialog with a solid black background displaying verified schematics queried from MindustryTool.searchSchematics(). The dialog SHALL render schematic cards as physical tiles with subtle borders, rounded corners, inner padding, and WebStyles action buttons, omitting vanilla Mindustry close buttons in favor of the aligned footer close action.

#### Scenario: Display schematic cards as physical tiles
- **WHEN** schematic browser dialog is opened
- **THEN** it SHALL render against an opaque black background with a grid of schematic cards having distinct rounded borders, inner padding, and WebStyles action buttons

#### Scenario: Card click in active game match
- **WHEN** user clicks on a schematic card while in an active match and rules allow schematics
- **THEN** the system SHALL download the schematic and immediately attach it to the player's placement cursor

#### Scenario: Card click in main menu
- **WHEN** user clicks on a schematic card from the main menu
- **THEN** the system SHALL open SchematicDetailDialog for the clicked schematic

### Requirement: Copy and Save Schematics
The system SHALL allow players to copy schematic Base64 strings to the clipboard and save schematics directly into Mindustry's local library.

#### Scenario: Copy schematic to clipboard
- **WHEN** user clicks the Copy action button on a schematic card or detail dialog
- **THEN** the system SHALL download schematic data, encode it as a Base64 string, copy it to the clipboard, and show a success notification

#### Scenario: Save schematic to local library
- **WHEN** user clicks the Save action button on a schematic card or detail dialog
- **THEN** the system SHALL download schematic data, deserialize it as a Mindustry Schematic, add it to Vars.schematics, and show a saved notification

### Requirement: Schematic Detail Inspection
The system SHALL provide a SchematicDetailDialog showing high-resolution preview image, author, dimensions, item requirements breakdown, tags, and description.

#### Scenario: View schematic requirements
- **WHEN** schematic detail dialog is presented
- **THEN** it SHALL display required construction items with item icons and amounts

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the preview image SHALL render above the scrollable details in a single-column stack
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the preview image and details SHALL render side-by-side in a two-column row

### Requirement: Mindustry UI and Keybind Integration
The system SHALL inject a Browse button into Mindustry's vanilla schematics dialog (Vars.ui.schematics) and register a customizable keybinding.

#### Scenario: Schematics dialog button integration
- **WHEN** SchematicBrowserFeature is enabled
- **THEN** a Browse Online button SHALL be present in Vars.ui.schematics.buttons
- **WHEN** SchematicBrowserFeature is disabled
- **THEN** the button SHALL be removed cleanly

#### Scenario: Hotkey trigger
- **WHEN** user presses the configured schematic browser keybind and no text field is focused
- **THEN** the schematic browser dialog SHALL be displayed

