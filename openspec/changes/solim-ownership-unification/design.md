## Context

The ownership audit behind `solim-reactivity-hardening` found 124 `Computed` construction sites and one broken row. Closing it surfaced the shape problem underneath: the machinery was correct, but a quarter of the component library could not receive what it was handed.

Measured, not estimated:

| | count | source |
|---|---|---|
| `Component` type references | 822 | `grep -r '\bComponent\b'` across all modules |
| `OwnershipContext` references | 166 | same |
| `SolimAssert` references | 32 | same |
| manual ownership sites | 51 | `grep 'bindings.add\|owned.add\|disposables.add\|registerDisposable\|binding = Effect.of'` |
| distinct field names for one concept | 6 | `bindings`, `owned`, `leaves`, `disposables`, `binding`, `registerDisposable` |
| sites that forgot entirely | 3 | `SolimImage.color/drawable`, `NetworkImage.color` |
| `LeafComponent` production subclasses | 4 | `SolimImage`, `NetworkImage`, `Badge`, `SolimVideo` |
| call sites needing `LeafComponent`'s concrete `E` from `element()` | **0** | `grep 'element()\.\(getTable\|getLabel\)\|Image \w+ = \w+\.element()'` |
| `extends BaseComponent` in `mod/` | 91 | `grep -r 'extends BaseComponent' mod/` |

The `0` in row seven is load-bearing: it is the measurement that dissolves `LeafComponent`.

## Goals / Non-Goals

**Goals**

- One implementation of ownership, one disposal order, one name for the leaf list.
- Reactive primitives auto-own into the component that configures them. No per-method bookkeeping, no possibility of forgetting.
- Remove the silent no-op `dispose()`/`isDisposed()` defaults.
- Remove the per-component closure from the ownership stack.
- Unblock the permanent session root and the remaining ownership gaps in `audit.md` §8.

**Non-Goals**

- Changing what `element()` returns at call sites. Measured as unused; changing it anyway would be speculative API churn.
- Renaming `BaseComponent` before the alias is retired. It is `mod/`'s primary extension point with 91 usages; the alias keeps that compiling.
- `Signal.subscribe` / `TwoWayBinding` registration (`audit.md` §8 gaps 9–10). Separate concerns, separate change.
- Any change to reactive *semantics*. This change is structural; it must not alter what tracks what.

## Decisions

### D1 — `Component` becomes the abstract base, not a marker plus two subclasses

`Component` currently is:

```java
public interface Component extends Disposable {
    Element element();
    @Override default void dispose() {}
    @Override default boolean isDisposed() { return false; }
}
```

Both defaults are hazards: a component that forgets `dispose()` leaks silently, and `isDisposed()` lies in a way that would also defeat `SolimEnv`'s teardown assertions. Making the type abstract deletes both.

Rejected — *keep the marker and add a fourth shape*. A fourth base class means a fourth copy of `dispose()`, `own()`, and the leaf list, and the same forgetting risk moves rather than disappears.

Rejected — *delete the `Component` name and rename the concept*. It is referenced in 822 places as a type (`Fragment.roots()`, `StructuralReconciler<K, C extends Component>`, `Dynamic.currentComponent`, `SolimStack.ownedLayers`). The name survives; only its kind changes.

### D2 — One class, two element strategies

The lazy/eager split is real (5 lazy, 20 eager) but does not justify a base class. The constructor carries it:

```java
protected Component()                     // lazy: build() assigns
protected Component(Element prebuilt)     // eager: element() returns immediately
```

`element()` is `final` and owns the whole lifecycle:

```java
public final Element element() {
    if (cached != null) return cached;    // eager: never builds, never pushes
    OwnershipContext.push(this);          // ownership window
    try { cached = build(); } finally { OwnershipContext.pop(); }
    if (cached == null) throw new IllegalStateException("build() returned null for " + getClass().getSimpleName());
    applyDefaultName(cached);
    return cached;
}
```

The eager path skips the window entirely, which is why the two rules in D5 matter.

*Trade-off — `element()` returns `Element`, never the concrete Arc type.* `Button.element()` stops returning `arc.scene.ui.Button`. Measured cost: zero call sites.

### D3 — `OwnershipContext` moves up into `solim-api`

```
  solim-api ──→ solim-runtime ──→ solim-core ──→ solim ──→ mod
  (Component)    (OwnershipContext)
```

`Component.element()` must push itself as owner, and it sits below the ownership stack. `Component` cannot move down instead: `Fragment`, `StructuralReconciler`, `AttachmentStack`, `OwnershipScope` and `ElementResolver` all reference it from `solim-runtime`.

So the stack moves up. `OwnershipContext` and `SolimAssert` relocate to `solim-api` under `solim.core`. Both are dependency-clean today — `OwnershipContext` needs only `arc.func.Cons`, `Component`, `Disposable`; `SolimAssert` needs only `arc.util.Nullable`.

*Alternative — keep the package `solim.runtime` while relocating the files.* Zero import churn across 166 references, at the price of a split package spanning two modules. Rejected: the user asked for a cleaner API, and a split package is the opposite. Chosen cost: 166 mechanical import updates.

*Alternative — leave `Component` in `solim-api` and have it call an injected hook.* Rejected as indirection for its own sake; the two types are one concept and belong together.

### D4 — The stack holds `Component`, not `Cons<Disposable>`

```java
  Deque<Cons<Disposable>>  +  push(this::own)   →  Deque<Component>  +  push(this)
```

After D1 every owner is a `Component`, so the indirection through `arc.func.Cons` buys nothing. `OwnershipScope` and `Fragment` implement `adopt` like everything else. This removes one allocation per built component and takes `arc.func` out of the ownership protocol — a small win, but it is free and it deletes a concept.

