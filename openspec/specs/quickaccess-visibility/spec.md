# quickaccess-visibility Specification

## Purpose

Feature visibility model for the Quick Access HUD based on dual overrides (`hidden` and `shown` string sets), metadata default visibility (`quickAccessByDefault`), and HUD button long-press fallback to `getMainDialog()`. Created by archiving change quickaccess-all-features.

## Requirements

**Source: quickaccess-all-features**

### Requirement: Feature metadata default QuickAccess visibility

The system SHALL support declaring default QuickAccess HUD visibility on `FeatureMetadata` via `quickAccessByDefault` (boolean, default false), with backward-compatible `quickAccess` builder and getter aliases.

#### Scenario: Feature specifies quickAccessByDefault
- **WHEN** a feature configures `quickAccessByDefault(true)`
- **THEN** `isQuickAccessByDefault()` and `isQuickAccess()` return true

#### Scenario: Feature omits quickAccessByDefault
- **WHEN** a feature does not configure `quickAccessByDefault`
- **THEN** `isQuickAccessByDefault()` and `isQuickAccess()` return false

### Requirement: Dual override HUD visibility persistence

The system SHALL persist user HUD visibility overrides in `QuickAccessFeature` using two independent string sets: `hidden` (for features with default true that user explicitly hides) and `shown` (for features with default false that user explicitly shows). Unset features SHALL fall back to their `quickAccessByDefault` metadata value.

#### Scenario: Default-on feature visible initially
- **WHEN** a feature has `quickAccessByDefault == true` and is neither in `hidden` nor in `shown`
- **THEN** `isFeatureVisible(id)` returns true

#### Scenario: Default-off feature hidden initially
- **WHEN** a feature has `quickAccessByDefault == false` and is neither in `hidden` nor in `shown`
- **THEN** `isFeatureVisible(id)` returns false

#### Scenario: User hides default-on feature
- **WHEN** user toggles a default-on feature off
- **THEN** the feature id is added to `hidden` and `isFeatureVisible(id)` returns false

#### Scenario: User shows default-off feature
- **WHEN** user toggles a default-off feature on
- **THEN** the feature id is added to `shown` and `isFeatureVisible(id)` returns true

#### Scenario: Reactivity on visibility changes
- **WHEN** either `hidden` or `shown` configuration updates
- **THEN** the QuickAccess HUD reactively reflows visible item buttons without rebuilding unchanged buttons

### Requirement: HUD button long-press dialog routing

The system SHALL open a feature's `getSettingDialog()` on long-press (>= 300ms) if available; if `getSettingDialog()` is null but `getMainDialog()` is non-null, the system SHALL open `getMainDialog()`.

#### Scenario: Feature has setting dialog
- **WHEN** player long-presses a feature button whose `getSettingDialog()` is non-null
- **THEN** the feature setting dialog is displayed

#### Scenario: Feature has main dialog and no setting dialog
- **WHEN** player long-presses a feature button whose `getSettingDialog()` is null and `getMainDialog()` is non-null
- **THEN** the feature main dialog is displayed
