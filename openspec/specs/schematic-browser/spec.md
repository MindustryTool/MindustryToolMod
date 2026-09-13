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

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card. The schematic title SHALL be rendered inside a translucent dark overlay along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts (like/comment buttons open the detail dialog since likes have no direct API):
1. Like button with like count (`Icon.upOpenSmall` + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview image occupies the upper hero area with title overlaid on bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

### Requirement: Full-Screen Schematic Detail Dialog
`SchematicDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the preview image SHALL occupy 55% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the preview image SHALL occupy 45% of the screen height with details scrolling underneath.

#### Scenario: Maximized viewing area in landscape
- **WHEN** a schematic detail dialog is opened in landscape mode
- **THEN** the preview image expands to fill 55% of the dialog width and full dialog height

### Requirement: Zero Layout Shift Image Loading
Schematic preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Image loads without reflow
- **WHEN** a schematic thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion

