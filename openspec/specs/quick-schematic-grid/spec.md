# quick-schematic-grid Specification

## Purpose
TBD - created by archiving change quick-schematic-grid. Update Purpose after archive.
## Requirements
### Requirement: Display Variants
The system SHALL support two distinct display variants for the Quick Schematic Grid: a floating persistent HUD and an on-demand Popup.

#### Scenario: Switching to HUD variant
- **WHEN** user selects HUD display mode in settings
- **THEN** the Quick Schematic Grid is rendered as a floating, draggable window on the screen

#### Scenario: Switching to Popup variant
- **WHEN** user selects Popup display mode in settings
- **THEN** the floating HUD is hidden, and the grid becomes accessible via the QuickAccess bar

#### Scenario: Opening Popup from QuickAccess
- **WHEN** user clicks the Quick Schematic Grid icon in the QuickAccess bar while in Popup mode
- **THEN** the grid popup appears anchored to the QuickAccess bar

### Requirement: Configurable Grid Geometry
The system SHALL allow configuring grid columns, button size, and gap spacing between buttons.

#### Scenario: Column count change
- **WHEN** user adjusts the columns configuration between 1 and 10
- **THEN** the grid layout immediately adjusts to display that number of columns per row

#### Scenario: Button size adjustment
- **WHEN** user adjusts the button size setting
- **THEN** the width and height of each schematic button in HUD and Popup update accordingly

#### Scenario: Button gap adjustment
- **WHEN** user adjusts the button gap setting between 0px and 16px
- **THEN** the horizontal and vertical spacing between adjacent buttons updates accordingly

### Requirement: Schematic Placement Activation
The system SHALL activate the linked schematic for in-game placement when its button is clicked.

#### Scenario: Activating a valid schematic in-game
- **WHEN** user clicks a schematic button in HUD or Popup while in an active game session
- **THEN** the system invokes schematic placement for the linked schematic

#### Scenario: Auto-dismissing Popup on activation
- **WHEN** user clicks a schematic button inside the Popup variant
- **THEN** the popup immediately closes and schematic placement is activated

#### Scenario: Schematics disabled by game rules
- **WHEN** user clicks a schematic button in a game where schematics are prohibited by rules
- **THEN** the system displays a disabled notification and does not activate placement

### Requirement: Entry Management in Settings
The system SHALL allow adding, reordering, and removing schematic entries.

#### Scenario: Adding a schematic via picker
- **WHEN** user clicks the add button in the settings dialog and selects a schematic from the picker
- **THEN** a new entry referencing that schematic is appended to the list and saved

#### Scenario: Reordering an entry earlier in the list
- **WHEN** user clicks the move left/up button on an entry that is not first
- **THEN** the entry swaps positions with the preceding item and the order is saved

#### Scenario: Reordering an entry later in the list
- **WHEN** user clicks the move right/down button on an entry that is not last
- **THEN** the entry swaps positions with the following item and the order is saved

#### Scenario: Removing an entry
- **WHEN** user clicks the remove button on an entry
- **THEN** the entry is deleted from the list and the remaining entries shift accordingly

### Requirement: Visual Representation and Preview
The system SHALL display a visual thumbnail preview for each configured schematic by default and support custom overrides.

#### Scenario: Default thumbnail preview
- **WHEN** a schematic entry has no custom icon configured
- **THEN** the system renders the schematic's generated preview texture on the button

#### Scenario: Missing or deleted schematic handling
- **WHEN** a linked schematic can no longer be located in the player's local library
- **THEN** the system displays a warning placeholder icon and disables activation

