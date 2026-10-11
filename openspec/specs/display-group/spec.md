# display-group Specification

## Purpose
TBD - created by archiving change display-group-popup. Update Purpose after archive.
## Requirements
### Requirement: DisplayGroupFeature QuickAccess integration
The system SHALL register `DisplayGroupFeature` with `id="display-group"`, icon `Icon.eye`, `enabledByDefault=true`, and `quickAccessByDefault=true`. In `DisplayGroupFeature`, tapping its QuickAccess button SHALL toggle the visibility of `DisplayGroupPopup`, and long-pressing (>= 300ms) SHALL open `DisplayGroupSettingsDialog`.

#### Scenario: Single click toggles popup visibility
- **WHEN** user taps the DisplayGroup button in the QuickAccess HUD
- **THEN** if `DisplayGroupPopup` is hidden, it opens anchored to the QuickAccess bar; if already showing, it closes

#### Scenario: Long click opens DisplayGroup settings dialog
- **WHEN** user long-presses (>= 300ms) the DisplayGroup button in the QuickAccess HUD
- **THEN** `DisplayGroupSettingsDialog` is displayed

### Requirement: DisplayGroupPopup anchored positioning and layout
The system SHALL display `DisplayGroupPopup` anchored to the QuickAccess bar using `QuickAccessPopupHelper`, clamped to screen bounds and layered behind the QuickAccess bar. The popup content SHALL render a reactive grid of visible display feature item buttons arranged in columns according to `colsConfig` (default 4).

#### Scenario: Popup renders anchored to QuickAccess bar
- **WHEN** `DisplayGroupPopup` is shown with a non-null QuickAccess bar anchor
- **THEN** the popup is positioned adjacent to the bar in the direction facing screen interior and kept within stage bounds

#### Scenario: Column reflow in popup
- **WHEN** `colsConfig` changes or screen bounds require reflow
- **THEN** the reactive grid updates its column layout according to the configured column count

### Requirement: Display overlay item button interaction and reactivity
Each display feature button rendered inside `DisplayGroupPopup` SHALL display the feature's icon, a tooltip with the feature's localized name, a highlight color reflecting active state (`Color.white` when enabled, `Color.darkGray` when disabled), and SHALL delegate single click to `feature.setEnabled(!feature.isEnabled())` and long click (>= 300ms) to opening that feature's setting dialog if available.

#### Scenario: Toggling a display feature from popup
- **WHEN** user clicks a feature button inside `DisplayGroupPopup`
- **THEN** that feature's enabled state toggles and its button icon color updates reactively without closing the popup

#### Scenario: Long-pressing a feature button from popup
- **WHEN** user long-presses (>= 300ms) a feature button inside `DisplayGroupPopup`
- **THEN** that feature's setting dialog opens if non-null

### Requirement: Display group configuration and order persistence
`DisplayGroupFeature` SHALL persist column count (`colsConfig`, default 4), custom display order (`displayOrderConfig`), and visibility sets (`hiddenFeaturesConfig`, `shownFeaturesConfig`). By default, the visible display items SHALL include `health-bar`, `range-display`, `pathfinding`, `progress-display`, `team-resource`, `visualizer`, `wave-preview`, and `toggle-rendering`.

#### Scenario: Reordering items in settings dialog
- **WHEN** user reorders display items in `DisplayGroupSettingsDialog`
- **THEN** `displayOrderConfig` updates and the popup item grid reflows in the new order

#### Scenario: Hiding a display item in settings dialog
- **WHEN** user unchecks a display item in `DisplayGroupSettingsDialog`
- **THEN** that item is added to `hiddenFeaturesConfig` and removed from `DisplayGroupPopup`

