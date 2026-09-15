# feature-order Specification

## Purpose

User-reorderable feature cards with persisted order across sessions, load-time normalization, boundary-aware chevron controls, and deterministic pinning of development features. Created by archiving change reorder-features.

## Requirements

**Source: reorder-features**

### Requirement: Ordered-ID list persistence

The system SHALL persist the user-defined non-development feature order as an ordered list of feature ids in a ConfigValue<Seq<String>> JSON entry owned by ModSettings, using a custom ordered JSON persister that preserves list order across save/load cycles.

#### Scenario: Order persists across sessions

- **WHEN** the user reorders features and the game restarts
- **THEN** the settings grid renders features in the persisted list order

#### Scenario: Ordered persister round-trips positions

- **WHEN** an ordered list of ids is saved and reloaded
- **THEN** every id occupies the same position as before saving

### Requirement: Load-time normalization

The system SHALL normalize the persisted ordered list during FeatureManager.init() after all registrations by deduplicating keeping the first occurrence, dropping stale (unregistered) ids, dropping development ids, appending missing non-development ids sorted by metadata order then id, and persisting the reconciled list only when it differs from storage. An unparseable or corrupt payload SHALL reset to the default order of registered non-development ids sorted by metadata order then id and overwrite the stored key.

#### Scenario: Duplicate ids healed

- **WHEN** the stored list contains the same feature id twice
- **THEN** the normalized list keeps the first occurrence, drops the later one, and persists the healed list

#### Scenario: Stale and development ids removed

- **WHEN** the stored list contains an unregistered id or a development feature id
- **THEN** the normalized list excludes both and persists the cleaned list

#### Scenario: New feature appended deterministically

- **WHEN** a registered non-development feature id is absent from the stored list
- **THEN** it is appended in metadata-order-then-id sequence relative to other missing ids

#### Scenario: Corrupt payload resets to default

- **WHEN** the stored order value cannot be parsed as a list of strings
- **THEN** the system falls back to all registered non-development ids sorted by metadata order then id and overwrites the stored key

#### Scenario: Clean list causes no write

- **WHEN** the stored list already reconciles exactly
- **THEN** no persist write occurs during init

### Requirement: Swap-based chevron reordering

The system SHALL move a non-development feature one position earlier on chevron-left activation and one position later on chevron-right activation by swapping adjacent positions in the global ordered list and persisting the result. The first item's move-left and the last non-development item's move-right SHALL be disabled. Both chevrons SHALL be disabled on every card while the search filter query is non-empty.

#### Scenario: Move left swaps with predecessor

- **WHEN** the user activates chevron-left on a non-development feature that has a predecessor in the global ordered list
- **THEN** the two features exchange positions, the change persists, and the grid re-renders in the new order

#### Scenario: Move right swaps with successor

- **WHEN** the user activates chevron-right on a non-development feature that has a non-development successor in the global ordered list
- **THEN** the two features exchange positions, the change persists, and the grid re-renders in the new order

#### Scenario: Boundary chevrons disabled

- **WHEN** a card represents the first item or the last non-development item of the global ordered list
- **THEN** the corresponding outward chevron is disabled

#### Scenario: Filtering disables reordering

- **WHEN** the search filter query is non-empty
- **THEN** all reorder chevrons are disabled until the query is cleared

### Requirement: Development pinning

The system SHALL always render development features after all non-development features regardless of persisted order, sorted deterministically by feature id, and SHALL NOT persist development ids or render reorder chevrons on development cards.

#### Scenario: Development features stay last

- **WHEN** the settings grid renders with any persisted order
- **THEN** every development feature appears after every non-development feature in feature-id order

#### Scenario: Development cards show no chevrons

- **WHEN** a card represents a development feature
- **THEN** no reorder chevrons are rendered on that card

### Requirement: Status row layout with trailing chevrons

Each non-development FeatureCard SHALL render its status text left-aligned in a row followed by a spacer pushing chevron-left and chevron-right buttons to the most-right position, keeping the divider accent below the row. Chevron activation SHALL NOT propagate to the card enable toggle, following the same click-consumption behavior as the existing header shortcut buttons.

#### Scenario: Status row arrangement

- **WHEN** a non-development feature card renders
- **THEN** the status text is left-aligned with the reorder chevrons at the most-right end of the same row and the divider accent below

#### Scenario: Chevron click does not toggle

- **WHEN** the user activates a reorder chevron on a card
- **THEN** the corresponding dialog-free reorder occurs and the feature's enabled state is unchanged

### Requirement: Localized reorder controls

The system SHALL resolve reorder chevron labels or tooltips from Core.bundle under documented keys carrying translator comments, with no hardcoded user-visible text.

#### Scenario: Reorder tooltips resolve

- **WHEN** a reorder chevron renders or is hovered
- **THEN** its accessible text resolves from the bundle