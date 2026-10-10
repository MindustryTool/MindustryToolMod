## MODIFIED Requirements

### Requirement: Global Settings Persistence
Global preferences that span features SHALL be maintained in `ModSettings` backed by `ConfigGroup.of("mindustrytool.settings")`. Supported settings SHALL include `featureOrder` (ordered list of feature IDs), `favoriteFeatures` (set of favorite feature IDs), `betaParticipate` (boolean flag for beta releases), `sharePresence` (boolean flag for game presence sharing), and `universalScale` (float scaling factor defaulting to `0.8f` on mobile and `1.0f` on desktop).

#### Scenario: Global settings persist across sessions
- **WHEN** the user changes `sharePresence`, `betaParticipate`, or `universalScale` and the game restarts
- **THEN** the modified values persist on subsequent loads

#### Scenario: Universal scale defaults by platform
- **WHEN** `ModSettings` is initialized without a saved `universal-scale` setting
- **THEN** `universalScale` defaults to `0.8f` if `Vars.mobile` is true, and `1.0f` otherwise

### Requirement: General Settings Dialog Rendering
The `GeneralSettingsDialog` SHALL extend `SolimDialog` and host a `GeneralSettingsView` component with a centered and width-constrained layout (`maxWidth(500f)`). The view SHALL render a scrollable vertical list of setting rows for mod-wide preferences without horizontal overflow or hardcoded fixed width, including beta updates, share game status, and a universal scale slider. The universal scale row SHALL display a slider bound to `ModSettings.universalScale.signal()` with a range from `0.5f` to `1.5f` (step `0.05f`) and a percentage text display. The dialog SHALL have a "Settings" title (bundle key `dialog.general-settings.title`) and a close button.

#### Scenario: Dialog opens with correct initial toggle and slider state
- **WHEN** `GeneralSettingsDialog` is shown
- **THEN** each checkbox and the universal scale slider reflect the current values of their corresponding `ModSettings` configs

#### Scenario: Toggling checkbox persists immediately
- **WHEN** the user clicks any setting checkbox in `GeneralSettingsDialog`
- **THEN** the corresponding `ModSettings` config reflects the new value without an additional confirm action and persists to `Core.settings`

#### Scenario: Adjusting universal scale slider persists immediately
- **WHEN** the user adjusts the universal scale slider in `GeneralSettingsDialog`
- **THEN** `ModSettings.universalScale` reflects the new value immediately and persists to `Core.settings`

## ADDED Requirements

### Requirement: Effective Scale Calculation
`ModSettings` SHALL provide reactive and non-reactive utility methods to compute effective scale by multiplying `universalScale` with local feature scale values. When either scale changes, `ModSettings.effectiveScale(Readable<Float>)` SHALL reactively emit the combined product.

#### Scenario: Reactive effective scale updates dynamically
- **WHEN** `universalScale` changes while a feature has a local scale of `1.2f`
- **THEN** the `effectiveScale` signal emits the updated product of the new `universalScale` and `1.2f`

#### Scenario: Fallback when local scale is null
- **WHEN** a feature's local scale is null
- **THEN** `effectiveScale` treats the feature scale as `1.0f` and emits `universalScale * 1.0f`
