# tactical-keybinds Specification

## Purpose
TBD - created by archiving change tactical-and-hud-keybinds. Update Purpose after archive.
## Requirements
### Requirement: Army Control RTS Control Groups
The Army Control feature SHALL provide support for control groups 1 through 9 on desktop.
Pressing `Ctrl + [1..9]` SHALL save currently selected commandable units to the respective group slot index.
Pressing `[1..9]` without Ctrl SHALL recall the saved units in the corresponding group slot and activate RTS command mode.

#### Scenario: Save and recall group on desktop
- **WHEN** the player has commandable units selected, holds Ctrl, and presses key `1`
- **THEN** control group 1 is created or updated with the selected unit IDs
- **WHEN** the player releases selection and presses key `1` without Ctrl
- **THEN** the units saved in group 1 are selected and command mode is activated

#### Scenario: Suppressed during typing
- **WHEN** any text field in the scene has focus (`Core.scene.hasField()`)
- **THEN** number key presses do NOT trigger group recall or save

### Requirement: Callout Tactical Menu Keybind
The Callout feature SHALL register a configurable keybind named `keybind.calloutMenu` defaulting to `KeyCode.unset`.
Activating the keybind SHALL open the tactical callout dialog.

#### Scenario: Trigger callout menu via keybind
- **WHEN** the user presses the key bound to `keybind.calloutMenu` while Callout feature is enabled
- **THEN** the callout tactical dialog is displayed

### Requirement: Grief Alert Jump Keybind
The Grief Alert feature SHALL register a configurable keybind named `keybind.griefAlertJump` defaulting to `KeyCode.unset`.
Activating the keybind while an active alert exists SHALL jump the camera to the alerted coordinates and trigger visual pulse highlight.

#### Scenario: Trigger grief alert jump via keybind
- **WHEN** an alert is active and the user presses the key bound to `keybind.griefAlertJump`
- **THEN** the camera centers on the alert tile, camera detaches from player, and the banner is dismissed

### Requirement: Block Stats Display Toggle Keybind
The Block Stats Display feature SHALL register a configurable toggle keybind named `keybind.blockStatsToggle` defaulting to `KeyCode.unset`.
Activating the keybind SHALL toggle the enabled state of the feature.

#### Scenario: Toggle block stats display via keybind
- **WHEN** the user presses the key bound to `keybind.blockStatsToggle`
- **THEN** the feature enabled state is inverted and overlays update accordingly

### Requirement: Settings UI Keybind Cards
The settings views for Army Control, Callout, Grief Alert, and Block Stats Display SHALL render a `SettingKeybindCard` when running on desktop.

#### Scenario: Keybind card visibility
- **WHEN** a desktop player opens the settings dialog of any of the four features
- **THEN** the Keybinds section card is displayed with the corresponding rebindable controls
- **WHEN** a mobile player opens the settings dialog
- **THEN** the Keybinds section card is hidden

