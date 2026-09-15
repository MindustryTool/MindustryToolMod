## MODIFIED Requirements

### Requirement: Feature Interaction via Quick Access HUD

The system SHALL display buttons for features that support quick access and are not in development, allowing toggling feature state and opening feature settings. For features in popup display mode, tapping the button SHALL open the feature popup instead of toggling; long-press SHALL still open settings.

#### Scenario: Single click toggles feature

- **WHEN** the player clicks a feature button on the Quick Access HUD for a feature in HUD display mode
- **THEN** the target feature's enabled state is toggled between enabled and disabled.

#### Scenario: Single click opens popup in popup mode

- **WHEN** the player clicks a feature button on the Quick Access HUD for a feature in popup display mode
- **THEN** the feature popup opens and the enabled state is unchanged.

#### Scenario: Long press opens feature settings

- **WHEN** the player long-presses a feature button on the Quick Access HUD for 300ms or longer
- **THEN** the target feature's settings dialog is displayed if available.

#### Scenario: Development features excluded from HUD

- **WHEN** a feature's metadata is in development, even when marked quick-access capable
- **THEN** no button for that feature appears on the Quick Access HUD and it is not offered in Quick Access visibility settings.

### Requirement: Legacy stub registry

The system SHALL register 13 metadata-only stub features with `development=true` and no logic or dialogs, porting old ids, icons, orders, `enabledByDefault`, and `quickAccess`: player-connect, pathfinding, range-display, pretty-chat, autoplay, wave-preview, save-sync, item-visualizer, smart-drill, smart-upgrade, music, progress-display, toggle-rendering. `god-mode` is no longer a stub (it is a real enable-capable placeholder shell defined by the `god-mode` capability). `time-control` is no longer a stub (it is a real enable-capable feature defined by the `time-control` capability). `health-bar` is no longer a stub (it is a real enable-capable feature defined by the `health-bar` capability). Old `chat-translation` SHALL NOT get a stub (covered by the existing `translation` feature).

#### Scenario: Registry contains stubs alongside real features

- **WHEN** the mod starts and `Main` registers features
- **THEN** `FeatureManager.getFeatures()` contains the 11 existing features plus the 13 development stubs sorted by metadata order

#### Scenario: Stub exposes metadata only

- **WHEN** a stub is inspected
- **THEN** it reports its ported id, icon, order, and `development=true`, returns `null` setting and main dialogs, and its `onEnable`/`onDisable` perform no game logic

### Requirement: Registry size test coverage

Count-sensitive tests SHALL reflect the enlarged registry of 11 real features plus 13 locked stubs.

#### Scenario: Feature count assertions updated

- **WHEN** the test suite runs after registration
- **THEN** expectations account for 24 registered features (or explicitly filter out development features where the test targets enabled-capable features only)
