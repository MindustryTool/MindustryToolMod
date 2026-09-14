# feature-settings-dialog Delta Specification

## MODIFIED Requirements

### Requirement: Responsive Grid and Lifecycle Disposal
The feature settings dialog SHALL adapt its grid layout dynamically to screen size and preserve its view instance across show and hide events, delegating lifecycle disposal of view components, reactive property bindings, and event listeners to the underlying `SolimDialog` without implementing manual disposal in `FeatureSettingDialog`. The dialog SHALL also expose a "Settings" action button that opens `GeneralSettingsDialog`.

#### Scenario: Viewport size changed
- **WHEN** the window is resized while the dialog is visible
- **THEN** the layout column count is recalculated reactively, the grid reflows, and existing card instances are preserved

#### Scenario: Dialog hidden and reopened
- **WHEN** the dialog is hidden and subsequently shown again
- **THEN** the existing `FeatureSettingsView` instance is reused without disposal or recreation, and its width and data are refreshed via `onShown`

#### Scenario: Permanent dialog disposal
- **WHEN** the dialog is permanently disposed
- **THEN** all associated view components, reactive property bindings, and event listeners are cleanly disposed by `SolimDialog` without `FeatureSettingDialog` overriding `onDispose()`

#### Scenario: Settings button opens general settings
- **WHEN** the user clicks the "Settings" action button in `FeatureSettingDialog`
- **THEN** a `GeneralSettingsDialog` is shown over the current dialog
