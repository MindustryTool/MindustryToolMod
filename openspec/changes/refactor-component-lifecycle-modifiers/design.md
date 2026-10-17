## Context

Solim applies UI properties through three mixin interfaces that a single component implements in combination:

```java
public final class Column extends Component
        implements CellConfig<Column>, ElementConfig<Column>, TableConfig<Column>, GapContainer {
```

Each interface contributes `default` methods that resolve an Arc type through an abstract accessor (`element()` or `table()`), mutate it, and — for `Readable` overloads — allocate an `Effect` and hand-ownership back manually:

```java
// ElementConfig.java:42
default SELF width(@Nullable Readable<Float> width) {
    if (width == null) return self();
    if (this instanceof CellConfig) ((CellConfig<?>) this).cellConfig().prefWidth = width;
    Element el = element();
    if (el != null) {
        Effect e = Effect.of(() -> { Float w = width.get(); if (w != null) width(w); });
        ((Component) this).own(e);          // repair: the effect was adopted by the caller's scope
    }
    return self();
}
```

Three consequences follow from the modifier body running at the call site:

1. **Ownership must be repaired by hand** — 30 `own()` sites across the three interfaces.
2. **A component is several mixins at once** — 25+ `if (this instanceof Component)` and `if (this instanceof CellConfig)` branches reaching across interfaces.
3. **The element is realized before the chain finishes** — each modifier calls `element()` immediately.

Separately, the ownership model has two escape hatches. `Component.own()` is public, so external code can corrupt the ownership tree. And `reparentTo` lets a component change owners after instantiation, which `ElementResolver` uses as a hack.

Measured usage in `mod/` (418 files total):

| Metric | Count |
|---|---:|
| Files with ≥1 modifier call | 174 |
| Total call sites | 4,837 |
| Sites inside chains of ≥3 modifiers | 3,031 (62.7%) |
| Static value arguments | 2,763 |
| No-argument calls | 1,607 |
| Callback arguments | 128 |
| Reactive arguments | 339 (7.0%) |
| Mod classes implementing the mixins | 0 |
| Distinct method names in use | 58 |

## Goals / Non-Goals

**Goals:**
- Remove `reparentTo` and delete `own()` outright.
- Define an ownership window (`build()` or `children()`) and confine `Effect` / `Computed` construction to it, enforced at runtime.
- Replace the three modifier mixins with one deferred `Props<T>` builder, so no modifier needs `own()` and no modifier realizes an element at the call site.
- Make reconcilers use the attachment pipeline, eliminating manual ownership repair after reconciliation.
- Keep cell sizing correct by writing into `PendingCellConfig`, reusing the existing attachment pipeline unchanged.
- Enforce strict single ownership so no `Disposable` is ever adopted by two live owners.
- Migrate the 4,837 mod call sites incrementally, then delete the mixins so the compiler enforces completion.

**Non-Goals:**
- Rewriting `AttachmentStack`, `ElementResolver`, or the retained Arc UI model.
- Changing `PendingCellConfig`'s resolution contract or `DEFAULT_CONFIGURATOR`'s behaviour.
- Converting eagerly constructed components to lazy `build()`; the `Props` design is chosen precisely so that conversion is unnecessary.
- Promoting `raw(...)` call sites to first-class properties. That is a possible follow-up once the escape hatch's real usage is known.

## Decisions

### 1. Remove `reparentTo` and enforce immutable component ownership
- **Decision:** Delete `reparentTo` from `Component` and `ElementResolver`.
- **Rationale:** A component belongs strictly to the scope that created it. Declarative architecture dictates recreating a component when a different parent needs the same structure.
- **Alternatives considered:** Keeping an internal reparenting hook. Rejected because no legitimate declarative use case requires mutating component ownership after instantiation.

### 2. The ownership window has two entry points: `build()` and `children()`
- **Decision:** An ownership window is any span where the component is the active ambient owner — its `build()` or a `children()` block. Reactive primitives may only be created inside one.
- **Rationale:**
  - `children()` is the universal window. `Column.children()` (`Column.java:204`) and `Button.children()` (`Button.java:87`) already call `OwnershipContext.push(this)` before the block and pop in `finally`.
  - `build()` is **not** universal. `Column`, `Row`, `Card`, `Grid`, `Wrap`, `Spacer`, `Badge`, `Divider`, `SolimImage`, `NetworkImage`, and `SolimVideo` all pass a prebuilt element to `super(element)`, so `build()` never runs for them. Defining the window as `build()` alone would be unsatisfiable for the bulk of the DSL.
  - A window is the only place where ownership needs no per-site repair, because `Effect.of(...)` and `OwnershipContext.register(...)` already adopt into the active owner. Confining creation to one removes the reason `own()` exists at all.
  - Today the ambient owner during a *constructor* is still the parent, which is why `Tabs.own(headerBar)`, `Divider.own(image)`, `Badge.own(label)`, and the `TwoWayBinding`s in `Switch` / `Checkbox` / `SolimSlider` / `SolimSelect` / `SolimTextField` had to hand-register.
  - A silent "you probably meant to do this in a window" is worse than a throw, because the alternative is a reactive primitive adopted by whichever scope happened to be ambient.
