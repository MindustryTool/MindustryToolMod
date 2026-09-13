# map-browser Specification

## Purpose
TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.
## Requirements
### Requirement: Browse Online Maps
The system SHALL provide a MapBrowserFeature and MapBrowserDialog with a solid black background displaying verified maps queried from MindustryTool.searchMaps(). The dialog SHALL render map cards as physical tiles with subtle borders, rounded corners, inner padding, and WebStyles action buttons, omitting vanilla Mindustry close buttons in favor of the aligned footer close action.

#### Scenario: Display map cards as physical tiles
- **WHEN** map browser dialog is opened
- **THEN** it SHALL render against an opaque black background with a grid of map cards having distinct rounded borders, inner padding, and WebStyles action buttons

#### Scenario: Card click opens map details
- **WHEN** user clicks on a map card
- **THEN** the system SHALL open MapDetailDialog for the clicked map

### Requirement: Download and Save Maps
The system SHALL allow players to download map .msav files directly into Mindustry's custom maps directory and import them into the active game maps registry.

#### Scenario: Direct map download
- **WHEN** user clicks the Download action button on a map card or detail dialog
- **THEN** the system SHALL download map bytes, save them into Vars.customMapDirectory, import them via Vars.maps.importMap(), and show a saved notification

### Requirement: Map Detail Inspection and Launching
The system SHALL provide a MapDetailDialog showing full map preview image, author, dimensions, description, tags, and shortcuts to save or host the map.

#### Scenario: Host or play map directly
- **WHEN** user clicks Host/Play button in the map detail dialog
- **THEN** the system SHALL ensure the map is downloaded and import-ready, opening Mindustry's host or custom game setup dialog

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the map preview image SHALL render above the scrollable details in a single-column stack
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the map preview image and details SHALL render side-by-side in a two-column row

### Requirement: Mindustry UI and Keybind Integration
The system SHALL inject a Browse button into Mindustry's maps dialog (Vars.ui.maps) and register a customizable keybinding.

#### Scenario: Maps dialog button integration
- **WHEN** MapBrowserFeature is enabled
- **THEN** a Browse Online button SHALL be present in Vars.ui.maps.buttons
- **WHEN** MapBrowserFeature is disabled
- **THEN** the button SHALL be removed cleanly

#### Scenario: Hotkey trigger
- **WHEN** user presses the configured map browser keybind and no text field is focused
- **THEN** the map browser dialog SHALL be displayed

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card. The map title SHALL be rendered inside a translucent dark overlay along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons using vanilla Mindustry icons with stat counts (`Icon.upOpenSmall` + likes opening details, `Icon.chatSmall` + comments opening details, `Icon.downloadSmall` + downloads triggering download, `Icon.play` triggering play). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview image occupies the upper hero area with title overlaid on bottom, followed by a compact action button row

### Requirement: Full-Screen Map Detail Dialog
`MapDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the map preview image SHALL occupy 55% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the map preview image SHALL occupy 45% of the screen height with details scrolling underneath.

#### Scenario: Maximized map viewing area
- **WHEN** a map detail dialog is opened in landscape mode
- **THEN** the map preview expands to fill 55% of dialog width and 100% of dialog height

### Requirement: Zero Layout Shift Map Loading
Map preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Map loads without reflow
- **WHEN** a map thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion

