## ADDED Requirements

### Requirement: Feature availability

The system SHALL provide a `grief-alert` feature that is enabled by default, and SHALL register it with the mod's feature manager.

#### Scenario: Feature is enabled by default

- **WHEN** the mod is loaded for the first time
- **THEN** the Grief Alert feature is enabled without user action

### Requirement: Explosive reactor detection

The system SHALL identify a block as an explosive reactor when it carries the reactor flag OR exposes a positive explosion radius, so that both vanilla and modded explosive reactors are covered, and SHALL only evaluate alerts for explosive reactors whose per-block monitoring toggle is enabled.

#### Scenario: Reactor flag detected

- **WHEN** a block carrying `BlockFlag.reactor` is constructed
- **THEN** it is treated as an explosive reactor candidate subject to its monitoring toggle

#### Scenario: Explosion radius detected

- **WHEN** a block with `explosionRadius > 0` that does not carry the reactor flag is constructed
- **THEN** it is treated as an explosive reactor candidate subject to its monitoring toggle

#### Scenario: Non-explosive block ignored

- **WHEN** a block without the reactor flag and without a positive explosion radius is constructed
- **THEN** no alert is evaluated for it

#### Scenario: Monitoring disabled for block

- **WHEN** an explosive reactor whose monitoring toggle is disabled begins construction near a core
- **THEN** no alert is shown for it

#### Scenario: Monitoring enabled for block

- **WHEN** an explosive reactor whose monitoring toggle is enabled begins construction near a core
- **THEN** the alert is evaluated normally

### Requirement: Construction-start trigger

The system SHALL evaluate alerts when construction of a block begins, and SHALL resolve the block being constructed from the construct build rather than the tile's current block.

#### Scenario: Alert evaluated on construction begin

- **WHEN** construction of an explosive reactor begins
- **THEN** the system evaluates the alert using the block being constructed

#### Scenario: Deconstruction begin ignored

- **WHEN** a construction-begin event is raised for deconstruction, indicated by the breaking flag
- **THEN** the system does not evaluate an alert

#### Scenario: Completion without begin is not required

- **WHEN** a block finishes construction without a begin evaluation having occurred
- **THEN** the system is not required to emit an alert for that placement

### Requirement: Builder attribution

The system SHALL ignore the local player's own placements, SHALL attribute alerts to the responsible player when one can be resolved, and SHALL fall back to the builder's team name when no player can be resolved.

#### Scenario: Local player placement ignored

- **WHEN** the local player begins constructing an explosive reactor near a core
- **THEN** no alert is shown

#### Scenario: Another player's placement attributed

- **WHEN** another player begins constructing an explosive reactor near a core
- **THEN** the alert identifies that player by name

#### Scenario: Artificial builder falls back to team

- **WHEN** a builder with no resolvable player begins constructing an explosive reactor near a core
- **THEN** the alert identifies the builder's team instead of a player

### Requirement: Distance to builder's team cores

The system SHALL measure the center-to-center Euclidean distance in tiles between the constructed reactor and the cores belonging to the builder's team, and SHALL alert when the nearest such core is within the configured radius.

#### Scenario: Core within radius

- **WHEN** the nearest core of the builder's team is within the configured radius
- **THEN** the alert is shown and identifies the target block's tile coordinates and the nearest core's tile coordinates

#### Scenario: Core outside radius

- **WHEN** every core of the builder's team is farther than the configured radius
- **THEN** no alert is shown

#### Scenario: No cores for the builder's team

- **WHEN** the builder's team has no live cores
- **THEN** no alert is shown

#### Scenario: Other teams' cores are not considered

- **WHEN** only cores belonging to a different team are within the configured radius
- **THEN** no alert is shown

### Requirement: Configurable radius

The system SHALL persist a configurable alert radius defaulting to 10 tiles, adjustable from 1 to 30 tiles through the feature's settings dialog.

#### Scenario: Default radius

- **WHEN** the feature has no stored radius
- **THEN** the alert radius is 10 tiles

#### Scenario: Radius adjusted in settings

- **WHEN** the player changes the radius slider and a matching placement occurs
- **THEN** the alert threshold uses the newly configured radius

