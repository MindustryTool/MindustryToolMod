## ADDED Requirements

### Requirement: Global and Screen Army Selection
The system SHALL provide buttons to select all friendly units across the map or visible on the screen, and automatically activate RTS Command Mode.

#### Scenario: Select all units across the map
- **WHEN** the user clicks "Select All (Map)"
- **THEN** all living units belonging to the player's team (excluding the player's controlled unit) are added to `input.selectedUnits`
- **AND** `input.commandMode` is set to true
- **AND** the camera does not move

#### Scenario: Select all units within the screen
- **WHEN** the user clicks "Select Screen"
- **THEN** all living units belonging to the player's team located within the current camera viewport bounds are added to `input.selectedUnits`
- **AND** `input.commandMode` is set to true

#### Scenario: Deselect all units
- **WHEN** the user clicks "Deselect All"
- **THEN** `input.selectedUnits` is cleared

### Requirement: Army Control Groups
The system SHALL allow players to create, save, recall, and delete named unit control groups.

#### Scenario: Create a control group
- **WHEN** the user clicks "Add Group" and selects a source ("Select Map", "Select Screen", or "Current Selection")
- **THEN** the candidate units are captured into a new control group
- **AND** the group persists its unit IDs

#### Scenario: Recall a control group
- **WHEN** the user clicks on a control group
- **THEN** all living units matching the group's stored IDs are placed into `input.selectedUnits`
- **AND** any dead or missing units are pruned from the group
- **AND** `input.commandMode` is set to true
- **AND** the camera does not move

#### Scenario: Delete a control group
- **WHEN** the user clicks the delete button on a control group
- **THEN** the group is removed from the active control groups list

### Requirement: Unit Type Deselection via Chips
The system SHALL display interactive chips representing each unit type in the active selection, and exclude an entire unit type when its chip is tapped.

#### Scenario: View active selection summary
- **WHEN** units are currently selected in `input.selectedUnits`
- **THEN** the UI displays each selected unit type's icon alongside its count in the Active Selection Bar

#### Scenario: Deselect specific unit type
- **WHEN** the user taps a unit type chip (e.g. Mono) in the Active Selection Bar
- **THEN** all units of that type are removed from `input.selectedUnits`
- **AND** the remaining unit types remain selected in `input.selectedUnits`

### Requirement: UI Display Mode and Persistence
The system SHALL provide options for displaying Army Control as a QuickAccess popup or persistent HUD, and keep the menu open during unit micro until explicitly dismissed.

#### Scenario: Popup remains open on action
- **WHEN** the user clicks "Select Map", "Select Screen", recalls a group, or taps a unit chip to exclude
- **THEN** the popup remains open so the user can continue managing army selections and issuing orders
- **AND** the popup only closes when the user clicks outside or clicks the close button

#### Scenario: Configurable display mode
- **WHEN** the user changes the display mode setting to HUD in feature settings
- **THEN** the Army Control bar is rendered as a persistent draggable HUD instead of a popup
