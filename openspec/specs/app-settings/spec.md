# app-settings Specification

## Purpose

Mechanical merge of 4 specs per change `spec-domain-merge` (stage 5 settings, concat-then-dedupe). Sources: feature-settings-dialog, feature-development-flag, general-settings-dialog, quick-access. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work. Reactive config primitives (`config-value-signal`, `contextual-config-value`, `orientation-signal`) live in `solim-reactivity` and are not duplicated here.
## Requirements

**Source: feature-settings-dialog**

Declarative feature settings dialog and view built with Solim reactive signals and structural layout components to manage, filter, re-enable, and inspect mod features.

### Requirement: Declarative Search and Filter
The feature settings dialog SHALL provide a search input backed by a Solim reactive `Signal<String>` that filters mod features in real time across feature names, descriptions, and metadata IDs.

#### Scenario: User types search query
- **WHEN** the user enters text into the search field
- **THEN** the filter signal updates and the feature grid automatically updates to display only matching features using case-insensitive matching

#### Scenario: Empty search results state
- **WHEN** the search filter matches no registered features
- **THEN** an empty state indicator is rendered with localized text (`feature.search.empty`)

### Requirement: Bulk Feature Re-enablement
The feature settings dialog SHALL provide an action control to re-enable all registered features and reactively refresh the card display.

#### Scenario: Re-enable action triggered
- **WHEN** the user activates the re-enable button
- **THEN** `FeatureManager.reenable()` is invoked and all feature cards update their status reactively

### Requirement: Feature Card Display and Interaction
The dialog SHALL present each feature as an interactive `FeatureCard` component displaying its icon, title, description, enabled/disabled status, and action shortcuts, updating its visual state reactively in-place without component recreation. Cards for features in development SHALL additionally display a localized "In Development" badge and SHALL NOT toggle state on click.

#### Scenario: Toggle feature state
- **WHEN** the user clicks a feature card outside of its action shortcut buttons
- **THEN** the feature's enabled state is toggled and the card's background color, status label text, and status label color update immediately via direct reactive property bindings on the existing elements without rebuilding the card

#### Scenario: Feature action shortcut invocation
- **WHEN** the user clicks an action shortcut on a card (main dialog, settings dialog, or help)
- **THEN** the corresponding dialog is opened and the click event does not propagate to toggle the card's enabled state

#### Scenario: Instantiation without parent table
- **WHEN** a `FeatureCard` is created
- **THEN** it is instantiated via constructor without passing a parent `Table`, and its lifecycle and dimensions are managed reactively by its parent container

#### Scenario: Development badge displayed
- **WHEN** a card represents a feature whose metadata is in development
- **THEN** an "In Development" badge resolving from `feature.status.in-development` is rendered on the card alongside the normal icon, title, and description

#### Scenario: Locked card click is a no-op
- **WHEN** the user clicks a development feature card outside of its action shortcut buttons
- **THEN** the feature remains disabled, no setting is written, no state event fires, and the help shortcut remains available

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

**Source: feature-development-flag**

Lifecycle flag marking features as in-development placeholders: always visible in settings with a badge, locked against enabling, excluded from Quick Access, covering legacy features awaiting full rewrites.

### Requirement: Development lifecycle flag on feature metadata
The system SHALL provide a `development` boolean on `FeatureMetadata`, defaulting to `false`, settable via the builder. Existing features without the flag behave exactly as before.

#### Scenario: Default is not in development
- **WHEN** metadata is built without setting the development flag
- **THEN** `isDevelopment()` returns `false` and all existing enable, sort, and display behavior is unchanged

#### Scenario: Stub opts into development
- **WHEN** a stub builds metadata with development set to `true`
- **THEN** `isDevelopment()` returns `true` and the feature is treated as a locked placeholder everywhere

### Requirement: Development features are locked placeholders
A feature whose metadata is in development SHALL never become enabled: enabling is rejected, no settings are written, no state event fires, and startup initialization skips it.

#### Scenario: Enabling a development feature is a no-op
- **WHEN** `enable()` or `setEnabled(true)` is called on a development feature
- **THEN** the feature remains disabled, no `mindustrytool.feature.<id>.enabled` setting is written, and no `FeatureStateChanged` event fires

#### Scenario: Startup skips development features
- **WHEN** `FeatureManager.init()` runs with development stubs registered
- **THEN** `onEnable()` is never invoked on development features regardless of stored settings

#### Scenario: Bulk re-enable leaves development features disabled
- **WHEN** `FeatureManager.reenable()` or `disableAll()` runs
- **THEN** development features remain disabled and are not added to the persisted enabled-features list

### Requirement: Legacy stub registry