- **Alternatives considered:**
  - `build()` only. Rejected: unsatisfiable for the twelve eager components above, and would require converting them first.
  - `children()` only. Rejected: lazy components that build children reactively outside a `children()` block would have no window.
  - Anywhere a component is the ambient owner, including reactive callbacks. Rejected: that is the existing contract renamed, and it leaves reconciliation as the dominant creation site.

### 3. `Props` is generic over the host; `Prop` is self-describing about its Arc target
- **Decision:** `Props<T>` where `T` is the DSL host component, holding a `List<Prop>`. Each `Prop` declares a `Kind` of `ELEMENT`, `TABLE`, or `CELL`.
- **Rationale:** A single type parameter cannot be the Arc type, because the motivating call mixes a `Table` property with `Element` properties:
  ```java
  .props(gap(2), width(4), color(Color.red))
  ```
  Parameterizing over the host lets `children(...)` return `T` directly and lets one list carry properties for every Arc type the host exposes. The host is exactly the component that already supplies `element()`, `table()`, and `cellConfig()`, so dispatch needs no new plumbing.
- **Alternatives considered:** `Props<Table>` and `Props<Element>` composed via an `allOf(...)`. Rejected: it makes the common mixed call verbose and pushes the merge problem onto every call site.

### 4. `props(...)` is pure; `children()` is the realization point
- **Decision:** `props(...)` allocates a list and nothing else. Properties are applied at the start of `children()`.
- **Rationale:**
  - Because `props(...)` is pure, calling it at any call site is safe, and the ownership-window rule is satisfied for the entire modifier DSL with no exemptions.
  - Applying at the start of `children()` also means the component is the ambient owner while its own properties are installed, so `Effect.of(...)` auto-registers.
  - Application order preserves today's last-write-wins behaviour, so a chain mixing both styles during migration has a defined outcome.
- **Alternatives considered:**
  - A dedicated `Component.configure()` hook. Rejected: it requires touching every component and adds a third realization path alongside `build()` and `children()`.
  - Converting all containers to lazy `build()` first. Rejected: architecturally tidy but a large prerequisite that changes the eager/lazy contract in `solim-component-ownership`.

### 5. Reactive properties install as effects during `applyAll()`
- **Decision:** A `Prop` holds either a static value or a `Readable`. `applyAll()` applies static values directly and wraps readables in `Effect.of(...)`.
- **Rationale:** `applyAll()` runs inside `children()` with the component as the active ambient owner, so `Effect.of(...)` auto-registers through `OwnershipContext.register(...)` and needs no `own()`. This deletes all 30 repair sites in one step and gives the 339 reactive mod call sites identical ownership semantics to today.

### 6. Cell properties are recorded, and the attachment path is already compliant
- **Decision:** `Kind.CELL` properties mutate the host's `PendingCellConfig` rather than a live cell.
- **Rationale:** `Column.children()` and its siblings order the teardown so that `OwnershipContext.pop()` runs *after* `AttachmentStack.pop()`:
  ```java
  } finally {
      AttachmentStack.pop();     // attachPendingComponents -> attach -> DEFAULT_CONFIGURATOR
      OwnershipContext.pop();    // owner popped only afterwards
  }
  ```
  The configurator therefore executes while the container is still the ambient owner, so the bindings it creates are already inside a window and owned by the correct component. `applyToCell` is the mechanism that transfers recorded configuration onto a live cell and cannot be deleted; it simply does not need to change.
- **Note:** the same ordering makes deferred cell configuration safe without any new mechanism, and `GapContainer.respace(cell.getTable())` still runs from the configurator.

### 7. Reconcilers route through the attachment pipeline
- **Decision:** `ForEach`, `Dynamic`, `When`, `ReactiveGrid`, `QueryView`, and `Popup` attach items through `AttachmentStack.attach(...)` instead of `container.add(el)`, and their direct `applyToCell` calls and `comp.adopt(d)` repair loops are deleted.
- **Rationale:** These are the only remaining window violations, and they are violations for a mechanical reason: hand-attaching bypasses the attachment pipeline, so the configurator never runs and the reconciler patches up afterwards.
  ```java
  // ForEach.java:181
  Cell<?> cell = container.add(el);                     // configurator never runs
  List<Disposable> cellEffects = sc.applyToCell(cell);  // during a signal flush — outside a window
  itemBindings.addAll(cellEffects);
  for (Disposable d : cellEffects) comp.adopt(d);       // manual repair
  ```
  Routing attachment through the pipeline lets `DEFAULT_CONFIGURATOR` handle every cell uniformly, removes the violation without any exemption, and leaves exactly one code path for cell configuration.
- **Incidental defect fixed:** that snippet registers each cell effect in **both** `itemBindings` and `comp` — concurrent adoption by two live owners, which decision 4 prohibits. It is benign today only because `Effect.dispose()` is idempotent.
- **Alternatives considered:**
  - A permanent documented exemption. Rejected: it leaves the rule porous and the double-adoption in place, for sites already being touched.
  - Wrapping each reconcile body in the reconciler's own window. Rejected: it keeps two mechanisms for cell configuration and reintroduces a third window entry point.

