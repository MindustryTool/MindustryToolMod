# army-control-group Specification

## Purpose

Fast RTS army control for large unit armies, especially on mobile/touchscreen where precision box-selecting is cumbersome. Provides one-click map/screen selection, named unit control groups with recall, and quick unit-type deselection via interactive chips, integrated with vanilla RTS Command Mode without moving the camera. Created by archiving change army-control-group.

## Requirements

**Source: army-control-group**

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
The system SHALL allow players to create, save, recall, and delete unit control groups via a compact icon-density presentation without group names.

#### Scenario: Create a control group
- **WHEN** the user activates the add-group (`+`) icon in the toolbar
- **THEN** the candidate units (current selection, else screen-visible units, else map units) are captured into a new control group
- **AND** the group persists its unit IDs

#### Scenario: Recall a control group
- **WHEN** the user clicks on a control group recall button showing unit icons plus total living count
- **THEN** all living units matching the group's stored IDs are placed into `input.selectedUnits`
- **AND** any dead or missing units are pruned from the group
- **AND** `input.commandMode` is set to true
- **AND** the camera does not move

#### Scenario: Delete a control group
- **WHEN** the user clicks the delete button on a control group
- **THEN** the group is removed from the active control groups list

#### Scenario: Empty groups collapse
- **WHEN** no control groups are saved
- **THEN** the groups section renders nothing (no section label and no `No groups saved` placeholder)

### Requirement: Unit Type Deselection via Chips
The system SHALL display compact icon-plus-count chips representing each unit type in the active selection, and exclude an entire unit type when its chip is tapped.

#### Scenario: View active selection summary
- **WHEN** units are currently selected in `input.selectedUnits`
- **THEN** the UI displays one chip per selected unit type showing its unit icon alongside its count only (no full type name in the chip)
- **AND** the full unit type name is available via the chip tooltip together with the deselection hint

#### Scenario: Deselect specific unit type
- **WHEN** the user taps a unit type chip (e.g. the Mono icon-plus-count chip) in the Active Selection Bar
- **THEN** all units of that type are removed from `input.selectedUnits`
- **AND** the remaining unit types remain selected in `input.selectedUnits`

#### Scenario: Empty selection collapses
- **WHEN** no units are currently selected
- **THEN** the Active Selection section renders nothing (no section label and no `No units selected` placeholder)

### Requirement: UI Display Mode and Persistence
The system SHALL provide Army Control as a compact QuickAccess popup or persistent HUD sharing one icon-toolbar layout with no title or section labels, and keep the menu open during unit micro until explicitly dismissed.

#### Scenario: Compact toolbar actions
- **WHEN** the Army Control UI is shown in either popup or HUD mode
- **THEN** a single toolbar row provides icon-only actions for select-map, select-screen, deselect-all, add-group, and close, each with a tooltip
- **AND** no `Army Control` title header is shown

#### Scenario: Popup remains open on action
- **WHEN** the user activates select-map, select-screen, recalls a group, or taps a unit chip to exclude
- **THEN** the popup remains open so the user can continue managing army selections and issuing orders
- **AND** the popup only closes when the user activates the toolbar close icon or clicks outside

#### Scenario: Configurable display mode
- **WHEN** the user changes the display mode setting to HUD in feature settings
- **THEN** the same compact Army Control content is rendered as a persistent draggable HUD instead of a popup
