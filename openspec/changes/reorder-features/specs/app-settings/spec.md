## MODIFIED Requirements

### Requirement: Legacy stub registry

The system SHALL register 14 metadata-only stub features with `development=true` and no logic or dialogs, porting old ids, icons, orders, `enabledByDefault`, and `quickAccess`: player-connect, pathfinding, range-display, pretty-chat, autoplay, wave-preview, save-sync, item-visualizer, god-mode, smart-drill, smart-upgrade, music, progress-display, toggle-rendering. `time-control` is no longer a stub (it is a real enable-capable feature defined by the `time-control` capability). `health-bar` is no longer a stub (it is a real enable-capable feature defined by the `health-bar` capability). Old `chat-translation` SHALL NOT get a stub (covered by the existing `translation` feature).

#### Scenario: Registry contains stubs alongside real features

- **WHEN** the mod starts and `Main` registers features
- **THEN** `FeatureManager.getFeatures()` contains the 10 existing features plus the 14 development stubs ordered with non-development features in persisted ordered-ID list sequence followed by development stubs sorted by feature id

#### Scenario: Stub exposes metadata only

- **WHEN** a stub is inspected
- **THEN** it reports its ported id, icon, order, and `development=true`, returns `null` setting and main dialogs, and its `onEnable`/`onDisable` perform no game logic
