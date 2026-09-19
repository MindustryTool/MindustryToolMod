# quick-schematic-grid Specification

## Purpose
Quick access palette for player schematics via a floating HUD or QuickAccess Popup with multi-page navigation, 2D grid matrix, and slot customization.
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
The system SHALL allow configuring global grid rows, grid columns, button size, and gap spacing between buttons.

#### Scenario: Row count adjustment
- **WHEN** user adjusts the rows configuration between 1 and 7
- **THEN** the grid layout immediately updates to render the configured number of rows

#### Scenario: Column count adjustment
- **WHEN** user adjusts the columns configuration between 1 and 7
- **THEN** the grid layout immediately updates to render the configured number of columns per row

#### Scenario: Button size adjustment
- **WHEN** user adjusts the button size setting
- **THEN** the width and height of each schematic button, empty slot tile, and page tab update accordingly

#### Scenario: Button gap adjustment
- **WHEN** user adjusts the button gap setting between 0px and 16px
- **THEN** the horizontal and vertical spacing between adjacent buttons and tabs updates accordingly

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
The system SHALL allow adding, editing, clearing, and re-picking schematic entries per page and coordinate in Settings.

#### Scenario: Assigning a schematic to an empty slot
- **WHEN** user clicks an empty slot in the settings grid editor and selects a schematic from the picker
- **THEN** a new entry referencing that schematic is created at the active page, row, and column coordinates and saved

#### Scenario: Editing an occupied slot
- **WHEN** user clicks an occupied slot in the settings grid editor
- **THEN** the slot dialog opens allowing the player to customize the label, pick an icon, repick the schematic, or clear the slot

#### Scenario: Clearing a slot
- **WHEN** user clears an assigned slot in the slot dialog
- **THEN** the entry is removed from configuration and the slot reverts to an unassigned empty slot

### Requirement: Multi-Page Navigation and Tab Column
The system SHALL support up to 8 distinct pages of schematic slots with a dedicated vertical tab column rendered as the first column of the grid in HUD/Popup, and a responsive wrapping tab bar in Settings. The system SHALL allow assigning a custom icon to each page and display that icon on the page tab button in place of the page number.

#### Scenario: Switching pages via tab column
- **WHEN** user clicks a page tab button in HUD or Popup
- **THEN** the active page index switches immediately and the grid renders the schematics configured for that page without closing the popup

#### Scenario: Adding a page in Settings
- **WHEN** user clicks the Add Page button in Settings when current page count is less than 8
- **THEN** a new page is created, appended to the page tabs, and selected as the active page

#### Scenario: Removing a page in Settings
- **WHEN** user confirms deletion of a page in Settings when more than 1 page exists
- **THEN** the page, its contained entries, and its assigned page icon are removed, any subsequent page icons shift down to match their new index, and the active page adjusts to a valid remaining page

#### Scenario: Height adaptation when page count exceeds grid rows
- **WHEN** the number of vertical items in the tab column exceeds the number of grid rows
- **THEN** the overall layout height expands to match the tab column height so all tabs remain fully visible and uniform in size

#### Scenario: Page tab displays assigned icon
- **WHEN** a page has a custom icon (font glyph or game emoji) assigned
- **THEN** the page tab button in HUD, Popup, and Settings renders that icon instead of the numeric index, with a tooltip indicating the page number

#### Scenario: Page tab falls back to numeric index
- **WHEN** a page has no custom icon assigned (or the icon is cleared)
- **THEN** the page tab button in HUD, Popup, and Settings renders the 1-based page index (e.g. "1", "2")

#### Scenario: Page tabs in Settings use wrapping layout
- **WHEN** multiple page tabs exist in the Settings dialog
- **THEN** the tabs are rendered inside a `wrap()` container so they flow cleanly across multiple lines when dialog width is constrained

### Requirement: Sparse Slots and Frameless Display
The system SHALL render HUD and Popup displays framelessly without an outer background card or container border, displaying unassigned slots as subtle standalone dark tiles.

#### Scenario: Frameless HUD rendering
- **WHEN** the HUD is displayed on screen
- **THEN** no background panel or outer bounding border wraps the grid and only the standalone buttons and tiles are visible

#### Scenario: Empty slot interaction in HUD
- **WHEN** user clicks an unassigned slot tile in the in-game HUD
- **THEN** no placement action or dialog is triggered and the click is treated as passive

#### Scenario: Standalone drag handle button
- **WHEN** drag handle display is enabled in configuration
- **THEN** a standalone move button is positioned at the top of the page tab column

### Requirement: Visual Representation and Preview
The system SHALL display a visual thumbnail preview for each configured schematic by default and support custom overrides.

#### Scenario: Default thumbnail preview
- **WHEN** a schematic entry has no custom icon configured
- **THEN** the system renders the schematic's generated preview texture on the button

#### Scenario: Missing or deleted schematic handling
- **WHEN** a linked schematic can no longer be located in the player's local library
- **THEN** the system displays a warning placeholder icon and disables activation

### Requirement: Page Icon Configuration in Settings
The system SHALL allow players to view, pick, and clear the icon assigned to the active page in the Settings dialog.

#### Scenario: Previewing active page icon
- **WHEN** a page is selected in Settings
- **THEN** the Page Icon row displays the current icon glyph or a placeholder indicating no icon is assigned

#### Scenario: Picking a page icon
- **WHEN** user clicks the Pick Icon button in the Page Icon row
- **THEN** the icon picker dialog opens, and choosing an icon assigns it to the active page and persists the change immediately

#### Scenario: Clearing a page icon
- **WHEN** user clicks the Clear button in the Page Icon row for a page with an assigned icon
- **THEN** the icon assignment is removed from configuration and the page tab reverts to displaying its numeric index

