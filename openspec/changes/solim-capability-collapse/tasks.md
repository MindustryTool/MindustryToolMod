## 1. Capability interfaces

- [x] 1.1 Add `Element element()`, `PendingCellConfig cellConfig()`, and default `alignCoordinated(...)` (with `coordinateTable`/`coordinateCell` helpers) to `ElementCapability`.
- [x] 1.2 Add `Table table()` (non-null) to `TableCapability`.
- [x] 1.3 Make `ContainerCapability` extend `ElementCapability` instead of `HostCapability`.
- [x] 1.4 Delete `CellCapability` and `AlignmentCapability` interfaces.

## 2. Configurable cleanup

- [x] 2.1 Remove `element()`, `table()`, `cellConfig()`, and alignment-related accessors from `Configurable`.
- [x] 2.2 Keep only `self()`, `isBuilt()`, `pendingProps()`, `setPendingProps()`, and `props(...)` on `Configurable`.

## 3. Host reclassification

- [x] 3.1 Add `TableCapability` to `Spacer` and `SolimCollapser` `implements` clauses.
- [x] 3.2 Remove `table()` (and `cellConfig()` where duplicated) from non-table hosts: `Text`, `SolimImage`, `Divider`, `Switch`, `SolimTextField`, `SolimSlider`, `SolimSelect`, `SolimVideo`, `SolimStack`, `SolimElement`.
- [x] 3.3 Update all host `implements` clauses to drop `CellCapability` and `AlignmentCapability` (inherited via `ElementCapability`), adding `ElementCapability` where a host currently lacks it.
- [x] 3.4 Ensure every host still declares `ElementCapability` (directly or via `TableCapability`/`ContainerCapability`/a semantic trait).

## 4. Internal call sites

- [x] 4.1 Rewrite `AlignmentCapability.alignCoordinated` logic (now on `ElementCapability`) to gate table access on `this instanceof TableCapability`.
- [x] 4.2 Update `Props.hostView().withTable` to use `host instanceof TableCapability ? ((TableCapability) host).table() : null`.
- [x] 4.3 Confirm `Tabs` and `QueryView` (table hosts) compile unchanged.
- [x] 4.4 Update mod caller `QuickAccessPopupHelper` if needed.

## 5. Tests and guards

- [x] 5.1 Update `HostCapabilityTest`: remove `CellCapability`/`AlignmentCapability` assertions, move `Spacer` to table hosts, keep leaves as `ElementCapability`-only.
- [x] 5.2 Update any tests calling `.table()` on now-non-table hosts to use `element()` or a host-specific accessor.
- [x] 5.3 Run `solim-core`, `solim`, and `mod` compilation; fix stragglers.
- [x] 5.4 Run the capability/ownership test suites.