# feature-keybinds Specification

## Purpose

Multi-keybind registration and centralized dispatch system for mod features with action callbacks, enabled-state guards, and vanilla Controls menu integration. Created by archiving change feature-keybind-system.
## Requirements

**Source: feature-keybind-system**

### Requirement: Multi-Keybind Registration
The system SHALL allow features to register one or more keybinds, each associated with a unique name, an Arc `KeyBind` instance, a `Runnable` action callback, and an execution precondition.

#### Scenario: Register toggle keybind
- **WHEN** a feature calls `bindToggle` with a keybind name and default keycode
- **THEN** the system SHALL register the keybind and assign an action that toggles the feature's enabled state with `requireEnabled` set to false

#### Scenario: Register multiple action keybinds on a single feature
- **WHEN** a feature (such as Time Control) registers multiple distinct keybinds with different action callbacks
- **THEN** the system SHALL maintain all registered keybinds independently for that feature

### Requirement: Centralized Keybind Dispatch with Focus Safety
The system SHALL centrally evaluate all registered feature keybinds during the game update cycle and SHALL suppress keybind execution whenever any text field or keyboard input field holds focus in the active scene.

#### Scenario: Trigger keybind when no field is focused
- **WHEN** a player presses and releases a configured keybind and `Core.scene.hasField()` is false
- **THEN** the system SHALL execute the associated action on the application main thread

#### Scenario: Suppress keybind when text field holds keyboard focus
- **WHEN** a player presses a configured keybind while typing in a chat box, search input, or dialog text field (`Core.scene.hasField()` is true)
- **THEN** the system SHALL NOT execute the keybind action

### Requirement: Action-Dependent Precondition Enforcement
The system SHALL check the `requireEnabled` precondition of a `FeatureKeybind` against the current state of its parent feature before triggering its action.

#### Scenario: Toggle keybind triggers while feature is disabled
- **WHEN** a player triggers a keybind configured with `requireEnabled = false` while the feature is disabled
- **THEN** the system SHALL execute the action and enable the feature

#### Scenario: Operational keybind suppressed while feature is disabled
- **WHEN** a player triggers a keybind configured with `requireEnabled = true` while the feature is disabled
- **THEN** the system SHALL NOT execute the action

### Requirement: Vanilla Controls Menu Integration
The system SHALL register all feature keybinds under the `MindustryTool` category with translatable bundle keys for display in Mindustry's standard "Rebind Keys" dialog.

#### Scenario: Display keybind in Mindustry controls menu
- **WHEN** a player opens Mindustry's vanilla Keybinds dialog (Settings -> Controls -> Rebind Keys)
- **THEN** all feature keybinds SHALL appear grouped under the "MindustryTool" section with localized names

#### Scenario: Rebind takes effect immediately
- **WHEN** a player rebinds a key in the vanilla dialog and closes the dialog
- **THEN** the feature SHALL respond to the newly assigned key without requiring a restart