### 8. A public escape hatch covers non-setter modifiers
- **Decision:** `raw(...)` accepts an arbitrary mutation against a chosen `Kind`.
- **Rationale:** 1,259 of the 4,837 mod call sites (26%) are component-specific fluent methods outside the three mixins — `color`, `style`, `icon`, `wrap`, `checked`, `text`, `fontScale`. Several are not expressible as a single setter:
  - `Column.align()` performs four coordinated actions: delegate to `TableConfig.align`, write `cellConfig().align`, apply to the parent cell, and re-apply to every existing cell.
  - `Column.gap()` mutates a field and then triggers a whole-table `respace()`.
  - `Button.style(Cons<SolimButtonStyleBuilder>)` allocates an `Effect` conditionally on whether the resolved builder is static.

  Without an escape hatch these would strand on a second chained API and the migration could never finish.

### 9. `Query` and `Units` stop allocating outside a window
- **Decision:** `Units.dvw()` and `Units.dvh()` stop returning a `Computed` and become plain value helpers. `Query.initEffect()` moves into the owning component's window.
- **Rationale:** `Query` is not a `Component` and has no window of its own; it initialises its effect from `Query.java:91`, outside any ownership scope. `Units.dvw()` / `Units.dvh()` (`Units.java:61-81`) return `new Computed<>(...)` from a static helper, so merely calling them allocates a reactive primitive at an arbitrary call site. Exempting them would leave exactly the ambiguity the window rule exists to remove, and would sit next to the public `signal()` / `computed()` factories it would contradict.
- **Consequence to accept:** `signal().map(...)` remains the sanctioned way to derive a value, and the static convenience helpers lose their reactive signature.

### 10. The guard lands after the migration, not alongside it
- **Decision:** The runtime guard ships as the last step of the migration, not with the `own()` removal.
- **Rationale:** Until the modifier mixins are gone, a chained `.padding(readable)` would throw. Landing the guard early would require a carve-out mechanism whose only purpose is to be deleted, and a JVM-wide guard cannot cleanly distinguish framework-internal allocation from application allocation. Sequencing the guard after the migration means it activates with zero violations remaining and needs no exemptions.
- **Ordering consequence:** the guard is what *forces* call sites to move, so it must land **before** `own()` is deleted. Deleting `own()` first would simply relocate the migration work into the deletion step.

### 11. Parallel DSL, then delete the mixins
- **Decision:** `Props` ships alongside the existing mixins; the mod migrates package by package; the three interfaces are deleted in a final step that breaks the build on any remaining call site.
- **Rationale:** A 4,837-site rewrite in one commit has no incremental safety net. Since no mod class implements the mixins — `solim.core.Component` does not implement them either — migration is purely call-site rewriting with no hierarchy changes, which makes an incremental path low-risk. The terminating compile error is a stronger completeness guarantee than a hand-audited checklist.
- **Note on tooling:** 3,031 sites sit inside chains of 3+ modifiers and are mechanically transformable (`expr.m1(a).m2(b)` → `expr.props(m1(a), m2(b))`). A codemod is worth writing for the static majority, but the 339 reactive sites and the 1,259 component-specific sites still need hand review.

## Risks / Trade-offs

- **[Risk] `gap()` timing changes**: today `gap()` calls `respace()` immediately at the call site; deferred, it runs at the start of `children()`, before the block that adds children.
  - *Mitigation*: `GapContainer.spaceAttachedCell` runs on each attach and `children()` already calls `respace()` at the end. Add a visual regression check for vertical and horizontal spacing with mixed child sizes.
- **[Risk] Two DSL styles alive simultaneously**: a chain mixing both styles could apply in an order the author did not intend.
  - *Mitigation*: `Props` applies at the start of `children()`, so a chained modifier written after `.props(...)` still wins and one written before is overridden. Document the precedence rule and pin it with a test.
- **[Risk] Components with no `children()`**: `Text`, `Spacer`, `Divider`, and bare `image()` have no `children(Runnable)` to hook.
  - *Mitigation*: a terminal `apply()` plus auto-apply at `element()` for any component carrying an unapplied `Props`.
- **[Risk] Reconciler routing changes item attach order**: items are currently added synchronously inside `reconcile()`; routing through `AttachmentStack` must not defer or reorder them.
  - *Mitigation*: `AttachmentStack.attach(...)` is synchronous. Add a test asserting reconciled items appear in key order and that cell constraints are applied exactly once per item.
- **[Risk] Removing `Units.dvw()` / `Units.dvh()` signatures** is a public API break beyond the mixin deletion.
  - *Mitigation*: both are re-expressed as plain value helpers, and the change already breaks mod call sites wholesale, so the delta is absorbed.
- **[Risk] Migration stalls with both APIs live**: 174 files is a long tail.
  - *Mitigation*: migrate by feature package in descending site count, and treat the mixin deletion as the change's completion criterion rather than a deferrable task.
