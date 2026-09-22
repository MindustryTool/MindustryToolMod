# smart-upgrade-trigger Specification

## Purpose

Double-tap gesture trigger for the Smart Upgrade menu replacing long-press hold detection. Two taps on the same building within a configurable tap-interval open the upgrade menu stacked above vanilla selection, while the `U` hover keybind, guards, and chain-upgrade behavior remain unchanged. Created by archiving change smart-upgrade-double-tap.

## Requirements

### Requirement: Double-tap opens upgrade menu on same building
The system SHALL open the Smart Upgrade menu when two `TapEvent`s land on the same building (`tile.build` identity) within the configured tap-interval window, and SHALL treat taps on different buildings as starting a new pair window.

#### Scenario: Second tap within interval opens menu
- **WHEN** the feature is enabled in an active game and the player taps a valid upgradeable building twice within the tap-interval
- **THEN** the upgrade menu opens for that building

#### Scenario: Taps on different buildings do not open menu
- **WHEN** two consecutive taps land on different buildings
- **THEN** no menu opens and the second tap becomes the first tap of a new pair window

#### Scenario: Slow second tap does not open menu
- **WHEN** the second tap arrives after the tap-interval window expires
- **THEN** no menu opens and the second tap becomes the first tap of a new pair window

#### Scenario: Multi-tile building tapped on different tiles
- **WHEN** two taps land on different tiles of the same multi-tile building within the interval
- **THEN** the menu opens for that building

#### Scenario: Triple tap triggers only once
- **WHEN** three rapid taps land on the same building
- **THEN** the menu opens once on the second tap and the third tap starts a new pair window without immediately reopening

### Requirement: Tap guards and menu interaction
The system SHALL only evaluate double-tap when the feature is enabled, a game is active, the tapped tile has a building on the player's team in an upgradeable group, no UI consumes the pointer, and the player is not placing a building; the upgrade menu SHALL stack above vanilla selection and dismiss on outside tap, Escape/Back, or invalid tile.

#### Scenario: Guarded taps are ignored
- **WHEN** a tap arrives while disabled, in menu state, on an enemy/neutral or non-upgradeable block, with `scene.hasMouse()`, or while `input.isBuilding`
- **THEN** no menu opens and pair state resets

#### Scenario: Menu stacks over vanilla selection
- **WHEN** the second tap opens the upgrade menu
- **THEN** vanilla building selection remains untouched with the upgrade menu layered above it in `hudGroup`

#### Scenario: Outside tap dismisses menu
- **WHEN** the menu is open and a tap lands outside the menu
- **THEN** the menu closes

#### Scenario: Opening tap is not treated as dismiss tap
- **WHEN** the second tap opens the menu
- **THEN** that same tap does not immediately dismiss the menu

### Requirement: Tap-interval setting replaces hold-duration
The system SHALL provide a `tap-interval` integer setting (milliseconds, range 150–600, default 300) controlling the double-tap window, SHALL remove the `hold-duration` setting, and SHALL NOT migrate old `hold-duration` persisted values.

#### Scenario: Default interval is 300ms
- **WHEN** the feature loads with no saved setting
- **THEN** the tap-interval is 300ms

#### Scenario: Interval is tunable
- **WHEN** the player moves the tap-interval slider in settings
- **THEN** the double-tap window updates reactively

#### Scenario: Old hold-duration is gone
- **WHEN** the player opens Smart Upgrade settings
- **THEN** no hold-duration control is shown and only the tap-interval control is present

### Requirement: Keybind and upgrade behavior unchanged
The system SHALL keep the `smartUpgradeTrigger` (`U`) hover path, upgrade-candidate computation, chain upgrade, max-updates / same-type / traverse-bridges settings, menu layout, tile highlight, and toasts behaving as before.

#### Scenario: U keybind still opens menu
- **WHEN** the player presses `U` while hovering a valid upgradeable building
- **THEN** the upgrade menu opens for that tile

#### Scenario: Chain upgrade unaffected
- **WHEN** the player picks a target block from the menu
- **THEN** the connected chain upgrades exactly as before the trigger change

### Requirement: Internationalization
The system SHALL resolve all user-visible Smart Upgrade trigger strings (help text, tap-interval label, settings title, reset, toasts) from `bundle.properties`, SHALL NOT hardcode display text in Java, SHALL document each new key with a translator comment including placeholder meanings, and SHALL ship matching VI entries.

#### Scenario: Help describes double-tap
- **WHEN** the player views Smart Upgrade help
- **THEN** the text describes double-tap (not long-press) plus the `U` keybind

#### Scenario: New keys carry translator comments
- **WHEN** a new user-visible trigger string is introduced
- **THEN** a corresponding key with a descriptive translator comment is added to `bundle.properties` with a matching VI value
