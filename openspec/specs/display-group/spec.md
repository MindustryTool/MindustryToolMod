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
`DisplayGroupFeature` SHALL persist column count (`colsConfig`, default 4), custom display order (`displayOrderConfig`, key `display-order`), and visibility overrides (`hiddenFeaturesConfig`, key `hidden`, and `shownFeaturesConfig`, key `shown`). The system SHALL NOT persist any list of default display members. The set of features recognized as display group members SHALL be determined solely by the `displayGroup` flag on each feature's metadata, and the default visibility of a member SHALL be derived from that same flag.

#### Scenario: Reordering items in settings dialog
- **WHEN** user reorders display items in `DisplayGroupSettingsDialog`
- **THEN** `displayOrderConfig` updates and the popup item grid reflows in the new order

#### Scenario: Hiding a display item in settings dialog
- **WHEN** user unchecks a display item in `DisplayGroupSettingsDialog`
- **THEN** that item is added to `hiddenFeaturesConfig` and removed from `DisplayGroupPopup`

#### Scenario: Members are visible in the popup by default
- **WHEN** a player has no visibility overrides recorded for a display group member
- **THEN** the item is visible in `DisplayGroupPopup`, because default visibility follows `isDisplayGroup()`

### Requirement: Display group membership declared on feature metadata
The system SHALL provide a `displayGroup` boolean on `FeatureMetadata`, defaulting to `false`, settable via the `displayGroup(boolean)` builder method and readable via `isDisplayGroup()`. `DisplayGroupFeature` SHALL derive its members by selecting every registered feature whose metadata reports `isDisplayGroup() == true`, and SHALL NOT resolve membership by comparing stored ids against a hardcoded list of feature ids. Features without the flag SHALL behave exactly as before.

#### Scenario: Default is not a display group member
- **WHEN** a feature builds metadata without setting the display group flag
- **THEN** `isDisplayGroup()` returns `false` and the feature is never rendered inside `DisplayGroupPopup`

#### Scenario: Overlay opts into the display group
- **WHEN** a feature builds metadata with `displayGroup` set to `true`
- **THEN** `isDisplayGroup()` returns `true` and the feature becomes a candidate for the popup and the group settings list

#### Scenario: Renaming a member does not drop it from the group
- **WHEN** a display group member changes its metadata id
- **THEN** the feature remains a display group member, because membership is resolved through the metadata object rather than by id lookup

#### Scenario: The eight display overlays are members
- **WHEN** `displayCandidates()` is evaluated with all features registered
- **THEN** it returns exactly `health-bar`, `range-display`, `pathfinding`, `blockstats-display`, `team-resources`, `visualizer`, `wave-preview`, and `toggle-rendering`

### Requirement: Default display group ordering derives from feature metadata
The system SHALL compute the default order of display group members by sorting members on `FeatureMetadata.getOrder()`, and SHALL NOT depend on the ordering of `FeatureManager.getFeatures()`. Reordering features in the main feature settings dialog SHALL NOT alter the display group ordering. A persisted `display-order` SHALL continue to take precedence over the derived default; entries that no longer resolve to a display group member SHALL be dropped, and members absent from the persisted order SHALL be appended at the end.

#### Scenario: Default ordering on a fresh install
- **WHEN** a player has no persisted `display-order` and opens the display group popup
- **THEN** members are rendered in ascending `FeatureMetadata.order`

#### Scenario: Reordering the main settings grid does not disturb the popup
- **WHEN** a player reorders features in the main feature settings dialog and then opens the display group popup
- **THEN** the popup renders members in the ordering established by `FeatureMetadata.order`, not the main grid ordering

#### Scenario: Persisted order survives membership changes
- **WHEN** a persisted `display-order` contains ids that are no longer display group members
- **THEN** those ids are dropped, existing member ids keep their relative sequence, and members absent from the persisted order are appended at the end

