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

The system SHALL delegate Quick Access HUD button long-press (>= 300ms) to `Feature#onQuickAccessLongClick(@Nullable Element anchor)` passing the active Quick Access HUD root element resolved dynamically at long-click time as the anchor. By default in `Feature`, `onQuickAccessLongClick` SHALL delegate to `onQuickAccessLongClick()`, which opens `getSettingDialog()` if available; if `getSettingDialog()` is null but `getMainDialog()` is non-null, it SHALL open `getMainDialog()`. Features MAY override `onQuickAccessLongClick` to customize hold behavior.

#### Scenario: Long-press opens setting dialog when present
- **WHEN** user long-presses (>= 300ms) a HUD feature button whose feature has non-null `getSettingDialog()`
- **THEN** `onQuickAccessLongClick` executes and that setting dialog is shown

#### Scenario: Long-press falls back to main dialog when setting dialog is null
- **WHEN** user long-presses (>= 300ms) a HUD feature button whose feature has null `getSettingDialog()` and non-null `getMainDialog()`
- **THEN** `onQuickAccessLongClick` executes and that main dialog is shown

#### Scenario: Long-press no-op when both dialogs are null
- **WHEN** user long-presses (>= 300ms) a HUD feature button whose feature has both dialogs null
- **THEN** no dialog is opened and no exception occurs

#### Scenario: Long-press anchor element is non-null when HUD is active
- **WHEN** user long-presses (>= 300ms) any feature button on the active Quick Access HUD
- **THEN** the anchor passed to `onQuickAccessLongClick(anchor)` is the non-null Quick Access HUD root element

### Requirement: HUD button click delegation to Feature

The system SHALL delegate Quick Access HUD button single-clicks directly to `Feature#onQuickAccessClick(@Nullable Element anchor)` passing the active Quick Access HUD root element resolved dynamically at click time as the anchor. In `Feature`, `onQuickAccessClick(@Nullable Element anchor)` SHALL delegate to `onQuickAccessClick()`, which defaults to toggling `setEnabled(!isEnabled())`. Features MAY override either method to customize click behavior.

#### Scenario: Default feature click toggles enabled state
- **WHEN** user clicks a feature button that uses default `Feature#onQuickAccessClick()`
- **THEN** the feature's enabled state is inverted (`setEnabled(!isEnabled())`)

#### Scenario: GodMode and TimeControl click toggles popup when in popup mode
- **WHEN** user clicks GodMode or TimeControl button in Quick Access HUD while in popup mode
- **THEN** the respective popup is toggled anchored to the active Quick Access HUD element

#### Scenario: GodMode and TimeControl click falls back to toggle when not in popup mode
- **WHEN** user clicks GodMode or TimeControl button in Quick Access HUD while not in popup mode
- **THEN** default click behavior is executed, toggling the feature's enabled state

#### Scenario: ChatFeature click toggles overlay collapse state
- **WHEN** user clicks the Chat button in Quick Access HUD
- **THEN** `ChatFeature` toggles its chat overlay collapse state (`collapsedConfig`)

#### Scenario: Anchor element is non-null when HUD is active
- **WHEN** user clicks any feature button on the active Quick Access HUD
- **THEN** the anchor passed to `onQuickAccessClick(anchor)` is the non-null Quick Access HUD root element

