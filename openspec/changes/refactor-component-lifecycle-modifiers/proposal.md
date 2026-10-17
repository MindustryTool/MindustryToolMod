## Why

Solim's component lifecycle carries three compounding problems.

**Ownership must be repaired by hand.** `Component.own()` is public, so every component that creates a resource for itself writes an explicit adoption call. Its reactive modifier interfaces contribute 30 such repairs, and reconcilers patch ownership by hand after the fact.

**Reactive state is created from anywhere.** `Effect` and `Computed` are constructed in constructors (`TwoWayBinding`, `ProgressBar`, `FeedbackBadge`, `ReactiveGrid.resolvedColumnCount`), in modifier methods (`CellConfig`, `ElementConfig`, `TableConfig`, `Button.enabled`), in static helpers (`Units.dvw`, `Units.dvh`, `Signal.map`, `Readable.map`), and during reconciliation (`PendingCellConfig.applyToCell` allocates ~10 bindings per cell on every pass). No single place defines where a component's reactive state comes from, so ownership is ambiguous and must be repaired per site.

**The modifier DSL cannot be fixed in place.** `CellConfig`, `ElementConfig`, and `TableConfig` are three self-typed mixins that a single component implements in combination. Each `default` method realizes the element eagerly, guards itself with `if (this instanceof Component)` and `if (this instanceof CellConfig)`, and — for `Readable` overloads — re-enters itself through the static overload. One user call fans out across two interfaces and a pending-cell record, and the effect lands wherever the chain happened to be written.

There is also an obsolete `reparentTo` escape hatch, and `solim.UI` factory methods call `attachToParent(x.element())` even though the `Component` constructor already queued the component for lazy realization.

## What Changes

- **Remove `reparentTo`**: Delete `reparentTo` from `Component` and `ElementResolver`. A component belongs strictly to the scope that instantiates it; a different parent instantiates a new one.
- **Delete `own()` instead of hiding it**: Remove `own()` from `Component` outright. There is no replacement method, no renamed alias, and no package-private escape hatch, so manual self-registration becomes inexpressible rather than merely discouraged.
- **Define an ownership window**: An ownership window is a span where a component is the active ambient owner — its `build()`, or a `children()` block. `children()` is universal: `Column.children()`, `Row.children()`, `Card.children()`, and `Button.children()` already call `OwnershipContext.push(this)` around the block. `build()` is not universal, because twelve components pass a prebuilt element to `super(element)` and never execute it. Both entry points are therefore part of the window.
- **Confine reactive state to an ownership window**: `Effect` and `Computed` may only be constructed inside a window, enforced by a runtime guard. Because `Signal.map()` and `Readable.map()` return a `Computed`, deriving a value is likewise window-only. Constructors allocate plain values only.
- **Replace the three modifier mixins with a deferred `Props<T>` builder**: `props(...)` records properties as pure data — resolving no Arc type, allocating no `Effect`, registering no ownership. `.children(...)` applies them, installing every reactive binding inside the window it already opens. Each `Prop` declares which Arc type it targets, so one list can carry `Table` properties such as `gap(2)` alongside `Element` properties such as `width(4)` and `color(red)`.
- **Route reconcilers through the attachment pipeline**: `ForEach`, `Dynamic`, `When`, `ReactiveGrid`, `QueryView`, and `Popup` currently hand-attach items with `container.add(el)`, then call `applyToCell` and `comp.adopt(d)` to repair ownership. Routing them through `AttachmentStack.attach(...)` lets `DEFAULT_CONFIGURATOR` handle every cell uniformly, which removes the only remaining window violation and incidentally fixes a concurrent-adoption defect.
- **Stop allocating outside windows**: `Units.dvw()` / `Units.dvh()` stop returning a `Computed`; `Query.initEffect()` moves into the owning component's window.
- **Strict Single Ownership for Disposables**: A `Disposable` may be owned by at most one `DisposableOwner` at a time.
- **`adopt()` Stays Internal**: It remains the single adoption primitive on `DisposableOwner` because `Component`, `SessionRoot`, and `OwnershipScope` implement it across modules, but it is documented as framework-internal and guarded against use in mod sources.
- **Remove Manual `.element()` in Declarative DSL**: Factory methods in `solim.UI` stop calling `AttachmentStack.attachToParent(x.element())`; `attachPendingComponents()` is the sole mechanism that resolves a pending component's element.

## Capabilities

### Modified Capabilities
- `solim-lifecycle`: Disallow manual component reparenting, guarantee lazy single realization of `element()`, and guarantee the ownership window is unwound deterministically.
- `solim-component-ownership`: Remove `own()` as a public API, confine `Effect` and `Computed` construction to an ownership window, and enforce strict single ownership.

### Added Capabilities
- `solim-config-dsl`: A deferred `Props` builder that records Element, Table, and Cell properties as data and applies them — installing all reactive bindings — inside the owning component's `children()` ownership window.

## Impact

- `solim-api`: `Component.java`, `OwnershipContext.java`, `SolimAssert.java`, `SessionRoot.java`.
- `solim-core`: new `solim.modifier.Prop`, `solim.modifier.Props`, `solim.modifier.Configurable`, and the property vocabulary; `CellConfig`, `ElementConfig`, `TableConfig` deleted; host wiring across `layout`, `input`, `display`, `overlay`, `reactive`, `graphics`; reconciler attachment routing.
- `solim-runtime`: `ElementResolver.java`, `AttachmentStack.java`, `OwnershipScope.java`.
- `solim`: `UI.java`.
- `mod`: **174 files, ~4,837 call sites** across 40 feature packages; 3,031 sites (62.7%) sit inside chains of 3 or more modifiers. No mod class implements the three mixin interfaces, so this is a pure call-site rewrite with no type hierarchy to re-plumb. Call-site profile: 2,763 static, 1,607 no-argument, 128 callback, and **339 reactive** — the only group that allocates effects and therefore the only group where semantics must be preserved exactly.
