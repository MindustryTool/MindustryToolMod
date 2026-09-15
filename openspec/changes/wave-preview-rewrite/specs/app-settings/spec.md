## MODIFIED Requirements

### Requirement: Legacy stub registry

The system SHALL register 13 metadata-only stub features with `development=true` and no logic or dialogs, porting old ids, icons, orders, `enabledByDefault`, and `quickAccess`: player-connect, pathfinding, range-display, pretty-chat, autoplay, save-sync, item-visualizer, god-mode, smart-drill, smart-upgrade, music, progress-display, toggle-rendering. `wave-preview` is no longer a stub (it is a real enable-capable feature defined by the `wave-preview` capability). `time-control` is no longer a stub (it is a real enable-capable feature defined by the `time-control` capability). `health-bar` is no longer a stub (it is a real enable-capable feature defined by the `health-bar` capability). Old `chat-translation` SHALL NOT get a stub (covered by the existing `translation` feature).

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