The system SHALL register 13 metadata-only stub features with `development=true` and no logic or dialogs, porting old ids, icons, orders, `enabledByDefault`, and `quickAccess`: player-connect, pathfinding, range-display, pretty-chat, autoplay, wave-preview, save-sync, item-visualizer, smart-drill, smart-upgrade, music, progress-display, toggle-rendering. `god-mode` is no longer a stub (it is a real enable-capable placeholder shell defined by the `god-mode` capability). `time-control` is no longer a stub (it is a real enable-capable feature defined by the `time-control` capability). `health-bar` is no longer a stub (it is a real enable-capable feature defined by the `health-bar` capability). Old `chat-translation` SHALL NOT get a stub (covered by the existing `translation` feature).

#### Scenario: Registry contains stubs alongside real features

- **WHEN** the mod starts and `Main` registers features
- **THEN** `FeatureManager.getFeatures()` contains the 11 existing features plus the 13 development stubs sorted by metadata order

#### Scenario: Stub exposes metadata only

- **WHEN** a stub is inspected
- **THEN** it reports its ported id, icon, order, and `development=true`, returns `null` setting and main dialogs, and its `onEnable`/`onDisable` perform no game logic

### Requirement: Localized development badge and stub text
The system SHALL localize the development badge and every stub's name, description, and help under fresh `feature.<id>.*` keys, ignoring old bundle keys. Each key SHALL carry a translator comment per project i18n rules.

#### Scenario: Badge text resolves
- **WHEN** a development card renders its badge
- **THEN** the text resolves from `feature.status.in-development` via `Core.bundle`

#### Scenario: Stub name, description, and help resolve
- **WHEN** a stub card or help dialog renders
- **THEN** `feature.<id>.name`, `feature.<id>.description`, and `feature.<id>.help` resolve from the bundle with no hardcoded user-visible text

### Requirement: Registry size test coverage

Count-sensitive tests SHALL reflect the enlarged registry of 11 real features plus 13 locked stubs.

#### Scenario: Feature count assertions updated

- **WHEN** the test suite runs after registration
- **THEN** expectations account for 24 registered features (or explicitly filter out development features where the test targets enabled-capable features only)

### Requirement: Mod-Wide Settings Config Group
The mod SHALL expose a dedicated `ModSettings` class that holds a `ConfigGroup` namespaced under `mindustrytool.settings` and declares all global `ConfigValue` entries as public static fields, including `betaParticipate: ConfigValue<Boolean>` (default `false`), `sharePresence: ConfigValue<Boolean>` (default `true`), and `freeCamera: ConfigValue<Boolean>` (default `false`).

#### Scenario: Beta flag is false by default
- **WHEN** `ModSettings.betaParticipate` is read without any prior user interaction
- **THEN** `ModSettings.betaParticipate.get()` returns `false`

#### Scenario: Beta flag persists across sessions
- **WHEN** the user sets `ModSettings.betaParticipate` to `true` and the game restarts
- **THEN** `ModSettings.betaParticipate.get()` returns `true` on the next load

#### Scenario: Share presence flag is true by default
- **WHEN** `ModSettings.sharePresence` is read without prior interaction
- **THEN** `ModSettings.sharePresence.get()` returns `true`

#### Scenario: Free camera flag is false by default
- **WHEN** `ModSettings.freeCamera` is read without prior interaction
- **THEN** `ModSettings.freeCamera.get()` returns `false`

#### Scenario: Global settings persist across sessions
- **WHEN** the user changes `sharePresence` or `freeCamera` and the game restarts
- **THEN** the modified values persist on subsequent loads

### Requirement: General Settings Dialog Rendering
The `GeneralSettingsDialog` SHALL extend `SolimDialog` and render a scrollable vertical list of setting rows for mod-wide preferences, including beta updates, share game status, and free camera. Each row SHALL display a label (from `Core.bundle`), optional description or tooltip, and a `checkBox` bound directly to the corresponding `ConfigValue.signal()`. The dialog SHALL have a "Settings" title (bundle key `dialog.general-settings.title`) and a close button.

#### Scenario: Dialog opens with correct initial toggle state
- **WHEN** `GeneralSettingsDialog` is shown
- **THEN** each checkbox reflects the current value of its corresponding `ModSettings` config

#### Scenario: Toggling checkbox persists immediately
- **WHEN** the user clicks any setting checkbox in `GeneralSettingsDialog`
- **THEN** the corresponding `ModSettings` config reflects the new value without an additional confirm action and persists to `Core.settings`

#### Scenario: Tooltip visible on hover
- **WHEN** the user hovers over a setting row
- **THEN** a tooltip or descriptive label is displayed explaining the preference

