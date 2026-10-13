## ADDED Requirements

### Requirement: Reactive keyed collections reconfigure after build
A reactive keyed collection SHALL apply a newly assigned item factory even when the factory is assigned after the collection's element has been built and attached to a parent. A keyed collection SHALL NOT require the caller to know whether assignment happened before or after build in order to have that factory take effect.

#### Scenario: Factory assigned after build renders items
- **WHEN** an item factory is assigned to a keyed collection that has already been built and has items
- **THEN** the items render with the newly assigned factory rather than remaining empty

#### Scenario: Factory assigned before build is unaffected
- **WHEN** an item factory is assigned to a keyed collection before it is built
- **THEN** the collection renders items as it did previously

#### Scenario: Assignment is consistent across keyed collections
- **WHEN** the same factory is assigned after build to each reactive keyed collection type
- **THEN** every one of them renders the items, with no collection type requiring a different call pattern

#### Scenario: Duplicate keys are still rejected predictably
- **WHEN** a factory assigned after build produces items with duplicate keys
- **THEN** the duplicate is dropped with a warning rather than throwing

### Requirement: Layout-bound cell constraints do not create reactive dependencies
Applying a reactive cell constraint to a cell SHALL NOT register a dependency on the mounting reactive observer. A reactive constraint SHALL install exactly one binding effect that re-applies the constraint when its source changes, and SHALL NOT additionally cause the enclosing mount operation's observer to depend on the constraint's source.

#### Scenario: Mounting effect gains no constraint dependency
- **WHEN** a reactive cell constraint is applied while a reactive observer is executing
- **THEN** that observer's dependency set does not include the constraint's source

#### Scenario: Constraint still reacts to its source
- **WHEN** the source of a reactive cell constraint changes after mounting
- **THEN** the constraint is re-applied to the cell exactly once

#### Scenario: Constraint applies at mount time
- **WHEN** a reactive cell constraint is applied during mounting
- **THEN** the cell reflects the constraint's current value immediately

### Requirement: Column count changes schedule a single reflow
Changing a reactive grid's column count SHALL schedule exactly one reflow per change, driven by the reactive chain, and SHALL NOT additionally perform an eager untracked reflow at the moment the column source is assigned.

#### Scenario: Column change reflows once
- **WHEN** the column count of a built reactive grid changes
- **THEN** the grid reconciles and lays out once for that change

#### Scenario: No eager reflow with stale data
- **WHEN** the column source is assigned
- **THEN** no reflow occurs synchronously at assignment time, so no intermediate frame is rendered from an untracked read

#### Scenario: Column change still propagates reactively
- **WHEN** the column count changes
- **THEN** item widths recompute and trailing spacer cells are adjusted to keep rows aligned

## MODIFIED Requirements

### Requirement: Keyed collections preserve component identity across data changes
A reactive keyed collection SHALL reconcile its items by a stable key so that an existing component is reused when its key persists across a data change, created when a key is new, and disposed only when its key is absent from the new data. The reconciliation SHALL be transactional: if an item factory throws partway through a pass, the components created during that failed pass SHALL be disposed and the previously active components SHALL be left untouched.

#### Scenario: Persisting key reuses the component
- **WHEN** the underlying data changes but retains a key that was previously rendered
- **THEN** the component for that key is reused rather than recreated

#### Scenario: New key creates a component
- **WHEN** the underlying data gains a key that was not previously rendered
- **THEN** a component is created for it

#### Scenario: Removed key disposes only that component
- **WHEN** the underlying data loses a previously rendered key
- **THEN** only that key's component is disposed and the others are retained

#### Scenario: Factory failure rolls back the pass
- **WHEN** an item factory throws partway through a reconciliation pass
- **THEN** the components created during that pass are disposed, previously active components are preserved, and the exception propagates