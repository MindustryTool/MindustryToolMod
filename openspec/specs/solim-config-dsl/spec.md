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

A property SHALL declare, through its `Requires` type parameter, which host capability it needs, and a `Props` SHALL be able to carry properties for `ElementCapability`, `TableCapability`, and `CellCapability` targets simultaneously. Extending the set of target capabilities SHALL NOT require changing `Props` itself. A property SHALL be dispatched to the target it declares only when the host provides the corresponding capability, which `props(...)` enforces at compile time.

#### Scenario: Mixed capabilities in one Props

- **WHEN** a `Props` records `gap(2)`, which requires `TableCapability`, together with `width(4)` and `color(Color.red)`, which require `ElementCapability`
- **THEN** all three are retained in one list and applied to the target the host exposes for each, and the call compiles because the host provides both capabilities

#### Scenario: A property declares its capability

- **WHEN** a property is recorded
- **THEN** it identifies the capability (`ElementCapability`, `TableCapability`, or `CellCapability`) its target requires, and `props(...)` accepts it only from a host that provides that capability

#### Scenario: An escape hatch accepts arbitrary mutations

- **WHEN** a component-specific modifier is not expressible as a setter
- **THEN** it is applied through a public escape hatch that accepts an arbitrary mutation against a chosen capability and target, so no second chained API is required

#### Scenario: Extending targets does not change Props

- **WHEN** a further capability and target are added
- **THEN** `Props` itself is unchanged, and only the new property factories and the hosts that declare the capability are affected

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

### Requirement: Every host exposes one children signature
 
A host that provides `ContainerCapability` SHALL expose `children(Runnable)` returning the host type, declared on the container capability rather than on `Configurable`, so a host that does not host children SHALL NOT expose `children`. A reconciler's item factory SHALL NOT be named `children`, because a zero-argument lambda would then be ambiguous between the two.
 
#### Scenario: Item factories are distinguished by name
 
 - **WHEN** a collection host such as `ForEach`, `ReactiveGrid`, `VirtualList`, or `Popup` accepts a per-item factory
 - **THEN** that factory is named `items(...)` rather than `children(...)`, and returns the host type for fluency
 
#### Scenario: A no-argument children lambda is unambiguous
 
 - **WHEN** a container host is given `children(() -> ...)`
 - **THEN** it resolves to the container block, with no competing `children(Func<T, Component>)` overload
 
#### Scenario: A leaf does not host children
 
 - **WHEN** `children(() -> ...)` is called on a host that does not provide `ContainerCapability`, such as `SolimImage`, `Text`, `Divider`, or `Spacer`
 - **THEN** the code does not compile

### Requirement: The mixin interfaces are removed

`CellConfig`, `ElementConfig`, and `TableConfig` SHALL NOT remain in the codebase once every call site has been migrated. Their removal SHALL be enforced by compilation rather than by manual audit.

#### Scenario: A straggler call site fails the build
- **WHEN** a migrated codebase still contains a call to a removed mixin method
- **THEN** compilation fails, leaving no silent mixed-style code paths

#### Scenario: No mod type hierarchy depends on the mixins
- **WHEN** the migration completes
- **THEN** no mod class implements the mixin interfaces, because the migration is a call-site rewrite only

### Requirement: Prop declares its required capability and value type

`Prop` SHALL be generic over two type parameters, `Prop<Requires, V>`: `Requires` is the host capability a property needs and `V` is the property's value type. A property factory SHALL produce a property whose value type matches the setter it applies, so an incompatible value is a compile error rather than a `ClassCastException` at application time. The value SHALL NOT be stored or passed as `Object`, and applying a property SHALL NOT narrow the value with a cast. Value-less properties SHALL be represented by a dedicated value-less form rather than a boolean flag riding alongside a `null` value, and a value-less property SHALL still apply even though it carries no payload.

#### Scenario: Value type is enforced at the factory

- **WHEN** a property that applies a `Float` is declared with a value that is not a `Float` or `Readable<Float>`
- **THEN** the code does not compile, because the factory's value type parameter rejects it

#### Scenario: Applying a property does not cast the value

- **WHEN** any `solim-core` property factory applies its value
- **THEN** the value flows through a typed setter with no `(Readable<Float>)`, `(Readable<Boolean>)`, `(Readable<Color>)`, `num(...)`, or `(Integer)` narrowing at the prop boundary

#### Scenario: Value-less property still applies

- **WHEN** a value-less property such as `growX()` or `top()` is passed to `props(...)`
- **THEN** it applies its mutation even though it carries no value, and it is not skipped as if its value were `null`

### Requirement: Property target dispatch is cast-free and label-independent

A property SHALL resolve its Arc target (`Element`, `Table`, or the pending `Cell` configuration) through a typed host view selected by the property's required capability, so the target is not cast from `Object`. A property's diagnostic `label` SHALL NOT determine behaviour: alignment and every other property SHALL carry its own value-typed applier instead of being recognised by comparing label strings.

#### Scenario: Alignment is not dispatched by label

- **WHEN** `top()`, `bottom()`, `left()`, `right()`, `center()`, or `align(...)` is applied
- **THEN** the alignment is recorded by the property's own typed applier, and no code compares `prop.label()` to decide the behaviour

#### Scenario: No target cast

- **WHEN** a property is applied to a table-backed host, an element-only host, or a host with a pending cell configuration
- **THEN** the target is obtained from the typed host view for the property's capability, without casting an `Object` target

### Requirement: Host capabilities are declared by marker interfaces

