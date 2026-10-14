## Context

Solim's declarative architecture expects components to be created inside tree-building scopes (`children(() -> ...)`), registered to ambient ownership (`OwnershipContext`), and mounted into the retained Arc UI tree by their parent containers. However, three legacy habits currently conflict with this vision:
1. `reparentTo`: Allows components to move across owners, used only as a hack in `ElementResolver`.
2. Public `Component.own()`: 60+ call sites perform manual self-registration, so binding a resource to "this" is something every component must write by hand.
3. Reactive state is created from anywhere: `Effect` and `Computed` are constructed in constructors (`TwoWayBinding`, `ProgressBar`, `FeedbackBadge`, `ReactiveGrid.resolvedColumnCount`), in modifier methods (`CellConfig`, `ElementConfig`, `TableConfig`, `Button.enabled`), in static helpers (`Units.dvw`, `Units.dvh`, `Signal.map`, `Readable.map`), and during reconciliation (`PendingCellConfig.applyToCell` allocates ~10 fresh bindings per cell on every pass). Ownership of that state is therefore scattered and must be repaired by `own(e)` at each site.
4. Duplicate attachment in `solim.UI`: Factory methods call `AttachmentStack.attachToParent(x.element())` immediately, while `Component`'s constructor already enqueues `this` to `entry.pendingComponents` to be realized lazily upon `pop()`.

## Goals / Non-Goals

**Goals:**
- Eliminate `reparentTo` from `Component` and `ElementResolver`.
- Delete `own()` outright, so manual self-registration is not merely discouraged but inexpressible.
- Confine `Effect` and `Computed` construction to `Component.build()`, enforced at runtime, so `build()` is the single place a component declares its reactive state.
- Confine every owned resource — reactive or not — to `build()` as well, so `Component`'s ambient owner frame is pushed exactly once and only around the build.
- Eliminate manual `attachToParent(x.element())` from `solim.UI`, letting `AttachmentStack.attachPendingComponents()` realize elements once and only once when added to parent.
- Enforce strict single ownership so no `Disposable` is ever adopted by two live owners.
- Ensure all existing unit and integration tests pass cleanly with zero leaks.

**Non-Goals:**
- Rewriting the retained Arc UI model or `AttachmentStack` core mechanics.
- Deferring modifier application in `CellConfig`, `ElementConfig`, and `TableConfig` to realization time. Those interfaces keep their current eager `element()` behavior and their `PendingCellConfig` naming; only their redundant `((Component) this).own(e)` calls are removed as a consequence of deleting `own()`. Migrating the modifiers themselves to `build()`-created effects is a separate follow-up change.
- Reworking reconciliation so that `PendingCellConfig.applyToCell` reuses bindings instead of reallocating them. The 1.3 guard deliberately does not cover binding allocation performed by the framework's own reconcilers.

## Decisions

### 1. Remove `reparentTo` and enforce immutable component ownership
- **Decision:** Delete `reparentTo` from `Component` and `ElementResolver`.
- **Rationale:** A component belongs strictly to the scope that created it. If a component is needed under a different parent or after disposal, declarative architecture dictates recreating the component.
- **Alternatives considered:** Keeping an internal reparenting hook. Rejected because no legitimate declarative use case requires mutating component ownership after instantiation.

### 2. Strictly Enforce Single Ownership for all `Disposable`s
- **Decision:** Every `Disposable` SHALL only ever be owned by at most one `DisposableOwner` at any given time.
- **Rationale:** Shared ownership causes catastrophic lifecycle bugs in reactive systems: when Owner A is disposed, the shared resource is destroyed, leaving Owner B holding a dead/disposed resource (premature disposal / use-after-free).
- **Implementation Strategy:**
  - Track current owner or assert on re-adoption.
  - If a `Disposable` is registered with an owner while already belonging to an active owner, reject or disallow multiple concurrent ownership.
  - Components and bindings form a strict ownership hierarchy tree without multi-owner DAGs.

### 3. `build()` is the only legal site for `Effect` and `Computed`
- **Decision:** Constructing an `Effect` or a `Computed` outside a `Component.build()` frame SHALL throw `IllegalStateException`.
- **Rationale:**
  - `build()` is the one place where the component is guaranteed to be the active ambient owner, because `Component.element()` pushes the component for exactly that window. Confining reactive construction to it means ownership needs no per-site repair at all — which is what makes deleting `own()` safe rather than merely bold.
  - Today the ambient owner during a *constructor* is still the parent, which is precisely why `Tabs.own(headerBar)`, `Divider.own(image)`, `Badge.own(label)`, and the `TwoWayBinding`s in `Switch` / `Checkbox` / `SolimSlider` / `SolimSelect` / `SolimTextField` had to hand-register. Moving that work into `build()` removes the whole category instead of relocating it.
  - A silent "you probably meant to do this in `build()`" is worse than a throw, because the alternative outcome is a reactive primitive adopted by whichever scope happened to be ambient.
