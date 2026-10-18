## MODIFIED Requirements

### Requirement: Props targets any Arc type

A property SHALL declare, through its `Requires` type parameter, which host capability it needs, and a `Props` SHALL be able to carry properties for `ElementCapability` and `TableCapability` targets simultaneously. `ElementCapability` also carries the pending parent-cell configuration, so cell properties are expressed against `ElementCapability` rather than a separate `CellCapability`. Extending the set of target capabilities SHALL NOT require changing `Props` itself. A property SHALL be dispatched to the target it declares only when the host provides the corresponding capability, which `props(...)` enforces at compile time.

#### Scenario: Mixed capabilities in one Props

- **WHEN** a `Props` records `gap(2)`, which requires `TableCapability`, together with `width(4)` and `color(Color.red)`, which require `ElementCapability`
- **THEN** all three are retained in one list and applied to the target the host exposes for each, and the call compiles because the host provides both capabilities

#### Scenario: A property declares its capability

- **WHEN** a property is recorded
- **THEN** it identifies the capability (`ElementCapability` or `TableCapability`) its target requires, and `props(...)` accepts it only from a host that provides that capability

#### Scenario: An escape hatch accepts arbitrary mutations

- **WHEN** a component-specific modifier is not expressible as a setter
- **THEN** it is applied through a public escape hatch that accepts an arbitrary mutation against a chosen capability and target, so no second chained API is required

#### Scenario: Extending targets does not change Props

- **WHEN** a further capability and target are added
- **THEN** `Props` itself is unchanged, and only the new property factories and the hosts that declare the capability are affected

### Requirement: Host capabilities are declared by marker interfaces

Solim SHALL declare host capabilities as interfaces in `solim.modifier`, rooted at `HostCapability`:
- `ElementCapability` (the host exposes an `Element`, a pending parent-cell configuration, and coordinated alignment; implemented by every host). It SHALL declare `Element element()`, `PendingCellConfig cellConfig()`, and the default `alignCoordinated(...)` behaviour.
- `TableCapability` (the host exposes a `Table`; extends `ElementCapability`). It SHALL declare `Table table()`, non-null by contract.
- `ContainerCapability` (the host hosts children; extends `ElementCapability`). It SHALL declare `children(Runnable)`.
Semantic trait capabilities (e.g. `TextCapability`) SHALL extend `ElementCapability`.

`CellCapability` and `AlignmentCapability` SHALL NOT exist as separate interfaces; their contracts are folded into `ElementCapability`.

A host SHALL declare the capabilities it provides in its `implements` clause, and that declaration SHALL be the single source of truth for capability. A host's capability SHALL NOT be inferred from `table() != null`, because a host may host children without being table-backed. A host that does not implement `TableCapability` SHALL NOT expose `table()`.

#### Scenario: A non-table container declares its capabilities

- **WHEN** a component built on `arc.scene.ui.layout.Stack` (such as `SolimStack`) declares its capabilities
- **THEN** it declares `ElementCapability` and `ContainerCapability`, and does not declare `TableCapability`, even though it hosts children

#### Scenario: An element-only host declares no table capability

- **WHEN** a leaf such as `SolimImage`, `Text`, or `Divider` declares its capabilities
- **THEN** it declares `ElementCapability` and does not declare `TableCapability` or `ContainerCapability`

#### Scenario: A table-backed host exposes a non-null table

- **WHEN** a host implements `TableCapability` (such as `Column`, `Spacer`, or `SolimCollapser`)
- **THEN** it exposes `table()` returning a non-null `Table`, and inherits `element()`, `cellConfig()`, and `alignCoordinated(...)` from `ElementCapability`

#### Scenario: Collapsed interfaces are absent

- **WHEN** the codebase is inspected for `CellCapability` or `AlignmentCapability`
- **THEN** neither interface exists, because both contracts live on `ElementCapability`

#### Scenario: Accessors are not universal on non-capability hosts

- **WHEN** a host that does not implement `TableCapability` (such as `Text` or `SolimImage`) is inspected
- **THEN** it does not declare or expose `table()`

### Requirement: props rejects a property the host cannot honour

A property SHALL declare its required capability through its `Requires` type parameter, and `props(...)` SHALL be bounded so that a property requiring a capability the host does not provide is a compile error. Acceptance SHALL be exactly: `Prop<A, V>` is accepted by a host `H` if and only if `H` is a subtype of `A`. A property whose target the host does not expose SHALL NOT be silently skipped at application time.

#### Scenario: A table property is rejected by an element-only host

- **WHEN** `gap(4f)` or `padding(4f)`, which require `TableCapability`, is passed to `props(...)` on an `ElementCapability`-only host such as `SolimImage`
- **THEN** the code does not compile

#### Scenario: A table property is accepted by a table host

- **WHEN** `gap(4f)` is passed to `props(...)` on a `TableCapability` host such as `Column`
- **THEN** it compiles and the recorded gap is applied to the host's table

#### Scenario: A cell property requires element capability

- **WHEN** `growX()`, `minWidth(...)`, or `margin(...)`, which require `ElementCapability` (which carries the pending cell configuration), is passed to `props(...)` on a host
- **THEN** it compiles, because every host provides `ElementCapability`

#### Scenario: The escape hatch states its capability

- **WHEN** a component-specific mutation is expressed through the raw escape hatch
- **THEN** the call names the capability the host must provide, so the escape hatch documents the host it targets