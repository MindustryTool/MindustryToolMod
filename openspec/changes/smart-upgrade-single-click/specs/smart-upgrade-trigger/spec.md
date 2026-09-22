## ADDED Requirements

### Requirement: Armed single-click opens upgrade menu
The system SHALL open the Smart Upgrade menu on a single valid `TapEvent` when the feature is both enabled AND armed, SHALL toggle the armed state on QuickAccess tap (auto-enabling the feature if disabled), and SHALL show a toast instruction plus highlight the QuickAccess icon while armed.

#### Scenario: Armed single tap opens menu
- **WHEN** the feature is enabled and armed and the player single-taps a valid upgradeable building
- **THEN** the upgrade menu opens for that building with no timing window

#### Scenario: Disarmed tap does not open menu
- **WHEN** the player taps a valid upgradeable building while disarmed
- **THEN** no menu opens and only vanilla selection applies

#### Scenario: Disabled feature does not trigger even when armed flag is set
- **WHEN** a tap arrives while the feature is disabled
- **THEN** no menu opens

#### Scenario: One-shot disarms after opening
- **WHEN** the trigger mode is one-shot and a single tap opens the menu
- **THEN** the feature disarms immediately after opening

#### Scenario: Persistent stays armed across triggers
- **WHEN** the trigger mode is persistent and a single tap opens the menu
- **THEN** the feature remains armed for further single-tap triggers until manually disarmed

#### Scenario: QuickAccess tap toggles armed state
- **WHEN** the player taps the Smart Upgrade QuickAccess icon
- **THEN** armed state toggles, arming auto-enables a disabled feature, and long-press still opens settings

#### Scenario: Arming cancelled explicitly or on state change
- **WHEN** the player taps outside a target, presses Escape/Back, re-taps the QuickAccess icon, returns to menu state, or the feature is disabled
- **THEN** the feature disarms

### Requirement: Trigger-mode setting
The system SHALL provide a `trigger-mode` setting with values `one-shot` (default) and `persistent`, SHALL persist the selection, SHALL apply it reactively, and SHALL reset it in reset-to-defaults.

#### Scenario: Default mode is one-shot
- **WHEN** the feature loads with no saved setting
- **THEN** the trigger mode is one-shot

#### Scenario: Mode is tunable
- **WHEN** the player changes the trigger-mode control in settings
- **THEN** the disarm-after-trigger behavior updates reactively

## MODIFIED Requirements

### Requirement: Tap guards and menu interaction
The system SHALL only evaluate single-click when the feature is enabled AND armed, a game is active, the tapped tile has a building on the player's team in an upgradeable group, no UI consumes the pointer, and the player is not placing a building; the upgrade menu SHALL stack above vanilla selection and dismiss on outside tap, Escape/Back, or invalid tile.

#### Scenario: Guarded taps are ignored
- **WHEN** a tap arrives while disabled, while disarmed, in menu state, on an enemy/neutral or non-upgradeable block, with `scene.hasMouse()`, or while `input.isBuilding`
- **THEN** no menu opens

#### Scenario: Menu stacks over vanilla selection
- **WHEN** an armed single tap opens the upgrade menu
- **THEN** vanilla building selection remains untouched with the upgrade menu layered above it in `hudGroup`

#### Scenario: Outside tap dismisses menu
- **WHEN** the menu is open and a tap lands outside the menu
- **THEN** the menu closes

#### Scenario: Opening tap is not treated as dismiss tap
- **WHEN** the triggering tap opens the menu
- **THEN** that same tap does not immediately dismiss the menu

### Requirement: Keybind and upgrade behavior unchanged
The system SHALL keep the `smartUpgradeTrigger` (`U`) hover path working on enabled alone while ignoring armed state, and SHALL keep upgrade-candidate computation, chain upgrade, max-updates / same-type / traverse-bridges settings, menu layout, tile highlight, and toasts behaving as before.

#### Scenario: U keybind still opens menu without arming
- **WHEN** the player presses `U` while hovering a valid upgradeable building and the feature is enabled but disarmed
- **THEN** the upgrade menu opens for that tile

#### Scenario: Chain upgrade unaffected
- **WHEN** the player picks a target block from the menu
- **THEN** the connected chain upgrades exactly as before the trigger change

### Requirement: Internationalization
The system SHALL resolve all user-visible Smart Upgrade trigger strings (help text, trigger-mode label, armed toast, settings title, reset, toasts) from `bundle.properties`, SHALL NOT hardcode display text in Java, SHALL document each new key with a translator comment including placeholder meanings, and SHALL ship matching VI entries.

#### Scenario: Help describes armed single-click
- **WHEN** the player views Smart Upgrade help
- **THEN** the text describes arming via QuickAccess plus single-tap and the `U` keybind

#### Scenario: New keys carry translator comments
- **WHEN** a new user-visible trigger string is introduced
- **THEN** a corresponding key with a descriptive translator comment is added to `bundle.properties` with a matching VI value

## REMOVED Requirements

### Requirement: Double-tap opens upgrade menu on same building
**Reason**: The pair-timing window is inaccessible for motor-impaired and mobile players; replaced by the armed single-click trigger.
**Migration**: Arm via the QuickAccess icon then single-tap the building, or press `U` while hovering when enabled.

### Requirement: Tap-interval setting replaces hold-duration
**Reason**: No timing window remains once double-tap is removed, so the interval control has no meaning.
**Migration**: None (persisted values are dropped, matching the earlier `hold-duration` removal precedent); use the new `trigger-mode` setting instead.