### D5 — The ownership window for eager components

`BaseComponent`'s window was automatic. The other 17 had none, which is precisely why they needed 51 manual sites. Two uniform rules replace all of them:

1. **A modifier hands its resource over.** `own(e)` is inherited from the base class, so `SolimImage.color(Readable)` forgetting becomes impossible.
2. **A `children()` block installs its component as ambient owner**, so anything created inside the block registers with it — the case `Button.children()` silently got wrong today, twenty lines from `Button.text()` which got it right by hand.

`ModifierOwnership.own`'s `instanceof DisposableOwner` guard is deleted: after D1 it is always true.

*Alternative — give every component a lazy `build()` window like `BaseComponent`'s.* Rejected: the fluent chain (`button().style().size().children()`) would have to either force an early build or queue the runnable, which is a much larger behavioural change and duplicates what `ReactiveGrid.children()` already does for a different reason.

### D6 — One leaf list, named once

`bindings`, `owned`, `leaves`, `disposables`, `binding` and `registerDisposable` collapse into one inherited `List<Disposable> disposables`. The 51 manual sites delete rather than move: each modifier already had to call `Effect.of`, and `own(...)` is now free and always correct.

### D7 — `applyDefaultName` runs for every component

`LeafComponent` called it in its constructor and `BaseComponent` called it after `build()`. Unifying means eager components now get names they did not have: roughly three short strings per instance, and `element.name` becomes `"solim-button-table"` rather than `null`.

Accepted. It is what makes every element identifiable in the MCP snapshot and the flame trace, the cost is negligible at construction, and it removes an inconsistency rather than adding one. Flagged as an accepted behavioural change because anything that reads `element.name` will see different values.

### D8 — `BaseComponent` is retired via an alias, not a flag day

91 `mod/` files extend it. The sequence:

1. `Component` absorbs everything; `BaseComponent extends Component` keeps only the `build()`-flavoured default and delegates.
2. `mod/` migrates `extends BaseComponent` → `extends Component` one file at a time, each independently reviewable.
3. `BaseComponent` is deleted once no references remain.

Rejected — renaming it outright in one commit. It is the framework's primary extension point; a rename that touches 91 application files at once is unreviewable and buys nothing.

## Risks / Trade-offs

**A large diff that is mostly mechanical.** 17 core classes plus 91 `mod/` files. Mitigation: the phases below are ordered so each is independently correct and independently revertible, and the `mod/` migration is one line per file.

**Silent behaviour change in `element.name`.** See D7. Mitigation: stated as an accepted change; the MCP snapshot makes it observable.

**`element()` becomes `final`.** A component can no longer customise element access — for example to lazily proxy it. None of the 25 need to, but this removes an escape hatch. Mitigation: the build/element split is internal; anyone needing different behaviour overrides `build()`.

**Eager components now register with `OwnershipContext` in their constructor.** `LeafComponent` already did; the other eager classes did too (`Button`'s constructor calls `OwnershipContext.register(this)`). Unifying changes *which* registrar receives it, not whether. Mitigation: the `SolimEnv` teardown assertions catch an unbalanced stack immediately.

**Moving `OwnershipContext` touches 166 references.** Mitigation: mechanical, IDE-refactorable, and the phase is separately revertible.

## Migration Plan

Each phase is independently correct and independently revertible.

1. **Lift** — move `OwnershipContext` and `SolimAssert` into `solim-api` under `solim.core`; update 166 imports. No behaviour change.
2. **Base** — turn `Component` into the abstract base carrying the tree, the leaf list, `adopt()` and the `onDispose → children → leaves` order. `BaseComponent` and `LeafComponent` become thin shims over it. No behaviour change for any component that already worked.
3. **Closure-free** — change the stack to hold `Component`; delete `BaseComponent.registrar`; `OwnershipScope` and `Fragment` implement `adopt`.
4. **Migrate eager classes** — 17 classes, one at a time, each independently reviewable: `Button`, `Text`, `Checkbox`, `Switch`, `SolimTextField`, `SolimSlider`, `SolimSelect`, `Divider`, `Spacer`, `Hud`, `Tabs`, `SolimStack`, `SolimDialog`, `Row`, `Column`, `Card`, `Wrap`/`Scroll`/`Grid`/`SolimCollapser`. Each: change `implements` → `extends`, delete the duplicated `constraints`/`bindings`/`disposed` fields, delete the `dispose()` override where the base's order suffices, delete the manual ownership sites, add the `children()` owner push.
5. **Delete the manual sites** — the 51 `bindings.add` / `owned.add` / `registerDisposable` calls across `RoundedDrawable`, `SolimVideo`, `SolimDialog`, `Badge`, `NetworkImage` become `own(...)` or are deleted as redundant.
6. **Retire the alias** — `mod/` moves to `extends Component`; `BaseComponent` deleted.
7. **Unblock** — revisit `solim-reactivity-hardening` tasks 7.3 and 7.9 (session root) now that nothing falls through for want of an owner.

Rollback: phases 1–3 are pure refactors and revert independently. Phase 4 is the behavioural one; if a class resists, it can stay on the old path until the rest have migrated.

## Open Questions

- Whether `SolimDialog` should keep its own `registerDisposable` public API or inherit `own`/`adopt`. It is the only class with a public registration method, and third-party code may call it.
- Whether `Row`/`Column`/`Card` should stay eager or move to the lazy `build()` flavour now that the difference is only a constructor argument. Keeping them eager is the lower-risk choice; migrating them is the more consistent one.
- Whether `applyDefaultName` should use the concrete component class name (`"solim-button-table"`) or a stable role name (`"solim-button"`). The former is better for debugging and worse for tooling that matches on names.
