# feature-development-flag Specification

## Purpose
Lifecycle flag marking features as in-development placeholders: always visible in settings with a badge, locked against enabling, excluded from Quick Access, covering legacy features awaiting full rewrites.
## Requirements
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
The system SHALL register 16 metadata-only stub features with `development=true` and no logic or dialogs, porting old ids, icons, orders, `enabledByDefault`, and `quickAccess`: player-connect, health-bar, pathfinding, range-display, pretty-chat, autoplay, wave-preview, save-sync, item-visualizer, god-mode, smart-drill, smart-upgrade, music, progress-display, toggle-rendering, time-control. Old `chat-translation` SHALL NOT get a stub (covered by the existing `translation` feature).

#### Scenario: Registry contains stubs alongside real features
- **WHEN** the mod starts and `Main` registers features
- **THEN** `FeatureManager.getFeatures()` contains the 8 existing features plus the 16 development stubs sorted by metadata order

#### Scenario: Stub exposes metadata only
- **WHEN** a stub is inspected
- **THEN** it reports its ported id, icon, order, and `development=true`, returns `null` setting and main dialogs, and its `onEnable`/`onDisable` perform no game logic

#### Scenario: Time-control icon fallback
- **WHEN** the `clock.png` asset is not yet supplied
- **THEN** the time-control stub displays a temporary `Icon.*` fallback and remains otherwise complete, so the asset can be swapped in later with no logic change

### Requirement: Localized development badge and stub text
The system SHALL localize the development badge and every stub's name, description, and help under fresh `feature.<id>.*` keys, ignoring old bundle keys. Each key SHALL carry a translator comment per project i18n rules.

#### Scenario: Badge text resolves
- **WHEN** a development card renders its badge
- **THEN** the text resolves from `feature.status.in-development` via `Core.bundle`

#### Scenario: Stub name, description, and help resolve
- **WHEN** a stub card or help dialog renders
- **THEN** `feature.<id>.name`, `feature.<id>.description`, and `feature.<id>.help` resolve from the bundle with no hardcoded user-visible text

### Requirement: Registry size test coverage
Count-sensitive tests SHALL reflect the enlarged registry of 8 real features plus 16 locked stubs.

#### Scenario: Feature count assertions updated
- **WHEN** the test suite runs after registration
- **THEN** expectations account for 24 registered features (or explicitly filter out development features where the test targets enabled-capable features only)
