## Why

Solim components currently suffer from redundant ownership calls, an obsolete `reparentTo` escape hatch, and manual `.element()` invocations across declarative DSL methods. Because `Component.own()` is public, every component repeats manual self-registration just to bind resources to itself. Reactive primitives are also created from anywhere — constructors, modifier methods, static helpers, and reconciliation callbacks — so no single place defines where a component's reactive state comes from, and ownership of those primitives is scattered across ad-hoc `own(e)` calls.

## What Changes

- **Remove `reparentTo`**: Delete `reparentTo` from `Component` and `ElementResolver`. Components belong strictly to the owner/scope that instantiates them and are recreated if a different parent requires them.
- **Delete `own()` instead of hiding it**: Remove `own()` from `Component` outright. There is no replacement method, no renamed alias, and no package-private escape hatch — manual self-registration becomes inexpressible rather than merely discouraged. Every call site migrates to ambient ownership via `Effect.of(...)` / `OwnershipContext.register(...)`, which already adopt into the active owner.
- **`build()` Is the Only Place to Create Reactive State**: `Effect` and `Computed` may only be constructed while a component is executing `build()`. A runtime guard fails loudly otherwise. Component constructors allocate plain values only; every reactive primitive and every owned resource is created in `build()`, where the component is the active ambient owner and ownership is unambiguous.
- **`.map()` Becomes a `build()`-only Operation**: Because `Signal.map()` and `Readable.map()` return a `Computed`, deriving a value is only legal inside `build()`. This is the single largest behavioural consequence and is the point of the change: reactive state is declared where the component owns it, not sprinkled through call sites.
- **Strict Single Ownership for Disposables**: Enforce that a `Disposable` can only be owned by at most one `DisposableOwner` at a time. Adopting an already-owned `Disposable` is an illegal operation or automatically unbinds from previous owner, preventing premature disposal and use-after-free bugs.
- **`adopt()` Stays Internal**: `adopt()` remains the single adoption primitive on `DisposableOwner` because `Component`, `SessionRoot`, and `OwnershipScope` implement it across modules, but it is documented as framework-internal and guarded against use in mod sources.
- **Remove Manual `.element()` in Declarative DSL**: Remove redundant `AttachmentStack.attachToParent(x.element())` calls from factory methods in `solim.UI`. `AttachmentStack.attachPendingComponents()` is the sole mechanism that invokes `comp.element()` when the parent scope closes.

## Capabilities

### Modified Capabilities
- `solim-lifecycle`: Disallow manual component reparenting and guarantee lazy single realization of `element()` on parent attachment.
- `solim-component-ownership`: Remove `own()` as a public API, confine `Effect` and `Computed` construction to `build()`, and enforce strict single ownership for all `Disposables`.

## Impact

- `solim-api`: `Component.java`, `OwnershipContext.java`, `SolimAssert.java`, `SessionRoot.java`.
- `solim-core`: `CellConfig.java`, `ElementConfig.java`, `TableConfig.java`, plus every component that creates reactive state (`layout`, `input`, `display`, `overlay`, `reactive`, `graphics` packages).
- `solim-runtime`: `ElementResolver.java`, `AttachmentStack.java`, `OwnershipScope.java`.
- `solim`: `UI.java`.
- Tests verifying ownership, attachment behavior, the build-phase guard, and the mod encapsulation guard.
