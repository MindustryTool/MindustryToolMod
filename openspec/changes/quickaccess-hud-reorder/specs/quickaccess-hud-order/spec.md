## ADDED Requirements

### Requirement: Persisted display order list

The system SHALL persist the Quick Access feature display order as an ordered list of feature ids in a `ConfigValue<Seq<String>>` owned by `QuickAccessFeature` (key `display-order`, `OrderedSeqPersister`), independent of `FeatureMetadata.order` and `ModSettings.featureOrder`.

#### Scenario: Order survives restart
- **WHEN** the player rearranges features and the game restarts
- **THEN** the stored display order lists the same ids in the same positions

### Requirement: Lazy order normalization

The system SHALL heal the stored list on every read by dropping ids with no registered feature, dropping development feature ids, and appending missing registered quick-access non-development feature ids at the end, writing back only when the healed list differs.

#### Scenario: Stale ids removed
- **WHEN** the stored list contains an unregistered id
- **THEN** reads exclude it and persist the cleaned list

#### Scenario: New features appended at end
- **WHEN** a registered quick-access non-development feature id is absent from the stored list
- **THEN** reads place it last and persist the extended list

### Requirement: Swap-based up/down reordering

The system SHALL move a feature one position earlier on arrow-up activation and one position later on arrow-down activation by swapping adjacent positions in the stored list and persisting the result. Hidden features keep their positions and participate in swaps.

#### Scenario: Move up swaps with predecessor
- **WHEN** the player activates arrow-up on a feature with a predecessor in the stored list
- **THEN** the two ids swap positions and the change persists

#### Scenario: Move down swaps with successor
- **WHEN** the player activates arrow-down on a feature with a successor in the stored list
- **THEN** the two ids swap positions and the change persists

### Requirement: Boundary placeholder spacers

The system SHALL render a same-size placeholder spacer instead of the up arrow on the first row and instead of the down arrow on the last row of the stored order.

#### Scenario: First row has no up arrow
- **WHEN** the settings rows render and a feature is first in the stored list
- **THEN** its row shows a spacer where the up arrow would be

#### Scenario: Last row has no down arrow
- **WHEN** the settings rows render and a feature is last in the stored list
- **THEN** its row shows a spacer where the down arrow would be

### Requirement: HUD follows stored display order

The system SHALL render Quick Access HUD buttons in stored display-order sequence, filtering hidden features after sorting, with the settings button always last. Reorders SHALL shuffle existing buttons without rebuilding them.

#### Scenario: HUD reflects stored order
- **WHEN** the stored order lists feature B before feature A and both are visible
- **THEN** the HUD shows B's button before A's button

#### Scenario: Hidden features keep slots silently
- **WHEN** a hidden feature sits between two visible features in the stored list
- **THEN** the HUD shows the visible neighbors adjacently with no gap or placeholder
