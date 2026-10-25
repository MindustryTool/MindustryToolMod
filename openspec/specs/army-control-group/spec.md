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
- **WHEN** no control groups are saved but units are selected
- **THEN** the groups section renders nothing (no section label and no `No groups saved` placeholder)

### Requirement: Unit Type Deselection via Chips
The system SHALL display compact icon-plus-count toggle chips representing each unit type in the discovered unit pool, allow bidirectional inclusion/exclusion by tapping chips, and provide batch controls to select all, select none, or invert the selection without discarding discovered unit types.

#### Scenario: View selection matrix summary
- **WHEN** units are captured into the discovered pool via map, screen, or box-selection
- **THEN** the UI displays one toggle chip per discovered unit type showing its unit icon alongside its count
- **AND** chips representing currently selected unit types render in an active highlighted state
- **AND** chips representing excluded unit types render in a dimmed, inactive state

#### Scenario: Toggle unit type off
- **WHEN** the user taps an active unit type chip in the Active Selection Bar
- **THEN** all units of that type are removed from `input.selectedUnits`
- **AND** the remaining unit types remain in `input.selectedUnits`
- **AND** the tapped chip transitions to a dimmed inactive state without being removed from the UI

#### Scenario: Toggle unit type on
- **WHEN** the user taps a dimmed inactive unit type chip in the Active Selection Bar
- **THEN** all living units of that type from the discovered pool are added back to `input.selectedUnits`
- **AND** the tapped chip transitions to an active highlighted state

#### Scenario: Batch select none
- **WHEN** the user activates the "None" batch control
- **THEN** all units are cleared from `input.selectedUnits`
- **AND** all discovered unit type chips remain visible in the UI in a dimmed inactive state

#### Scenario: Batch select all
- **WHEN** the user activates the "All" batch control
- **THEN** all living units in the discovered pool are added to `input.selectedUnits`
- **AND** all discovered unit type chips transition to an active highlighted state

#### Scenario: Batch invert selection
- **WHEN** the user activates the "Invert" batch control
- **THEN** all currently active unit types are removed from `input.selectedUnits`
- **AND** all currently inactive unit types from the discovered pool are added to `input.selectedUnits`
- **AND** their respective chip states are inverted

#### Scenario: Pool dismissal on clear
- **WHEN** the user activates the toolbar "Deselect All" (✕) action or taps empty ground to clear selection
- **THEN** `input.selectedUnits` is cleared
- **AND** the discovered unit pool is completely cleared and all chips collapse

#### Scenario: Empty selection collapses
- **WHEN** no units are in the discovered pool and no control groups are saved
- **THEN** the Active Selection section renders nothing (no section label and no `No units selected` placeholder)

### Requirement: UI Display Mode and Persistence
The system SHALL provide Army Control as a compact QuickAccess popup or persistent HUD sharing one 4-icon toolbar layout with no title, section labels, or close button, keep the bar-facing popup edge fixed as content height changes, reconcile visible shells on every display-mode change, and keep the menu open during unit micro until explicitly dismissed.

#### Scenario: Compact toolbar actions
- **WHEN** the Army Control UI is shown in either popup or HUD mode
- **THEN** a single toolbar row provides icon-only actions for select-map, select-screen, deselect-all, and add-group, each with a tooltip
- **AND** no `Army Control` title header is shown
- **AND** no close button is shown.

#### Scenario: Popup remains open on action
- **WHEN** the user activates select-map, select-screen, recalls a group, or taps a unit chip to exclude
- **THEN** the popup remains open so the user can continue managing army selections and issuing orders
- **AND** the popup only closes when the user clicks outside or uses the system back button.

#### Scenario: Popup keeps its bar-facing edge on content change
- **WHEN** selection chips or control groups appear, collapse, or reflow while the popup is shown
- **THEN** the popup edge facing the QuickAccess bar stays fixed (top edge when hanging below the bar, bottom edge when sitting above it)
- **AND** growth never extends into the QuickAccess bar.

#### Scenario: Doubly-empty state shows a ghost hint line
- **WHEN** no units are selected and no control groups are saved
- **THEN** the popup shows a single ghost hint line instead of dead gap space.

#### Scenario: Switching to popup mode removes the HUD
- **WHEN** the user changes the display mode setting from HUD to popup
- **THEN** a mounted HUD is removed and no HUD is rebuilt until the mode changes back.

#### Scenario: Switching to HUD mode hides the popup and builds the HUD
- **WHEN** the user changes the display mode setting from popup to HUD
- **THEN** an open popup is hidden and the persistent draggable HUD is built.

#### Scenario: Popup mode without QuickAccess falls back to HUD
- **WHEN** the display mode is popup but the QuickAccess feature is disabled
- **THEN** the persistent HUD is shown instead, since the popup has no anchor.

#### Scenario: Configurable display mode
- **WHEN** the user changes the display mode setting to HUD in feature settings
- **THEN** the same compact Army Control content is rendered as a persistent draggable HUD instead of a popup

### Requirement: Army Control Overlay Scaling

The Army Control HUD and its QuickAccess popup SHALL render from a single effective scale equal to `ModSettings.effectiveScale(feature.scaleConfig.signal())`, the product of the global universal scale and the feature's local scale. Both surfaces SHALL apply that scale to every interactive control, icon, text run, and the panel width in the compact content, and SHALL keep structural spacing at fixed `unit(...)` values. No minimum clamp SHALL be applied, and text SHALL follow the raw scale.

#### Scenario: HUD content scales with the signal

- **WHEN** the effective scale changes from 1.0 to 0.5 while the HUD is shown
- **THEN** toolbar buttons, the drag handle, selection chips, group recall rows, group delete buttons, and unit icons render at half their 1× size
- **AND** chip counts, group counts, and the empty-state hint render at half their 1× font scale
- **AND** the panel width becomes `unit(65) × 0.5`

#### Scenario: Popup matches the HUD

- **WHEN** Army Control is displayed in popup mode at an effective scale of 1.5
- **THEN** the popup content uses the same scaled control and text sizes as the HUD at 1.5
- **AND** the popup width is `unit(65) × 1.5`
- **AND** the popup's own padding, gaps, corner radius, and border width remain at their fixed `unit(...)` values

#### Scenario: Universal scale multiplies the feature scale

- **WHEN** the universal scale is 0.8 and the Army Control feature scale is 1.5
- **THEN** both the HUD and the popup render at 1.2×

#### Scenario: Structural spacing stays fixed

- **WHEN** the effective scale is 0.5
- **THEN** chip padding, chip gaps, group-row gaps, panel padding, panel gap, corner radius, and border width are unchanged from 1.0
- **AND** only control dimensions, icon dimensions, panel width, and font scale follow the signal

#### Scenario: No minimum clamp

- **WHEN** the universal scale is 0.5 and the Army Control feature scale is 0.5
- **THEN** both surfaces render at 0.25× with no floor applied to buttons, icons, panel width, or text

#### Scenario: HUD re-clamps when scale changes

- **WHEN** the effective scale changes while the HUD is shown and the new width would place part of the HUD outside the screen
- **THEN** the HUD is re-clamped into the screen
- **AND** the clamped position is persisted through the bound position signals

#### Scenario: Popup keeps its horizontal position during live scale changes

- **WHEN** the effective scale changes while the popup is open
- **THEN** the popup's X position is unchanged
- **AND** its bar-facing vertical edge remains pinned as content height changes
- **AND** horizontal overflow at a screen edge is accepted until the popup is reopened

#### Scenario: 1× identity

- **WHEN** the effective scale is 1.0
- **THEN** control sizes, text sizes, panel width, and spacing match the pre-change Army Control layout

