# solim-config-dsl Specification

## Purpose
TBD - created by archiving change refactor-component-lifecycle-modifiers. Update Purpose after archive.
## Requirements
### Requirement: Props applies inside a component-scoped window

A `Props<T>` SHALL be applied either immediately inside a window that the host opens for itself, or during realization. It SHALL be generic over its host component so that a single recorded list MAY carry properties targeting `Element`, `Table`, and `Cell` simultaneously. Constructing a `Props` SHALL NOT create an `Effect` or register ownership; only applying it does.

#### Scenario: Mixed Arc targets in one Props
- **WHEN** a `Props` records `gap(2)`, which targets `Table`, together with `width(4)` and `color(Color.red)`, which target `Element`
- **THEN** all three are retained in one list and applied to whichever target the host exposes for each

#### Scenario: Constructing Props creates no reactive primitive
- **WHEN** a `Props` is constructed
- **THEN** no `Effect` is created and no ownership registration occurs; only an ordered list of properties is produced

### Requirement: An already realized host applies props in its own window

When a host is already built, `props(...)` SHALL open a window with the host as the active ambient owner before applying, and SHALL close it afterwards.

#### Scenario: Reactive properties bind to the host, not the enclosing scope
- **WHEN** `props(...)` is called on a realized host from inside an enclosing `children()` block
- **THEN** the host is the active ambient owner for the application, so any `Effect` the properties create is owned by the host rather than the enclosing component

#### Scenario: Eagerly constructed hosts need no construction change
- **WHEN** a host such as `Column`, `Row`, `Card`, `Text`, or `Divider` is eagerly constructed and receives `props(...)`
- **THEN** the properties apply immediately, with no requirement that the host be converted to lazy `build()`

#### Scenario: Binding is disposed with the host
- **WHEN** a reactive property installs a binding on a realized host
- **THEN** disposing the host disposes the binding, and subsequent signal writes no longer reach the Arc target

#### Scenario: Properties are applied at most once
- **WHEN** a `Props` has been applied through either path
- **THEN** a later `applyPendingProps()` on the same host does not re-apply it

### Requirement: A lazy host applies props during realization

When a host is not yet built, `props(...)` SHALL record the properties and `Component.onRealized(Element)` SHALL drain them inside the window that realization already opens.

#### Scenario: Deferred application still lands in a window
- **WHEN** a lazy host receives `props(...)` and is later realized
- **THEN** the properties are applied during realization, while the host is the active ambient owner

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

### Requirement: Every host exposes one children signature

Every `Configurable` host SHALL expose `children(Runnable)` returning the host type. A reconciler's item factory SHALL NOT be named `children`, because a zero-argument lambda would then be ambiguous between the two.

#### Scenario: Item factories are distinguished by name
- **WHEN** a collection host such as `ForEach`, `ReactiveGrid`, `VirtualList`, or `Popup` accepts a per-item factory
- **THEN** that factory is named `items(...)` rather than `children(...)`, and returns the host type for fluency

#### Scenario: A no-argument children lambda is unambiguous
- **WHEN** any host is given `children(() -> ...)`
- **THEN** it resolves to the container block, with no competing `children(Func<T, Component>)` overload

### Requirement: The mixin interfaces are removed

`CellConfig`, `ElementConfig`, and `TableConfig` SHALL NOT remain in the codebase once every call site has been migrated. Their removal SHALL be enforced by compilation rather than by manual audit.

#### Scenario: A straggler call site fails the build
- **WHEN** a migrated codebase still contains a call to a removed mixin method
- **THEN** compilation fails, leaving no silent mixed-style code paths

#### Scenario: No mod type hierarchy depends on the mixins
- **WHEN** the migration completes
- **THEN** no mod class implements the mixin interfaces, because the migration is a call-site rewrite only