Solim SHALL declare host capabilities as marker interfaces in `solim.modifier`, rooted at `HostCapability` and implemented by every host: `ElementCapability` (the host exposes an `Element`), `TableCapability` (the host exposes a `Table`, and extends `ElementCapability`), `CellCapability` (the host has a pending parent-cell configuration), and `ContainerCapability` (the host hosts children). A host SHALL declare the capabilities it provides in its `implements` clause, and that declaration SHALL be the single source of truth for capability. A host's capability SHALL NOT be inferred from `table() != null`, because a host may host children without being table-backed.

#### Scenario: A non-table container declares its capabilities

- **WHEN** a component built on `arc.scene.ui.layout.Stack` (such as `SolimStack`) declares its capabilities
- **THEN** it declares `ElementCapability` and `ContainerCapability`, and does not declare `TableCapability`, even though it hosts children

#### Scenario: An element-only host declares no table capability

- **WHEN** a leaf such as `SolimImage`, `Text`, or `Divider` declares its capabilities
- **THEN** it declares `ElementCapability` and does not declare `TableCapability` or `ContainerCapability`

### Requirement: props rejects a property the host cannot honour

A property SHALL declare its required capability through its `Requires` type parameter, and `props(...)` SHALL be bounded so that a property requiring a capability the host does not provide is a compile error. Acceptance SHALL be exactly: `Prop<A, V>` is accepted by a host `H` if and only if `H` is a subtype of `A`. A property whose target the host does not expose SHALL NOT be silently skipped at application time.

#### Scenario: A table property is rejected by an element-only host

- **WHEN** `gap(4f)` or `padding(4f)`, which require `TableCapability`, is passed to `props(...)` on an `ElementCapability`-only host such as `SolimImage`
- **THEN** the code does not compile

#### Scenario: A table property is accepted by a table host

- **WHEN** `gap(4f)` is passed to `props(...)` on a `TableCapability` host such as `Column`
- **THEN** it compiles and the recorded gap is applied to the host's table

#### Scenario: A cell property requires a parent cell

- **WHEN** `growX()`, `minWidth(...)`, or `margin(...)`, which require `CellCapability`, is passed to `props(...)` on a host that does not provide a pending cell configuration
- **THEN** the code does not compile

#### Scenario: The escape hatch states its capability

- **WHEN** a component-specific mutation is expressed through the raw escape hatch
- **THEN** the call names the capability the host must provide, so the escape hatch documents the host it targets

### Requirement: Semantic capabilities restrict a property to the hosts that honour it

Beyond the target capability a property writes, a property MAY require a semantic capability describing that the host honours it. Semantic capabilities SHALL be trait marker interfaces in `solim.modifier` extending `ElementCapability`, implemented by every host that honours the corresponding property family. A property whose validity relative to a host is a host property SHALL require a trait, so applying it to a host that does not implement the trait is a compile error. Where a property is honoured by more than one widget family, the property SHALL require the trait those families share, so that "valid on A or B" is expressed as a shared trait rather than a union.

#### Scenario: A trait property is rejected by a host without the trait

- **WHEN** a property requiring a trait, such as `checked(...)` requiring `CheckableCapability`, is passed to `props(...)` on a host that does not implement the trait
- **THEN** the code does not compile

#### Scenario: A shared trait covers several widget families

- **WHEN** a property that is honoured by `Button`, `Checkbox`, and `Switch` is declared
- **THEN** it requires the trait all three implement, and compiles on any of them without a per-widget overload or a union type

#### Scenario: Traits compose with target capabilities

- **WHEN** a host both hosts children and accepts a trait property
- **THEN** it declares the container capability and the trait together, and one `props(...)` call carries both its layout and trait properties

### Requirement: A capability-proven property never silently no-ops

When a property reaches application, it SHALL produce its documented effect or throw a descriptive error. A property SHALL NOT silently do nothing because the host cannot honour it. A silent no-op SHALL occur only when the property's value is genuinely absent; a mismatch between the value and the host SHALL throw. A thrown error intended to be read by a user SHALL use a translation key rather than a hardcoded message.

#### Scenario: A mismatch throws

- **WHEN** a property whose validity depends on the runtime class of its value is applied to a host that does not accept that value class
- **THEN** application throws a descriptive error naming the host and the value class, rather than silently doing nothing

#### Scenario: An absent value is a no-op

- **WHEN** a property is given a `null` value where absence is meaningful
- **THEN** it performs no mutation and does not throw

### Requirement: Configuration is declared only through props

Configuration of a Solim host SHALL be declared exclusively through `.props(...)`. A chained configuration method SHALL NOT exist for any value the host renders, lays out, or exposes as state; the only chained methods that remain SHALL be content (appends a child), behaviour (registers a callback or performs an imperative action), structure (`children`/`items`/`tab`/`show`/`hide`), and accessors (getters). Properties SHALL be applied after any fluent content declared before them, and when two properties in the same `props(...)` call write the same target the later one SHALL win.

#### Scenario: A configuration setter is absent

- **WHEN** the public API of a host is inspected for a configuration setter such as `Text.wrap(boolean)`, `Button.checked(Readable<Boolean>)`, `SolimVideo.autoplay(boolean)`, `SolimCollapser.collapsed(boolean)`, or `Grid.columns(int)`
- **THEN** the setter does not exist, and the equivalent property factory is declared instead

#### Scenario: Content and behaviour stay fluent

- **WHEN** a button is declared as `button().text("Save").icon(Icon.add).onClick(this::save)`
- **THEN** all three calls compile, because content and behaviour are not configuration

#### Scenario: Later property wins

- **WHEN** a single `props(...)` call contains two properties writing the same target
- **THEN** the later property is the effective value

#### Scenario: Props apply after fluent content

- **WHEN** fluent content is declared and configuration is declared in `props(...)`
- **THEN** the configuration applies to the host after the content exists, so the configured value is the effective one

