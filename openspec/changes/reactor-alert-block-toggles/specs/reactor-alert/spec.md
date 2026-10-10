## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Per-block monitoring toggles

The system SHALL persist an individual monitoring toggle per explosive block, keyed by block name and defaulting to enabled, and SHALL expose the toggles in the feature's settings dialog as labeled rows showing the block icon, localized name, explosion radius, and checkbox.

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
- **THEN** no listed block is monitored and no reactor alerts are shown