### Requirement: Beta Participation Drives Update Channel
When `ModSettings.betaParticipate.get()` is `false`, `UpdateService` SHALL consult it when processing the GitHub releases response as before: prereleases (entries where `"prerelease": true` in the JSON) SHALL be excluded from the changelog, and the stable `mod.hjson` version gate is unchanged. When the flag is `true`, `UpdateService` SHALL skip the `mod.hjson` fetch and determine the latest version solely from the GitHub releases list as the maximum tag over all entries (stable and prerelease) compared with `VersionUtils` semantics. If that latest tag is greater than the installed version, the update dialog SHALL be shown with the latest version displayed as the raw release tag (e.g. `v5.0.3-v8-beta`), a prerelease-inclusive changelog, and an Update action that installs that exact tag via the `githubImportMod(repo, isJava, release, forceEnable)` overload. Release fetch failure, an empty release list, and same-number ties after suffix stripping (e.g. `v5.0.3-v8` vs `v5.0.3-v8-beta`) SHALL resolve to silent (log and finish with no dialog).

#### Scenario: Beta off — prerelease excluded
- **WHEN** `ModSettings.betaParticipate.get()` is `false` and GitHub returns a prerelease newer than the installed version
- **THEN** the prerelease is ignored and no update dialog is shown (assuming no stable release is newer)

#### Scenario: Beta on — prerelease triggers dialog with raw tag
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the maximum release tag (e.g. `v5.0.3-v8-beta`) is greater than the installed version (e.g. `v5.0.1-v8`)
- **THEN** the update dialog is shown displaying the raw release tag, the changelog includes prereleases, and activating Update installs that exact tag

#### Scenario: Beta on — nothing newer stays silent
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and no release tag is greater than the installed version
- **THEN** no update dialog is shown

#### Scenario: Beta on — fetch failure or empty releases ignored
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the releases fetch fails or returns no usable entries
- **THEN** the failure is logged and finished silently with no dialog and no error popup

#### Scenario: Beta on — same-number tie stays silent
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the newest prerelease tag parses equal to the installed version (e.g. `v5.0.3-v8-beta` vs installed `v5.0.3-v8`)
- **THEN** no update dialog is shown

**Source: quick-access**

Provides an in-game overlay HUD allowing players to quickly toggle features, open feature settings, and customize the HUD's position, opacity, scale, and layout.

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

The system SHALL display buttons for features that support quick access and are not in development, allowing toggling feature state and opening feature settings. For features in popup display mode, tapping the button SHALL toggle the feature popup visibility (opening if closed, hiding if already showing or clicked again) instead of toggling feature enablement; long-press SHALL still open settings.

#### Scenario: Single click toggles feature

- **WHEN** the player clicks a feature button on the Quick Access HUD for a feature in HUD display mode
- **THEN** the target feature's enabled state is toggled between enabled and disabled.

#### Scenario: Single click opens popup in popup mode

- **WHEN** the player clicks a feature button on the Quick Access HUD for a feature in popup display mode while its popup is not showing
- **THEN** the feature popup opens and the enabled state is unchanged.

#### Scenario: Clicking button again hides popup in popup mode

- **WHEN** the player clicks a feature button on the Quick Access HUD for a feature in popup display mode while its popup is already showing
- **THEN** the feature popup closes and the feature enabled state is unchanged.

#### Scenario: Long press opens feature settings

- **WHEN** the player long-presses a feature button on the Quick Access HUD for 300ms or longer
- **THEN** the target feature's settings dialog is displayed if available.

#### Scenario: Development features excluded from HUD

- **WHEN** a feature's metadata is in development, even when marked quick-access capable
- **THEN** no button for that feature appears on the Quick Access HUD and it is not offered in Quick Access visibility settings.

### Requirement: Quick Access Settings Configuration
The system SHALL provide a settings dialog to customize HUD opacity, scale, grid columns, individual feature visibility, and feature display order on the Quick Access HUD.

#### Scenario: Changing HUD parameters updates HUD reactively
- **WHEN** the user modifies opacity, scale, columns, or feature visibility in the Quick Access settings dialog
- **THEN** the HUD updates its styling and layout reactively and saves settings immediately.

#### Scenario: Declarative Settings Dialog Structure
- **WHEN** `QuickAccessSettingsDialog` is constructed
- **THEN** it configures slider and checkbox inputs without manual `.subscribe()` calls or temporary single-use local variables, utilizing declarative chained row layouts (`row().gap(...).children(...)`).

#### Scenario: Reordering features updates HUD reactively
- **WHEN** the user activates a row's up or down arrow in the Quick Access settings dialog
- **THEN** the stored display order swaps the adjacent entries, both the settings rows and the HUD re-render in the new order, and the change persists.

### Requirement: Internationalization (i18n) Support
The system SHALL load all user-visible display text for Quick Access from translation bundle properties.

#### Scenario: Loading display text from bundle
- **WHEN** Quick Access feature names, tooltips, dialog titles, or setting labels are rendered
- **THEN** all strings are resolved from `assets/bundles/bundle.properties` using `Core.bundle.get` or `Core.bundle.format`.

