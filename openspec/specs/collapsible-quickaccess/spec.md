# collapsible-quickaccess Specification

## Purpose

Configurable collapsible HUD overlay capability allowing the Quick Access HUD to collapse down into a compact draggable icon badge and expand back into the full feature bar. Created by archiving change collapsible-quick-access-hud.

## Requirements

**Source: collapsible-quick-access-hud**

### Requirement: Collapsible Quick Access configuration
The system SHALL provide a `collapsible` boolean configuration in `QuickAccessFeature` defaulting to `false`, and a `collapsed` boolean configuration defaulting to `false` to store the active collapsed state.

#### Scenario: Default configuration state
- **WHEN** the mod is initialized with default settings
- **THEN** `collapsibleConfig.get()` returns false and `collapsedConfig.get()` returns false

#### Scenario: User toggles collapsible setting
- **WHEN** user enables the "Collapsible" option in Quick Access settings
- **THEN** `collapsibleConfig` updates to true and persists across restarts

#### Scenario: Collapsed state persists
- **WHEN** user collapses the HUD and reloads the map or restarts
- **THEN** `collapsedConfig` retains its true state

### Requirement: Collapsed state display and interaction
The system SHALL display the Quick Access HUD as a single compact draggable button with the Quick Access grid icon when both `collapsibleConfig` and `collapsedConfig` are true.

#### Scenario: HUD collapses to compact badge
- **WHEN** `collapsibleConfig` is true and `collapsedConfig` is set to true
- **THEN** the HUD renders only the compact draggable button with `grid-2x2.png` icon

#### Scenario: Clicking collapsed badge expands HUD
- **WHEN** user clicks the collapsed compact badge
- **THEN** `collapsedConfig` is set to false and the full HUD bar expands

#### Scenario: Dragging collapsed badge repositions HUD
- **WHEN** user drags the collapsed compact badge
- **THEN** the HUD coordinates update via `xSignal` and `ySignal` without triggering expansion

### Requirement: Expanded state collapse toggle
The system SHALL display a collapse toggle button in the expanded HUD bar when `collapsibleConfig` is true, positioned immediately following the drag handle.

#### Scenario: Collapse button visible when collapsible enabled
- **WHEN** `collapsibleConfig` is true and `collapsedConfig` is false
- **THEN** a collapse toggle button (`[ ◀ ]`) is visible next to the drag handle

#### Scenario: Collapse button hidden when collapsible disabled
- **WHEN** `collapsibleConfig` is false
- **THEN** no collapse toggle button is shown and the HUD remains fully expanded

#### Scenario: Clicking collapse button collapses HUD
- **WHEN** user clicks the collapse toggle button
- **THEN** `collapsedConfig` is set to true and the HUD transitions to the collapsed badge

### Requirement: Boundary clamping on collapse transition
The system SHALL ensure the HUD remains within screen bounds when toggling between collapsed and expanded states.

#### Scenario: Screen clamping on state change
- **WHEN** `collapsedConfig` value changes
- **THEN** `hud.keepInScreen()` is executed asynchronously to ensure the HUD element does not clip outside screen boundaries