- **Implementation Strategy:**
  - `Component` exposes a build-phase flag set immediately around `build()` in `element()` and cleared in `finally`, readable without depending on `solim-runtime`.
  - `Effect.of(...)` and the `Computed` constructor assert the flag and throw `IllegalStateException` naming the offending call site otherwise.
  - `.map()` is covered transitively: `Signal.map()` and `Readable.map()` return a `Computed`, so deriving a value becomes a `build()`-only operation. This is the intended, visible consequence — `Units.dvw()` / `Units.dvh()` and every `.map()` call site move into `build()`.
  - `Query` and `Units` have no `build()`. `Query.initEffect()` moves to the point where the owning component builds, and the static `Units` helpers are re-expressed so they do not allocate a `Computed` at the call site.
  - `PendingCellConfig.applyToCell` allocates per-cell bindings during reconciliation, which runs inside an effect body rather than a build frame. The guard therefore exempts framework-internal binding allocation, and that exemption is an explicit Non-Goal to be closed by the follow-up change noted above.
- **Alternatives considered:**
  - Allowing creation in constructors as well. Rejected: the component is not the ambient owner during its own constructor, so it would preserve the multi-owner DAG the change exists to remove.
  - Allowing creation anywhere a component is the ambient owner, including reactive callbacks. Rejected: that is the existing contract renamed, and it leaves reconciliation as the dominant creation site.

### 4. Delete `own()` and keep `adopt()` as the internal primitive
- **Decision:** `Component.own()` is removed with no replacement. `adopt()` stays on `DisposableOwner`, documented as framework-internal.
- **Rationale:**
  - A renamed or package-private `ownSelf()` would preserve the very manual self-registration this change exists to delete. Deletion is the only variant that makes the omission impossible to get wrong.
  - `adopt()` cannot be package-private: `DisposableOwner` is a public interface implemented by `Component` and `SessionRoot` in `solim-api` and by `OwnershipScope` in `solim-runtime`. Restricting it is a documentation and guard-test concern rather than a visibility one.
- **Implementation Strategy:**
  - `Effect.of(...)` and `OwnershipContext.register(...)` already route through `OwnershipContext.register`, so deleting `own()` needs no new registration API.
  - `Component.listen(...)` builds its `DisposableAction` and hands it to `OwnershipContext.register(...)`.
  - `CellConfig`, `ElementConfig`, and `TableConfig` lose their `((Component) this).own(e)` lines entirely.
  - The mod encapsulation guard test rejects `own(`, `adopt(`, and `reparentTo(` in mod sources.
- **Alternatives considered:** Letting constructor-time resources fall to the ambient parent scope. Rejected: it silently changes disposal ownership and, once 1.3 lands, those constructors can no longer create reactive state at all.

### 5. Remove manual `.element()` in `solim.UI`
- **Decision:** Factory methods in `solim.UI` (`button()`, `image()`, `badge()`, `video()`, `card()`, `row()`, etc.) return the component without calling `AttachmentStack.attachToParent(x.element())`.
- **Rationale:** The `Component` constructor already invokes `AttachmentBridge.registerPending(this)`. When the containing scope's `AttachmentStack.pop()` runs, `attachPendingComponents()` resolves `comp.element()` once, attaches it to the parent Table, and configures its Cell.

## Risks / Trade-offs

- **[Risk] `.map()` becomes illegal outside `build()`**: This is the widest-reaching consequence and it is intentional, but it turns ordinary-looking derivations into errors for any caller that has not yet migrated.
  - *Mitigation*: The guard throws with a message naming the required frame, and every internal `.map()` call site is migrated in the same change (1.5).
- **[Risk] Standalone component creation without an ambient parent table**: If someone instantiates `new Button()` outside any `AttachmentStack` scope and never calls `.element`, the Arc element is never realized.
  - *Mitigation*: That is the exact definition of lazy declarative components! Root boundaries that mount views (e.g., Dialogs or standalone HUD roots) explicitly call `.element()` or use `View.mount()` / `OwnershipScope.build()`.
- **[Risk] Constructor-created child components lose their owner**: A child component constructed in a parent's constructor is currently claimed with `own(...)`; once `own()` is gone and construction self-ownership is not added, it attaches to the ambient parent instead.
  - *Mitigation*: Task 1.6 moves every such child into `build()`, where the component is the active ambient owner. `Divider`, `Badge`, `Tabs`, `SolimDialog`, and `SolimVideo` are the sites to check.
- **[Risk] Leaked build frame**: A `build()` that throws or a component disposed mid-build could leave the component as the ambient owner.
  - *Mitigation*: The flag is cleared in `finally` and `OwnershipContext.pop(this)` already runs in the same `finally`. A leak-check assertion fails the build if the ambient stack is non-empty after a `build()` returns.
- **[Risk] Unit tests that assert on manual ownership**: Some legacy tests call `own(...)` directly to observe disposal ordering, and many create `Effect`s outside any `build()`.
  - *Mitigation*: Tests assert observable behavior — that a resource is disposed with its component and not with the parent — inside a `build()` scope rather than by invoking an ownership method.
