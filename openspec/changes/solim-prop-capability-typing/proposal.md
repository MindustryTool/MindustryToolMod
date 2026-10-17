## Why

Phase 1 gave `Prop` a value type and a `Requires` type parameter, but declined to use it: every factory requires the root marker `HostCapability`, which every host implements. Nothing is rejected, so the original defect survives:

- `solim.UI.image(...).props(gap(4f))` compiles. `gap` targets a `Table`, `SolimImage.table()` returns `null`, and `Props.targetFor` returns `null`, so `Prop.writeTo` returns early — a silent no-op.
- Capability is currently a runtime guess (`table() != null`), which is wrong for `SolimStack`: it is a child-hosting container built on `arc.scene.ui.layout.Stack`, not a `Table`, so any table-kind property is a no-op there too.
- `children(Runnable)` is declared on `Configurable`, so a leaf (`SolimImage`, `Text`, `Divider`, `Spacer`, `SolimTextField`, `SolimSlider`, `Switch`, `NetworkImage`, `SolimVideo`) accepts a child block that attaches nothing.
- Alignment is dispatched by an `instanceof` ladder in `Values.alignCoordinated`, which is the largest single cast cluster in the modifier package.

## What Changes

- **Capability markers become real**: `solim.modifier.HostCapability` (root) ← `ElementCapability` ← `TableCapability`, plus `CellCapability` and `ContainerCapability`. A host declares the capabilities it provides by implementing them; the markers are interfaces, not Arc types, so a container that is not a `Table` can still declare `ContainerCapability` without declaring `TableCapability`.
- **`props(...)` rejects what a host cannot honour**: a property whose `Requires` is not a supertype of the host is a compile error. Acceptance is exactly `Prop<A, V>` is accepted by host `H` iff `H` is a subtype of `A`.
- **`children(Runnable)` moves onto `ContainerCapability`**: leaves lose it, containers keep it. `Props.children` is deleted.
- **Alignment dispatch becomes polymorphic**: the `instanceof` ladder is replaced by an alignment capability each container implements, following the existing `GapContainer.find(...)` pattern.
- **Runtime `table() != null` stops being the capability test**: hosts state their capabilities by declaration, and `Props.targetFor`'s `null`-target no-op path becomes unreachable for correctly typed calls.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-config-dsl`: the "Props targets any Arc type" requirement is replaced by capability-based declaration and compile-time acceptance; the "Every host exposes one children signature" requirement moves `children(Runnable)` onto `ContainerCapability` so non-container hosts no longer expose it.
- `solim-layout`: "Unified alignment direction and bitmask composition" is amended so alignment dispatch is a capability method rather than an `instanceof` ladder over concrete container types.

## Impact

- `solim-core`: new marker types in `solim.modifier`; every host's `implements` clause; `Configurable` (loses `children`); `Props` (loses `children`, typed target lookup); `Values` (`alignCoordinated` and the layout/table/cell factories gain real `Requires`); `GapContainer` and the containers (alignment capability).
- `solim-api`: `Component` gains the root/container capability implementations so mod-side subclasses inherit them.
- `mod`: call sites that apply a table-only property to an element-only host, or `children(...)` to a leaf, become compile errors — these are the defects this phase exists to surface.
- Tests: `PropsDslTest` (rejection cases), a new capability test, and tests that currently call `children()` on a leaf.

## Non-Goals

- Semantic per-widget markers (`icon` only on `Button`/`Image`, `style` value types) — next phase.
- Removing chained configuration methods — last phase.
