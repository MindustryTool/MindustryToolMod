# quick-access Specification

## Purpose
Provides an in-game overlay HUD allowing players to quickly toggle features, open feature settings, and customize the HUD's position, opacity, scale, and layout.

## Requirements

### Requirement: Quick Access Overlay HUD Display
The system SHALL display an in-game Quick Access overlay HUD containing feature action buttons when the `quick-access` feature is enabled.

#### Scenario: HUD added to game scene when enabled
- **WHEN** the `quick-access` feature is enabled during active gameplay
- **THEN** the Quick Access overlay view is added to `Vars.ui.hudGroup` and remains visible while `Vars.ui.hudfrag.shown` is true and `Vars.state.isGame()` is true.

#### Scenario: HUD removed when feature is disabled
- **WHEN** the `quick-access` feature is disabled
- **THEN** the Quick Access overlay view is removed from `Vars.ui.hudGroup`.

### Requirement: Quick Access Drag & Repositioning
The system SHALL allow players to drag the Quick Access HUD via its anchor button and persist its position across game sessions and screen orientation changes.

#### Scenario: Dragging anchor moves the HUD
- **WHEN** the player drags the anchor move button of the Quick Access HUD
- **THEN** the HUD coordinates update, remain within screen bounds, and persist position settings under separate portrait/landscape configuration keys.

### Requirement: Feature Interaction via Quick Access HUD
The system SHALL display buttons for features that support quick access and are not in development, allowing toggling feature state and opening feature settings.

#### Scenario: Single click toggles feature
- **WHEN** the player clicks a feature button on the Quick Access HUD
- **THEN** the target feature's enabled state is toggled between enabled and disabled.

#### Scenario: Long press opens feature settings
- **WHEN** the player long-presses a feature button on the Quick Access HUD for 300ms or longer
- **THEN** the target feature's settings dialog is displayed if available.

#### Scenario: Development features excluded from HUD
- **WHEN** a feature's metadata is in development, even when marked quick-access capable
- **THEN** no button for that feature appears on the Quick Access HUD and it is not offered in Quick Access visibility settings.

### Requirement: Quick Access Settings Configuration
The system SHALL provide a settings dialog to customize HUD opacity, scale, grid columns, and individual feature visibility on the Quick Access HUD.

#### Scenario: Changing HUD parameters updates HUD reactively
- **WHEN** the user modifies opacity, scale, columns, or feature visibility in the Quick Access settings dialog
- **THEN** the HUD updates its styling and layout reactively and saves settings immediately.

#### Scenario: Declarative Settings Dialog Structure
- **WHEN** `QuickAccessSettingsDialog` is constructed
- **THEN** it configures slider and checkbox inputs without manual `.subscribe()` calls or temporary single-use local variables, utilizing declarative chained row layouts (`row().gap(...).children(...)`).

### Requirement: Internationalization (i18n) Support
The system SHALL load all user-visible display text for Quick Access from translation bundle properties.

#### Scenario: Loading display text from bundle
- **WHEN** Quick Access feature names, tooltips, dialog titles, or setting labels are rendered
- **THEN** all strings are resolved from `assets/bundles/bundle.properties` using `Core.bundle.get` or `Core.bundle.format`.