### Requirement: Repeat throttling

The system SHALL suppress alerts raised within 3 seconds of a previous alert.

#### Scenario: Rapid second alert suppressed

- **WHEN** two qualifying placements occur within 3 seconds of each other
- **THEN** only the first alert is shown

#### Scenario: Alert after cooldown shown

- **WHEN** a qualifying placement occurs more than 3 seconds after the previous alert
- **THEN** the alert is shown

### Requirement: Alert presentation

The system SHALL present the alert as a red-colored message in the chat fragment (or HUD toast when offline) containing the builder, the constructed block's localized name, the target block's tile coordinates `(targetX, targetY)`, and the nearest core's tile coordinates `(coreX, coreY)`.

#### Scenario: Message contents

- **WHEN** an alert is emitted
- **THEN** the alert message contains the builder (player or team), the constructed block's localized name, the target tile coordinates, and the nearest core tile coordinates

#### Scenario: Red color applied

- **WHEN** the alert message is rendered
- **THEN** the message is colored red using inline markup

#### Scenario: UI unavailable

- **WHEN** the chat fragment and HUD fragment are not available
- **THEN** the system does not throw and simply skips presentation

### Requirement: Internationalization

The system SHALL resolve all user-visible Grief Alert strings from the translation bundle under `feature.grief-alert.*`.

#### Scenario: Strings resolve from bundle

- **WHEN** the feature name, description, settings label, or alert message is displayed
- **THEN** it resolves from a `feature.grief-alert.*` translation key rather than hardcoded text

#### Scenario: New keys carry comments and placeholders

- **WHEN** a new Grief Alert translation key is introduced
- **THEN** a descriptive translator comment is added above it and its placeholders are documented

### Requirement: Feature icon asset

The system SHALL use the `triangle-alert` icon for the feature.

#### Scenario: Icon resolution

- **WHEN** the feature icon is loaded
- **THEN** it resolves the `triangle-alert` icon asset, falling back gracefully if the asset is absent

#### Scenario: Missing icon does not break the feature

- **WHEN** the `triangle-alert` asset is not present
- **THEN** the feature still loads and operates using the fallback icon

### Requirement: Per-block monitoring toggles

The system SHALL persist an individual monitoring toggle per explosive block under `mindustrytool.features.grief-alert.block.<blockName>`, defaulting to enabled, and SHALL expose the toggles in the feature's settings dialog as labeled rows showing the block icon, localized name, explosion radius, and checkbox.

#### Scenario: Toggle persisted per block

- **WHEN** the player disables monitoring for a block and restarts the game
- **THEN** the toggle remains disabled

#### Scenario: Unknown block defaults to monitored

- **WHEN** an explosive block with no stored toggle begins construction near a core
- **THEN** the alert is evaluated normally

#### Scenario: Rows show block identity and radius

- **WHEN** the settings dialog renders the monitored-blocks section
- **THEN** each row shows the block icon, localized name, explosion radius, and its checkbox state

#### Scenario: Only placeable blocks listed

- **WHEN** the settings dialog builds the monitored-blocks section
- **THEN** blocks players cannot place are excluded from the list

### Requirement: Block list discovery

The system SHALL discover the monitored-blocks list by scanning loaded content with the explosive-reactor predicate when the settings dialog opens, and SHALL rebuild the event-time lookup lazily when absent.

#### Scenario: Modded reactor discovered

- **WHEN** a modded explosive reactor is loaded and the settings dialog opens
- **THEN** it appears in the monitored-blocks section defaulting to enabled

#### Scenario: Lookup rebuilt lazily

- **WHEN** a placement is evaluated without a built lookup
- **THEN** the lookup is rebuilt from loaded content before evaluation

### Requirement: Bulk monitoring actions

The system SHALL provide All and None actions that enable or mute every listed explosive block at once.

#### Scenario: Enable all blocks

- **WHEN** the player activates the All action
- **THEN** every listed block becomes monitored

#### Scenario: Mute all blocks

- **WHEN** the player activates the None action
- **THEN** no listed block is monitored and no grief alerts are shown
