## MODIFIED Requirements

### Requirement: Mod-Wide Settings Config Group
The mod SHALL expose a dedicated `ModSettings` class that holds a `ConfigGroup` namespaced under `mindustrytool.settings` and declares global `ConfigValue` entries as public static fields, including `betaParticipate: ConfigValue<Boolean>` (default `false`) and `sharePresence: ConfigValue<Boolean>` (default `true`). The legacy `freeCamera: ConfigValue<Boolean>` field SHALL be deprecated and synchronized with `FreeCameraFeature.get().enabled()`.

#### Scenario: Beta flag is false by default
- **WHEN** `ModSettings.betaParticipate` is read without any prior user interaction
- **THEN** `ModSettings.betaParticipate.get()` returns `false`

#### Scenario: Beta flag persists across sessions
- **WHEN** the user sets `ModSettings.betaParticipate` to `true` and the game restarts
- **THEN** `ModSettings.betaParticipate.get()` returns `true` on the next load

#### Scenario: Share presence flag is true by default
- **WHEN** `ModSettings.sharePresence` is read without prior interaction
- **THEN** `ModSettings.sharePresence.get()` returns `true`

#### Scenario: Free camera flag mirrors FreeCameraFeature
- **WHEN** `ModSettings.freeCamera` is read
- **THEN** its value reflects `FreeCameraFeature.get().isEnabled()`

#### Scenario: Global settings persist across sessions
- **WHEN** the user changes `sharePresence` or `betaParticipate` and the game restarts
- **THEN** the modified values persist on subsequent loads

### Requirement: General Settings Dialog Rendering
The `GeneralSettingsDialog` SHALL extend `SolimDialog` and host a `GeneralSettingsView` component with a centered and width-constrained layout (`maxWidth(500f)`). The view SHALL render a scrollable vertical list of setting rows for mod-wide preferences without horizontal overflow or hardcoded fixed width, including beta updates and share game status. Each row SHALL display a label (from `Core.bundle`), optional description or tooltip, and a `checkBox` bound directly to the corresponding `ConfigValue.signal()`. The dialog SHALL have a "Settings" title (bundle key `dialog.general-settings.title`) and a close button.

#### Scenario: Dialog opens with correct initial toggle state
- **WHEN** `GeneralSettingsDialog` is shown
- **THEN** each checkbox reflects the current value of its corresponding `ModSettings` config

#### Scenario: Toggling checkbox persists immediately
- **WHEN** the user clicks any setting checkbox in `GeneralSettingsDialog`
- **THEN** the corresponding `ModSettings` config reflects the new value without an additional confirm action and persists to `Core.settings`
