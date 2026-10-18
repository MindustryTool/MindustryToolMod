## Context

Solim's host-capability migration introduced `ElementCapability`, `TableCapability`, `CellCapability`, `ContainerCapability`, and `AlignmentCapability` as marker interfaces, but the actual accessors (`element()`, `table()`, `cellConfig()`, `alignCoordinated(...)`) still live on `Configurable`. As a result:

- `table()` is forced on every host, returning `null` for non-table hosts (`Text`, `SolimImage`, `Divider`, `Switch`, `SolimTextField`, `SolimSlider`, `SolimSelect`, `SolimVideo`, `SolimStack`, `SolimElement`).
- `CellCapability`, `AlignmentCapability`, and `ElementCapability` are implemented by every host, so they distinguish nothing.
- `Spacer` and `SolimCollapser` own a real `Table` but are not declared as `TableCapability` hosts, so the capability list is already dishonest.

The `solim-config-dsl` spec mandates that capabilities are the single source of truth and that capability must not be inferred from `table() != null`.

## Goals / Non-Goals

**Goals:**
- Make capabilities the single source of truth for both "what a host has" and "how to reach it".
- Remove `table()` from non-table hosts so misuse is a compile error.
- Collapse redundant universal markers into `ElementCapability`.
- Reclassify `Spacer` and `SolimCollapser` as table hosts.

**Non-Goals:**
- No change to the props DSL shape or to `Prop.Requires` semantics beyond capability class names.
- No removal of semantic trait capabilities (`TextCapability`, `ImageCapability`, …).
- No change to `ContainerCapability`'s subset status.
- No runtime behavior change for existing table hosts.

## Decisions

1. **Accessors move to capabilities, not the other way around.** `ElementCapability` gains `element()`, `cellConfig()`, and the default `alignCoordinated(...)`. `TableCapability` gains `table()` (non-null). `Configurable` keeps only the props-DSL contract (`self`, `isBuilt`, `pendingProps`, `props`). Rationale: capabilities already encode the subset distinctions; accessors belong with them. Alternative considered — keep accessors on `Configurable` and add `hasTable()` — rejected because it preserves the null lie and the redundant markers.

2. **Collapse `CellCapability` and `AlignmentCapability` into `ElementCapability`.** Both are universal (every host implements them) and their contracts (`cellConfig()`, `alignCoordinated`) are accessors/defaults, not subset distinctions. Folding them removes two interfaces with no loss of information. Alternative considered — keep them as type aliases extending `ElementCapability` — rejected because aliases still require every host to declare them and do not reduce the marker noise.

3. **`TableCapability` and `ContainerCapability` extend `ElementCapability`.** This gives subset hosts the universal accessors by inheritance and keeps the hierarchy a single chain. `HostCapability` stays the empty root so "is a host" checks are unchanged.

4. **`table()` is non-null on `TableCapability`.** Since only hosts that genuinely own a table implement it, a null return is no longer representable. Internal code that previously null-checked (`AlignmentCapability`, `Props.withTable`) now gates on `instanceof TableCapability` instead.

5. **Reclassify, don't special-case.** `Spacer` and `SolimCollapser` are added to `TableCapability`; no runtime shims are introduced. Hosts that previously returned `null` simply drop the method.

## Risks / Trade-offs

- [Breaking compile for mod callers of `.table()` on leaves] → Intended; callers migrate to `element()` or a host-specific accessor (`SolimStack.stack()`). Documented in tasks.
- [Large mechanical edit across host classes and tests] → Mitigated by capability-gated call sites and a single compile pass to find stragglers.
- [HostCapabilityTest assertions must change] → Updated in the same change; the test becomes the enforcement of the new contract.
- [Props generic bound] → `Props<T extends Configurable<T> & DisposableOwner>` still compiles because `Configurable` no longer declares `element()`/`cellConfig()`; `Props` casts through `ElementCapability`/`TableCapability` at the host-view boundary, which is safe given the capability contract.

## Migration Plan

1. Add accessors to `ElementCapability` and `TableCapability`; add `cellConfig()`/`alignCoordinated` defaults to `ElementCapability`.
2. Update `ContainerCapability` to extend `ElementCapability`.
3. Remove `CellCapability` and `AlignmentCapability` interfaces; delete files.
4. Update all host `implements` clauses: drop `CellCapability`/`AlignmentCapability`, add `ElementCapability` where missing, add `TableCapability` to `Spacer` and `SolimCollapser`.
5. Remove `table()`/`cellConfig()`/`alignCoordinated`/`element()` overrides from `Configurable` and from non-table hosts.
6. Update `AlignmentCapability` logic (now on `ElementCapability`) and `Props.hostView()` to gate on `TableCapability`.
7. Update `HostCapabilityTest` and capability assertions.
8. Compile `solim-core`, `solim`, and `mod`; fix stragglers.

Rollback: revert the commit; the change is atomic at the interface level.

## Open Questions

- Should `ElementCapability.element()` be declared non-null while `Component.element()` is already non-null final? Yes — keep them aligned.
- Should `Spacer`'s internal table be exposed as `TableCapability` given it has no children? Yes — it owns a real table and accepts table props; this is exactly the "table-backed leaf" case the capability model should express.