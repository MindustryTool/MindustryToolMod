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
The system SHALL provide a SchematicDetailDialog showing high-resolution preview image, author, dimensions, item requirements breakdown, tags, and description. All detail sections SHALL use `WebStyles.previewCard()` containers with consistent `gap(unit(1))` spacing. The preview image SHALL use viewport-relative sizing via `dvw()`/`dvh()` with values large enough for comfortable inspection (50% of screen height in portrait, 60% of dialog width in landscape). Action buttons SHALL use `WebStyles.cardActionText()` styling matching SchematicCard's stat buttons.

#### Scenario: View schematic requirements
- **WHEN** schematic detail dialog is presented
- **THEN** it SHALL display required construction items with item icons and amounts inside a `WebStyles.previewCard()` container with `gap(unit(1))` spacing

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the preview image SHALL render above the scrollable details in a single-column stack with height of 50% screen height via `dvh(50f)`
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the preview image and details SHALL render side-by-side in a two-column row with the image occupying 60% of dialog width via `dvw(60f)`

#### Scenario: Consistent section styling
- **WHEN** any detail section is rendered (author, stats, tags, requirements, description)
- **THEN** it SHALL be wrapped in a `WebStyles.previewCard()` styled container with `gap(unit(1))` internal spacing

#### Scenario: Action buttons match card style
- **WHEN** the Copy and Save action buttons are rendered in the detail dialog
- **THEN** they SHALL use `WebStyles.cardActionText()` styling with `height(unit(9))` and `growX()`, matching SchematicCard's stat button appearance

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
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card inside a preview card container whose height dynamically matches its width (1:1 square aspect ratio) via a reactive preview height signal. The schematic title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts:
1. Like button with like count (heart icon + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview container occupies the upper area with width and height equal (1:1 square ratio) and title centered along the bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

#### Scenario: Dynamic square aspect ratio adjustment
- **WHEN** the parent grid cell width changes due to window resize or column count reflow
- **THEN** the preview card container height SHALL update to match the new width

### Requirement: Full-Screen Schematic Detail Dialog
`SchematicDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the preview image SHALL occupy 60% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the preview image SHALL occupy 50% of the screen height with details scrolling underneath.

#### Scenario: Maximized viewing area in landscape
- **WHEN** a schematic detail dialog is opened in landscape mode
- **THEN** the preview image expands to fill 60% of the dialog width and full dialog height

#### Scenario: Large preview in portrait
- **WHEN** a schematic detail dialog is opened in portrait mode
- **THEN** the preview image occupies 50% of the screen height via `dvh(50f)`

### Requirement: Zero Layout Shift Image Loading
Schematic preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Image loads without reflow
- **WHEN** a schematic thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion

