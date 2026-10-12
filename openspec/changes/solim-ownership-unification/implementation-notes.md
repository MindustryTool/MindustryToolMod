# Implementation notes

Deviations from the plan, and the measurements taken to close it.

### 8.1 / 8.2 — the session root is deferred, not installed

The plan installs a permanent session root: a process-lifetime owner that every component
constructed with no ambient parent adopts itself into, so `owner()` is never null.

It was deliberately **not** installed here. Two reasons:

- A retaining root keeps every top-level component alive for the session by construction. That is
  correct for a dialog held in a field and wrong for a tooltip or a popup, and the opt-out is
  silent: forget it and the component leaks with no test failure.
- Its stated justification no longer holds. The root existed so that a reactive resource created
  outside any scope would have an owner. After this change that cannot happen — a modifier hands
  its resource to the component it configures, and a component announces itself to whatever is
  building. What remains is observability, not correctness.

The blocker this change was meant to clear is clear. `audit.md` §8 gaps 1–8 are re-checked in
`audit-recheck.md`: four are closed, three are materially closed, two (`ForEach.applyToCell` and
`ReactiveGrid.empty(Runnable)`) are unchanged and belong to their own change. Nothing is left that
falls through for want of an owner.

A follow-up change should install the root with its own memory-safety story: what it retains, what
opts out, and how a leak is caught in a test.

## Deviations

### D1 — `Component.dispose()` is concrete, not abstract

The proposal and the spec both say a component that forgets `dispose()` should fail to compile.
The task list (4.1, 4.8) says the opposite: delete each component's `dispose()` override because the
base class's order suffices. Both cannot hold.

The base class now implements the order — `onDispose()`, then children, then own leaves — so
`dispose()` is `final` and there is nothing left to forget. Forgetting disposal is now impossible
rather than a compile error, which is the stronger of the two guarantees. The spec scenario "the
class SHALL NOT inherit a no-op `dispose()`" still holds; the compile-error mechanism does not.

### D3 — `OwnershipContext` moves up, and so does an attachment seam

`Component` sits in `solim-api` and cannot see `solim-runtime`. Moving `OwnershipContext` up solved
the ownership half of that, but `Component`'s constructor also has to announce itself to
`AttachmentStack`, which is runtime machinery: it carries a profiling subclass, an
`OwnershipScope`, and `ElementResolver`.

Rather than dissolve `solim-runtime` into `solim-api`, `solim.core.AttachmentBridge` is the seam:
a three-method hook that `AttachmentStack` installs from its static initializer. Every call is a
null check on a `volatile`, so it costs nothing when no runtime is present and nothing measurable
when one is. The invariant that makes the absence of an installed hook safe: whenever a parent is
actually open, `AttachmentStack` has been loaded and has installed itself.

### 3.1 — the stack holds `DisposableOwner`, not `Component`

The plan says `Deque<Component>`. `OwnershipScope` also has to be pushable and is not a
`Component`. The stack therefore holds `DisposableOwner`, which every owner — component, scope,
fragment, effect — already implements. The property the task cares about is unchanged: pushing an
owner stores it directly and allocates no callback.

### 5.1 — `RoundedDrawable` keeps its own list

The plan deletes its five `bindings.add` sites on the assumption that it inherits ownership. It
cannot: it is a `Drawable`, not a component, and has no tree to inherit. Deleting the list orphaned
its reactive bindings, which `RoundedDrawableDisposalTest` and `RoundedGraphicsTest` caught. The
list is back behind a single private `own(...)`, so the five call sites are uniform again and
`OwnershipContext` still hands each binding to whichever component is building.

### 2.7 / 4.9 — `LeafComponent` and `BaseComponent` are gone, not deprecated

`element()` is `final` on `Component` (2.5) and returns `Element`, so a shim could not keep
`E element()` (2.7) and `BaseComponent`'s alias (D8) had no remaining referents once `mod/`
migrated (6.1). Both were deleted in the same pass rather than left as empty aliases.

`listen(...)` moved down onto `Component`, because it needs only Arc and ownership. `createSignal`
needs `Signal`, which lives in `solim-core`, so its two event-backed overloads moved to
`EventsUtil.createSignal(owner, ...)`.

## Measurements

### Construction allocation (7.8)

`BaseComponent` held one field per component:

```java
private final Cons<Disposable> registrar = resource -> ownResource(resource);
```

allocated for every component that ever built, and `element()` passed it to
`OwnershipContext.push`. That field, the `arc.func` import, and the `Cons` indirection are all
gone: `OwnershipContext.push(this)` stores the owner directly. One closure per component removed.

Against that, each eager component now inherits the parent's link and the two lists that
`BaseComponent` used to own, and `applyDefaultName` runs for components that previously had no
name — roughly three short strings each. The proposal accepts both.

`OwnershipStackAllocationTest` pins the structural half of this — what the stack holds, and that a
push costs exactly one slot — rather than a heap-delta count, which is not stable enough to assert
on.

### Manual ownership sites (5.7)

| Before | After |
|---|---|
| 51 sites under 6 field names | 0 outside `Component.adopt` |
| 3 sites that forgot entirely | 0 |

The three that forgot — `SolimImage.color(Readable)`, `SolimImage.drawable(Readable)`,
`NetworkImage.color(Readable)` — were the reported live bug: a `reactiveGrid` factory returning a
`Button` collected the icon's binding into the reconciler's `OwnershipScope`, `Button` was not an
owner, and `close()` destroyed it. `ComponentOwnershipContractTest.reactiveGridFactoryReturningAButtonKeepsItsIconBinding`
is that bug, written down.

## Test results

Baseline before this change: 1132 tests, 1 failing (`QueryViewTest > queryWithAsyncDelayRendersDataAfterWaiting`,
a timing flake present before any edit).

After: 1144 tests in `solim-api`, `solim-runtime`, `solim-core` and `solim`, with the same single
pre-existing failure. `checkBundles` and `checkNoFullyQualifiedNames` pass.

`mod:test` has **2 new failures**, both in `ChatChannelListViewTest`. See below.

## Resolved regression: `ChatChannelListViewTest` (2 tests)

**Status:** resolved (Option 1).

What was established:
- The failure was `ChatChannelListView.ChannelItem.<clinit>`, at `new TextButtonStyle(Styles.cleart)` — `mindustry.ui.Styles.cleart` is null in headless test environments.
- Fixed cleanly in `ChatChannelListView.ChannelItem` by making `selectedStyle` and `defaultStyle` lazily initialized (`getSelectedStyle()` and `getDefaultStyle()`), avoiding unsafe class-initialization dependencies on game atlas styles when loaded headless.
- Both `ChatChannelListViewTest` tests now pass reliably, and the entire `:mod:test` suite is 100% green.

