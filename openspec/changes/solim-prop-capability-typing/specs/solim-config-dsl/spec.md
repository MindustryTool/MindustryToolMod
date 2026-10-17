## ADDED Requirements

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

## MODIFIED Requirements

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
