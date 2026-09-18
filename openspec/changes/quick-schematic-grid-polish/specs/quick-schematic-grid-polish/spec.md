## ADDED Requirements

### Requirement: Centered Aspect-Correct Thumbnails
The system SHALL render each schematic preview centered within its square button bounds, preserving aspect ratio with letterboxing, in the HUD grid, the popup grid, and the picker cards.

#### Scenario: Square button shows full schematic centered
- **WHEN** a grid button renders a non-square schematic
- **THEN** the whole preview is visible, centered, with empty bands only on the letterboxed sides

#### Scenario: Picker cards render previews within card bounds
- **WHEN** the schematic picker displays cards
- **THEN** each preview is contained within its card preview area rather than overflowing or anchoring top-left

### Requirement: Empty Grid Guidance
The system SHALL display a guidance hint in the HUD and popup grids when no schematic entries are configured.

#### Scenario: Empty HUD shows hint
- **WHEN** the grid has zero entries and renders as HUD
- **THEN** a hint directing the player to settings is shown instead of a bare handle

#### Scenario: Empty popup shows hint
- **WHEN** the grid has zero entries and renders as popup
- **THEN** the same guidance hint is shown inside the popup

### Requirement: Delete Confirmation
The system SHALL require confirmation before removing a configured schematic entry.

#### Scenario: Confirming delete removes entry
- **WHEN** the user confirms the delete prompt on an entry
- **THEN** the entry is removed and the remaining entries shift accordingly

#### Scenario: Cancelling delete keeps entry
- **WHEN** the user dismisses the delete prompt without confirming
- **THEN** the entry list is unchanged

### Requirement: Slot Editing
The system SHALL provide a per-entry edit dialog allowing a custom display label, replacement of the linked schematic, and a custom icon, opened via a dedicated per-row button.

#### Scenario: Opening edit from settings row
- **WHEN** the user taps the edit button on a configured entry
- **THEN** the slot edit dialog opens for that entry

#### Scenario: Setting a custom label
- **WHEN** the user saves a label in the edit dialog
- **THEN** grid buttons and the settings row display the custom label for that slot

#### Scenario: Replacing the linked schematic
- **WHEN** the user picks a different schematic from within the edit dialog
- **THEN** the slot links to the new schematic and its preview updates

### Requirement: Custom Slot Icons
The system SHALL offer a vanilla-style icon picker (font glyphs and unlocked content emojis in two sections) and display a chosen icon in place of the schematic preview on grid buttons.

#### Scenario: Picking an icon replaces preview
- **WHEN** a slot has a custom icon saved
- **THEN** HUD and popup buttons for that slot render the icon instead of the schematic preview

#### Scenario: Clearing an icon restores preview
- **WHEN** the user clears the custom icon in the edit dialog
- **THEN** the slot renders the schematic preview again

#### Scenario: Legacy icon fields ignored
- **WHEN** a stored entry contains the previous icon fields without the new icon field
- **THEN** the entry loads normally and renders the schematic preview

### Requirement: Drag-to-Reorder Entries
The system SHALL allow reordering settings entries by long-pressing a row to grab it, dragging to a new position with live preview, and dropping to persist the order.

#### Scenario: Long-press grabs a row
- **WHEN** the user holds a settings row background for the grab duration without moving
- **THEN** the row enters reorder mode and list scrolling locks

#### Scenario: Dragging previews new order
- **WHEN** the user drags a grabbed row to another position
- **THEN** the list visually reflects the new order while dragging

#### Scenario: Dropping persists order
- **WHEN** the user releases a grabbed row
- **THEN** the new order is saved and scrolling unlocks

#### Scenario: Early movement scrolls instead
- **WHEN** the finger moves before the grab duration elapses
- **THEN** the list scrolls normally and no reorder occurs

#### Scenario: Presses on row buttons stay tappable
- **WHEN** a press starts on an interactive row button
- **THEN** the button behaves normally and no reorder grab occurs
