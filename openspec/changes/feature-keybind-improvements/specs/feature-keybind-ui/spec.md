## ADDED Requirements

### Requirement: Feature Keybind Inline Component
The system SHALL provide a declarative Solim UI component `SettingKeybindCard` that displays a list of registered `FeatureKeybind` descriptors with their localized names, interactive keybind chips, and reset buttons.

#### Scenario: Display keybinds for a feature
- **WHEN** a feature has registered keybinds and renders `SettingKeybindCard.of(feature)`
- **THEN** a section card titled with the localized keybinds label SHALL render displaying a row for each registered keybind

#### Scenario: Hide keybind card when feature has no keybinds
- **WHEN** a feature has zero registered keybinds and calls `SettingKeybindCard.of(feature)`
- **THEN** the component SHALL render an empty element or omit the section card

### Requirement: Interactive Inline Key Rebinding
The system SHALL support inline key capture when the user clicks a keybind chip, listening for input until a valid key/mouse button is pressed or the action is canceled.

#### Scenario: Successfully rebind a key
- **WHEN** the user clicks an idle keybind chip and subsequently presses a key (e.g., `G`)
- **THEN** the chip SHALL update its label to `G`, the underlying `KeyBind` SHALL update its value and save to settings, and the new key SHALL trigger the action immediately

#### Scenario: Cancel key rebinding
- **WHEN** the key chip is in listening state and the user presses `Escape`
- **THEN** the listening state SHALL cancel and the keybind value SHALL remain unchanged

#### Scenario: Clear keybind to unset
- **WHEN** the key chip is in listening state and the user presses `Backspace`
- **THEN** the keybind value SHALL be set to `KeyCode.unset` and saved

### Requirement: Keybind Reset to Default
The system SHALL provide a reset action next to each keybind row that is active when the keybind has been modified from its default value.

#### Scenario: Reset key to default
- **WHEN** a keybind has been modified from its default keycode and the user clicks the reset button
- **THEN** the keybind value SHALL be restored to its default value, saved to settings, and the UI chip SHALL update to reflect the default key
