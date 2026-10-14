## ADDED Requirements

### Requirement: Deferred Props builder

A `Props<T>` SHALL record properties as data without touching an Arc type, allocating an `Effect`, or registering ownership. `Props<T>` SHALL be generic over its host component so that a single recorded list MAY carry properties targeting `Element`, `Table`, and `Cell` simultaneously.

#### Scenario: Building Props has no side effects
- **WHEN** a call site writes `.props(gap(2), width(4), color(Color.red))`
- **THEN** no Arc type is resolved, no `Effect` is created, and no ownership registration occurs; only an ordered list of properties is produced

#### Scenario: Mixed Arc targets in one Props
- **WHEN** a `Props` records `gap(2)`, which targets `Table`, together with `width(4)` and `color(Color.red)`, which target `Element`
- **THEN** all three are retained in one list and applied to whichever target the host exposes for each

#### Scenario: Props is safe to build outside an ownership window
- **WHEN** a `Props` is constructed at a call site that is not inside `build()` or a `children()` block
- **THEN** construction succeeds, because recording a property creates no reactive primitive

### Requirement: Properties apply inside the children window

A `Props` SHALL apply its recorded properties at the start of the host's `children()` block, before the block runs and before the children are attached. The host SHALL be the active ambient owner for the duration of the application.

#### Scenario: Reactive properties install inside the window
- **WHEN** a `Props` holds a property backed by a `Readable` and the host's `children()` block runs
- **THEN** the binding is installed as an `Effect` while the host is the active ambient owner, with no explicit ownership registration at the call site

#### Scenario: Static properties apply without any reactive primitive
- **WHEN** a `Props` holds only static-valued properties
- **THEN** they are applied directly to the Arc target and no `Effect` is allocated

#### Scenario: Eagerly constructed hosts are supported
- **WHEN** a host such as `Column`, `Row`, `Card`, `Grid`, `Wrap`, or `Spacer` carries a `Props`
- **THEN** the properties are applied through its `children()` block even though the host never executes `build()`

#### Scenario: Application order is preserved
- **WHEN** a `Props` records two properties that write the same target property
- **THEN** the later recorded property wins, matching the last-write-wins behaviour of a chained call

### Requirement: Props targets any Arc type

A property SHALL declare which Arc type it applies to, and a `Props` SHALL be able to carry properties for `Element`, `Table`, `Cell`, and any further Arc type added later. Extending the set of target types SHALL NOT require changing `Props` itself.

#### Scenario: A property declares its target
- **WHEN** a property is recorded
- **THEN** it identifies its target as `Element`, `Table`, or `Cell`, and is dispatched to that target when applied

#### Scenario: An escape hatch accepts arbitrary mutations
- **WHEN** a component-specific modifier is not expressible as a setter
- **THEN** it is applied through a public escape hatch that accepts an arbitrary mutation against a chosen target, so no second chained API is required

### Requirement: Cell properties are recorded before attachment

A property that targets the parent `Cell` SHALL be recorded into the host's pending cell configuration during application, not applied to a live cell, so that the existing cell configurator applies it when the element is attached.

#### Scenario: Cell constraints survive the deferral
- **WHEN** a `Props` records `growX()`, `minWidth(...)`, or `margin(...)` and the host is realized and attached
- **THEN** the pending cell configuration holds those constraints before the cell configurator runs, and the resulting cell reflects them

#### Scenario: The configurator runs inside a window
- **WHEN** a host carrying a `Props` is realized by the pending attachment mechanism
- **THEN** the cell configurator executes while the container is still the active ambient owner, because `OwnershipContext.pop()` runs after `AttachmentStack.pop()`

### Requirement: Reconciled items use the same cell configuration path

Items created by a reconciler SHALL be attached through the same attachment pipeline as any other child, so that cell configuration is applied by the shared cell configurator rather than repaired at each call site.

#### Scenario: Reconciled items do not hand-attach
- **WHEN** `ForEach`, `Dynamic`, `When`, `ReactiveGrid`, `QueryView`, or `Popup` mounts or reconciles an item
- **THEN** the item is attached through the attachment pipeline and no cell configurator call or manual adoption repair is performed at the reconciler

#### Scenario: A reactive cell constraint has exactly one owner
- **WHEN** a reconciled item carries a reactive cell constraint
- **THEN** the binding is adopted by exactly one component and disposed exactly once, rather than being registered in both the reconciler's binding list and the item

### Requirement: Chained and Props modifiers have a defined precedence

While both styles exist during migration, a modifier chained after `.props(...)` SHALL override the configured value, and a modifier chained before it SHALL be overridden by the configured value.

#### Scenario: Chained modifier after Props wins
- **WHEN** a call site writes `.props(gap(2)).gap(4f)`
- **THEN** the effective gap is 4f

#### Scenario: Chained modifier before Props loses
- **WHEN** a call site writes `.gap(4f).props(gap(2))`
- **THEN** the effective gap is 2f

### Requirement: The mixin interfaces are removed

`CellConfig`, `ElementConfig`, and `TableConfig` SHALL NOT remain in the codebase once every call site has been migrated. Their removal SHALL be enforced by compilation rather than by manual audit.

#### Scenario: A straggler call site fails the build
- **WHEN** a migrated codebase still contains a call to a removed mixin method
- **THEN** compilation fails, leaving no silent mixed-style code paths

#### Scenario: No mod type hierarchy depends on the mixins
- **WHEN** the migration completes
- **THEN** no mod class implements the mixin interfaces, because the migration is a call-site rewrite only
