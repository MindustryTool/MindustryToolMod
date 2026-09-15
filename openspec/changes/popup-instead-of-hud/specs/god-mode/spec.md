## ADDED Requirements

### Requirement: GodMode placeholder shell

The system SHALL register GodMode as a real (non-development) feature with QuickAccess membership, an enable signal, and a display-mode option defaulting to HUD, exposing no cheat controls and performing no game logic in any state.

#### Scenario: GodMode registers as non-development

- **WHEN** the mod starts and features register
- **THEN** `god-mode` reports `development=false`, appears in the settings grid without the In Development badge, and is toggleable like other real features

#### Scenario: Shell performs no logic

- **WHEN** GodMode is enabled in any display mode
- **THEN** no game state is modified and no overlay beyond the shell surfaces is shown

### Requirement: GodMode QuickAccess presence

The system SHALL list GodMode among QuickAccess HUD items (subject to existing visibility rules) with tap opening its popup in popup mode and toggling in HUD mode, and long-press opening its settings entry.

#### Scenario: GodMode QuickAccess button renders

- **WHEN** QuickAccess renders its item grid with GodMode visible
- **THEN** a GodMode button is present alongside other quick-access features

### Requirement: GodMode display mode and popup shell

The system SHALL persist a GodMode display-mode option (`ConfigValue<String>`, HUD/popup values, default HUD) and toggle a popup shell from its QuickAccess button (opening if closed, hiding if already showing or clicked again) under the same placement, suppression, fallback, and styling rules as the TimeControl popup, reusing the shared icon-only horizontal row layout from `GodModeHudView` (excluding drag handle, icon-only with tooltips, disabled state governed by `canEdit`) with no title header, wrapped in a container with a black background, `rounded(unit(2))`, `padding(unit(1))`, and `gap(unit(1))`.

#### Scenario: GodMode popup shell opens

- **WHEN** the player taps the GodMode QuickAccess button in popup mode with QuickAccess on while the popup is not showing
- **THEN** a popup shell anchored above or below the QuickAccess bar opens and the standalone path stays suppressed

#### Scenario: Clicking button again hides popup

- **WHEN** the player taps the GodMode QuickAccess button in popup mode with QuickAccess on while the GodMode popup is already showing
- **THEN** the popup closes and the feature enabled state is unchanged

#### Scenario: GodMode falls back silently

- **WHEN** GodMode display mode is popup and QuickAccess is off
- **THEN** the HUD-path behavior applies with no message and the setting stays at popup
