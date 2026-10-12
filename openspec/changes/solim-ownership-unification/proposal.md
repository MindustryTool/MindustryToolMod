# Solim Ownership Unification

## Why

Solim has one ownership concept implemented three times, and the copies disagree.

```
  Component (interface, 2 default methods)     BaseComponent        LeafComponent<E,SELF>
  ┌───────────────────────────────────┐       ┌──────────────────┐  ┌──────────────────────┐
  │ Element element()                 │       │ tree + leaves    │  │ element at ctor     │
  │ default void dispose() {}  ← trap │       │ element() pushes │  │ flat leaves         │
  │ default isDisposed() → false ←trap│       │   owner for      │  │ NO ownership window │
  └───────────────────────────────────┘       │   build()        │  │ NO build()          │
              ▲                             └──────────────────┘  └──────────────────────┘
              │ implements                                        4 subclasses
   ┌──────────┴───────────┐                                         generics unused
   │ Button  Text  Checkbox│         Row  Column  Card  Wrap
   │ Switch  SolimTextField│         Scroll  Grid  SolimCollapser
   │ SolimSlider … Hud Tabs│         SolimDialog  Tabs  SolimStack
   └──────────────────────┘
     17 classes, each with its own
     bindings / owned / disposables / registerDisposable
```

Consequences, all measured rather than estimated:

- **51 hand-written ownership sites** across 17 classes, under **6 different field names** for one concept.
- **3 sites forgot entirely**: `SolimImage.color(Readable)`, `SolimImage.drawable(Readable)`, `NetworkImage.color(Readable)` create an `Effect` and store nothing.
- **The forget is a live bug.** A `reactiveGrid` factory returning a `Button` collects the icon's colour `Effect` into the reconciler's `OwnershipScope`; `Button` is not a `DisposableOwner`, so `adoptInto` is a no-op and `close()` destroys it. The icon keeps the colour from its one synchronous initial run and never updates again.
- **`Component`'s defaults are a trap.** A component that forgets `dispose()` silently leaks forever while `isDisposed()` reports `false`.
- **A closure per component.** `OwnershipContext.push(this::own)` allocates a `arc.func.Cons` for every component that is ever built.
- **The session root is blocked** (`solim-reactivity-hardening` task 7.3) because 17 components cannot accept what they're handed, so a retaining root would convert harmless drops into permanent retention.

## What Changes

### One base class

`Component` stops being a marker interface and becomes the abstract base that owns everything shared: the parent link, the child list, the flat leaf list, `adopt()`, and the `onDispose() → children → leaves` disposal order. Its two no-op defaults are deleted; forgetting to implement `dispose()` becomes a compile error rather than a silent leak.

`BaseComponent` and `LeafComponent` dissolve into it. `BaseComponent` is kept as a **deprecated alias** so the 91 `extends BaseComponent` sites in `mod/` keep compiling during migration, then removed.

### One element strategy, two ways in

`Component` supports both shapes through its constructor, so there is no second base class:

```java
protected Component()                        // lazy:   build() assigns the element
protected Component(Element prebuilt)        // eager:   element() returns immediately
```

`LeafComponent`'s generic `element()` return type is dropped. Measured: **zero** call sites depend on the concrete Arc type from `element()`.

### The ownership window is uniform

`BaseComponent`'s window came for free from `element()` wrapping `build()`. The other 17 classes had none, which is exactly why they hand-rolled `bindings.add(e)`. Two rules replace all 51 sites:

```java
// 1. a modifier hands its resource to the component it configures
Effect e = Effect.of(body);
own(e);                       // inherited, unconditional, no instanceof guard

// 2. a children() block installs its component as the ambient owner
OwnershipContext.push(this);
AttachmentStack.push(table, ATTACHER);
try { r.run(); } finally { AttachmentStack.pop(); OwnershipContext.pop(); }
```

`ModifierOwnership`'s `instanceof DisposableOwner` guard disappears: after this change it is always true.

### No closures in the ownership path

The ambient stack holds `Component` directly instead of `arc.func.Cons`, so pushing an owner allocates nothing. `OwnershipScope` and `Fragment` implement `adopt` like everything else. `arc.func` leaves the ownership protocol entirely.

### `OwnershipContext` moves to `solim-api`

`Component.element()` must push itself as owner, but `Component` sits a module below `OwnershipContext`. The two are one concept — "the ambient scope a `Component` participates in" — so `OwnershipContext` and `SolimAssert` move up into `solim-api` under `solim.core`. `Component` is still referenced from `solim-runtime` (`Fragment`, `StructuralReconciler`, `AttachmentStack`, `OwnershipScope`), so it cannot move down instead.

## Capabilities

### New Capabilities

- `solim-ownership-unification`: a single component base class carrying the ownership tree, one disposal order, one uniform rule for reactive resources, and a closure-free ambient ownership stack.

### Modified Capabilities

- `solim-lifecycle`: disposal order and tree semantics are now the base class's contract rather than `BaseComponent`'s; `withoutAutoOwnership` and `Raw pause/resume` stay removed.
- `ambient-naming`: `OwnershipContext` moves to `solim-api`; `Component` becomes an abstract class rather than an interface, so `implements Component` becomes `extends Component`.
- `disposable-contract`: components no longer inherit no-op `dispose()`/`isDisposed()` defaults.
- `solim-reactivity`: reactive primitives auto-own into the component that configures them, with no per-method bookkeeping.

## Impact

**Affected modules**

- `:solim-api` — `Component` becomes abstract and carries the tree; `OwnershipContext` and `SolimAssert` move here; new `Ownership` contract
- `:solim-runtime` — loses `OwnershipContext`/`SolimAssert`; `Fragment`, `OwnershipScope`, `AttachmentStack`, `StructuralReconciler` compile against the moved types
- `:solim-core` — `BaseComponent` and `LeafComponent` dissolve; **17 classes** migrate from `implements Component` to `extends Component`; 51 manual ownership sites deleted
- `:mod` — 91 `extends BaseComponent` → `extends Component` (mechanical, one line each); `extends BaseComponent` alias keeps them compiling until then
- `:solim` — facade unchanged in shape; may drop fully-qualified re-exports

**Performance**

- Per-frame cost: **unchanged**. Ownership runs at construct → build → dispose only; nothing in this change touches layout or draw.
- Construction: roughly neutral. `Button` swaps its own `bindings` for the inherited `disposables`; containers gain one empty `children` list and one `parent` reference (~70 B each, tens per screen).
- Construction **improves** by one allocation per component: the `Cons` closure per `push(this::own)` disappears.
- New small cost: `applyDefaultName` now also runs for the eager components (`Button`, `Text`, …), roughly three short strings per instance. Accepted — it is what gives every element a debuggable name for the MCP server and flame traces.

**Behavioural risks accepted**

- `applyDefaultName` assigns `"solim-button-table"`-style names to elements that previously had none.
- The ownership stack becomes single-threaded by construction (it was already guarded by `SolimAssert`).
- `element()` becomes `final`; no component may customise element access. None of the 25 need to.

**Unblocks**

`solim-reactivity-hardening` task 7.3 (permanent session root) and `audit.md` §8 gaps 1–8, because after this change nothing falls through the ownership machinery for want of an owner.

**Not addressed here**

- `Signal.subscribe(...)` returns a `Subscription` the caller must still keep; `audit.md` §8 gap 9.
- `TwoWayBinding` is not itself registered; gap 10.
- Push-based invalidation, `Signal.set()`'s observer-list copy, and the `ThreadLocal` inconsistency between `ReactiveContext` and the other pools remain out of scope, unchanged from the parent change.
