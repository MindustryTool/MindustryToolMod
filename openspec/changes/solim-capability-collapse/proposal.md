## Why

`Configurable` forces every Solim host to implement `table()`, but only table-backed hosts actually have a table; the rest return `null`. That makes the type system lie ("has a table") and forces callers to defend against null. At the same time, `ElementCapability`, `CellCapability`, and `AlignmentCapability` are implemented by every host, so they distinguish nothing — they are three redundant markers on top of `HostCapability`. The capability hierarchy is out of sync with both the accessors and the "single source of truth" rule in `solim-config-dsl`.

## What Changes

- **BREAKING**: Move `table()` off `Configurable` and onto `TableCapability`, so only table-backed hosts expose it (non-null by contract).
- **BREAKING**: Move `element()`, `cellConfig()`, and `alignCoordinated(...)` off `Configurable` and onto `ElementCapability`, making it the universal accessor bundle for every host.
- **BREAKING**: Collapse `CellCapability` and `AlignmentCapability` into `ElementCapability` (fold their accessors/defaults into it); delete the two redundant interfaces.
- Make `TableCapability` and `ContainerCapability` extend `ElementCapability` instead of `HostCapability`, inheriting the universal accessors.
- Reclassify hosts that already own a real table but are not declared as table hosts: `Spacer` and `SolimCollapser` gain `TableCapability`.
- Remove `table()` (and the now-relocated accessors) from non-table hosts: `Text`, `SolimImage`, `Divider`, `Switch`, `SolimTextField`, `SolimSlider`, `SolimSelect`, `SolimVideo`, `SolimStack`, and the `SolimElement` base.
- Update internal call sites (`AlignmentCapability`, `Props`, `Tabs`, `QueryView`) to gate table access on `TableCapability`.
- Update `HostCapabilityTest` and other capability assertions to the collapsed hierarchy.

## Capabilities

### New Capabilities

- (none — this reuses and tightens the existing host-capability contract)

### Modified Capabilities

- `solim-config-dsl`: host capabilities become the single source of truth for both "what a host has" and "how to reach it"; accessors relocate from `Configurable` to their capability; redundant universal markers collapse into `ElementCapability`.

## Impact

- Affected code: `solim-core` host classes and capability interfaces; `AlignmentCapability`, `Props`; `HostCapabilityTest`; mod callers of `.table()` on non-table hosts.
- Affected APIs: `Configurable` loses `element()`, `table()`, `cellConfig()`, `alignCoordinated`-adjacent access; `TableCapability`/`ElementCapability` gain them. Mod code calling `.table()` on leaves becomes a compile error (intended).
- Dependencies/systems: none external; this is an internal Solim refactor building on the existing capability migration.