## Context

Phase 1 left every factory at `Prop<HostCapability, V>`, so `props(...)` accepts everything. The capability information that would let a host reject a property exists only at runtime:

| Host | `table()` | Can host children | Can it use `gap()`/`padding()`? |
| --- | --- | --- | --- |
| `Column`, `Row`, `Card`, `Grid`, `Wrap`, `Tabs`, `Scroll`, `Popup`, `SolimCollapser` | non-null | yes | yes |
| `SolimStack` | **null** | yes (`SolimToken.bind(stack)`) | no |
| `Text`, `SolimImage`, `Badge`, `Divider`, `Spacer`, `SolimTextField`, `SolimSlider`, `Switch`, `Checkbox`, `NetworkImage`, `SolimVideo` | null | no | no |
| `Hud`, `SolimDialog` | non-null (a wrapper) | yes | yes |

`SolimStack` is the decisive case: `table() != null` cannot be the capability test, because a legitimate `TableCapability`-free container exists. Capability must be declared, and `props(...)` bounded so the compiler rejects an unsupported property.

`Configurable` currently declares `children(Runnable)`, so leaves expose a method that calls `OwnershipContext.push`/`pop` around a block that attaches nothing (see `Checkbox.children` and `SolimCollapser.children` for the container pattern).

`Values.alignCoordinated` dispatches alignment with an `instanceof` ladder over concrete container types because alignment must update both the parent cell (`PendingCellConfig.align`) and, for containers, the inner table's default child alignment. That ladder is the largest cast cluster left in the modifier package.

## Goals / Non-Goals

**Goals:**

- Make an unsupported property a compile error: `image().props(gap(4f))`, `text("x").props(padding(4f))`, `spacer().children(() -> …)`.
- Make `children(Runnable)` mean "this host hosts children" by moving it to a container capability.
- Replace the alignment `instanceof` ladder with polymorphic dispatch that matches the existing `GapContainer.find(...)` pattern.
- Keep the phase additive in DSL terms: `props(...)` call sites that are already correct keep compiling unchanged.

**Non-Goals:**

- Semantic markers such as "`icon` is only valid on a `Button`/`Image`" or a typed `style`. A property whose validity depends on the *value* (not just the host type) needs the third phase.
- Deleting chained configuration methods. During this phase a chained `.padding(4f)` on an image still compiles; only its `props(...)` form is rejected.
- Restricting reconciler configuration (`columns`, `key`, `items`, `overscan`), which stays fluent by decision.

## Decisions

### 1. Capability markers are declared interfaces, not Arc types

```java
public interface HostCapability {}                       // root, every host
public interface ElementCapability extends HostCapability {}   // host exposes Element
public interface TableCapability extends ElementCapability {}  // host exposes a Table
public interface CellCapability extends HostCapability {}      // host has a pending parent Cell
public interface ContainerCapability extends HostCapability {} // host accepts children
```

A `TableCapability` host implements `CellCapability` and `ContainerCapability` as well. `SolimStack` implements `ElementCapability` and `ContainerCapability` but not `TableCapability`.

- *Alternative — use `arc.scene.ui.layout.Table` as the capability*: forces `SolimStack` to be table-backed or to lie, and couples the property system to an Arc concrete class. Rejected.
- *Alternative — derive capability from `table() != null`*: cannot express `SolimStack`. Rejected.

### 2. Acceptance is `H <: A`, expressed by the `props` bound

`props(...)` is bounded `Prop<? super SELF, ?>...`. A `Prop<TableCapability, V>` is accepted by a host whose `SELF` implements `TableCapability`, because then `SELF <: TableCapability <: (super SELF)` holds for that argument. An element-only host does not satisfy `Prop<TableCapability, …>`, so `gap(4f)` is rejected. The `Requires` parameter added in Phase 1 makes this a local change to the factories, not to the DSL.

- *Alternative — runtime `IllegalArgumentException` at apply time*: catches the bug at screen build rather than compile, and the whole point is the compile-time guarantee. Rejected as the primary mechanism (a `raw(...)` escape hatch remains for genuinely dynamic cases).

### 3. `children(Runnable)` moves to `ContainerCapability`

`Configurable<SELF>` keeps `props`, `pendingProps`, `setPendingProps`, `element`, `table`, `cellConfig`, `isBuilt`, `self`, `name`. `ContainerCapability<SELF>` declares `SELF children(@Nullable Runnable block)`. Containers implement it with their current body; leaves drop it. `Props.children(Runnable)` is deleted because `props(...)` already returns `SELF`, so `props(gap(2)).children(() -> …)` still reads naturally.

- *Alternative — keep `children` on `Configurable` and no-op it for leaves*: keeps a method that attaches nothing, which is the defect. Rejected.
- *Alternative — put `children` on `ContainerCapability` without a self type parameter*: then fluent chaining after `children(...)` would return the capability, not the host. Rejected; the capability is generic over `SELF`.

### 4. Alignment dispatch is a capability, not an `instanceof` ladder

Introduce an alignment capability (name: `AlignmentCapability`) with a method to apply a composed alignment mask to both the host's parent cell and, when the host is a container, its inner table defaults. Each container implements it; `Values.alignCoordinated` calls it through the host view and its `instanceof` ladder is deleted.

- *Alternative — keep the ladder*: leaves ~11 casts and duplicates the container list in two places (`Values` and the marker definitions). Rejected.
- *Alternative — make alignment a plain `Element` prop*: cannot express the "and the inner table defaults too" half for containers. Rejected.

### 5. A host's capability set is asserted, not inferred

Each host's `implements` clause lists its capabilities explicitly. This is the single source of truth; `table() != null` is never consulted for capability. A test asserts the declared set for the hosts where the distinction matters (`SolimStack`, `SolimImage`, `Text`).

## Risks / Trade-offs

- **Many `implements` clauses change.** Mechanical, but a missed host either loses a capability (its properties stop compiling) or gains one it cannot honour (a silent no-op returns). Mitigated by a per-host test asserting the declared set and by the fact that the reviewer sees every `implements` line.
- **`mod` call sites break.** That is intended, but the count matters: every `children(...)` on a leaf and every table-kind property on an element-only host becomes an error. The proposal scopes those to this phase's tasks.
- **One suppression remains** for the generic varargs on `props(...)` (introduced in Phase 1); this phase does not add another.
- **`SolimStack` needs its own `ContainerCapability` implementation** and must not be assumed table-backed anywhere. The `ATTACHER`/`AttachmentStack` path already handles it; only the capability test must not regress to `table() != null`.
- **Raw escape hatch.** A property that is genuinely dynamic can still use `raw(...)`; after this phase `raw(...)` is explicitly capability-typed so its call site states which host it targets.

## Migration Plan

1. Add the marker types; make every host implement its declared set (additive: nothing is removed yet).
2. Narrow the `Values` factories to their real `Requires` (layout/table factories → `TableCapability`, element factories → `ElementCapability`, cell factories → `CellCapability`, child-hosting factories → `ContainerCapability`).
3. Move `children(Runnable)` to `ContainerCapability`; delete it from `Configurable` and from the leaves; delete `Props.children`.
4. Introduce the alignment capability and delete the `Values.alignCoordinated` ladder.
5. Build `solim-core` and `mod`; fix the call sites the compiler now rejects (leaf `children(...)`, element-only table props).
6. Add the capability-declaration and rejection tests.

Rollback: revert the change directory and its diff. Phase 1's typed props remain valid; only the marker bounds disappear.

## Open Questions

None.
