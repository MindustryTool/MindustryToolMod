## Context

Solim's declarative architecture expects components to be created inside tree-building scopes (`children(() -> ...)`), registered to ambient ownership (`OwnershipContext`), and mounted into the retained Arc UI tree by their parent containers. However, five legacy habits currently conflict with this vision:
1. `reparentTo`: Allows components to move across owners, used only as a hack in `ElementResolver`.
2. Redundant `.own(e)` in mixin interfaces: Calling `((Component) this).own(e)` when `Effect.create(...)` already registers into `OwnershipContext.current()`.
3. Eager realization in `CellConfig`: Calling `((Component) this).element()` on every modifier call (`.minWidth()`, `.maxWidth()`, etc.) coupled with 25+ `if (this instanceof Component)` checks, realizing the element before layout structure and chaining complete.
4. Duplicate attachment in `solim.UI`: Factory methods call `AttachmentStack.attachToParent(x.element())` immediately, while `Component`'s constructor already enqueues `this` to `entry.pendingComponents` to be realized lazily upon `pop()`.
5. Public visibility of `Component.own()` and `adopt()`: Allows external and mod code to manually disrupt ambient ownership.

## Goals / Non-Goals

**Goals:**
- Eliminate `reparentTo` from `Component` and `ElementResolver`.
- Restrict `own()` and `adopt()` to package-private internal engine scope.
- Remove redundant `((Component) this).own(e)` calls in `CellConfig`, `ElementConfig`, and `TableConfig`.
- Transform `CellConfig` into a pure, deferred property recorder without `element()` calls or `instanceof Component` checks.
- Eliminate manual `attachToParent(x.element())` from `solim.UI`, letting `AttachmentStack.attachPendingComponents()` realize elements once and only once when added to parent.
- Ensure all existing unit and integration tests pass cleanly with zero leaks.

**Non-Goals:**
- Rewriting the retained Arc UI model or `AttachmentStack` core mechanics.
- Changing `PendingCellConfig` resolution or the contract of `DEFAULT_CONFIGURATOR`.
- Breaking backward compatibility of public DSL signatures in `solim.UI.*`.

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

### 3. Clean Configuration Records (`CellConfig`, `ElementConfig`, `TableConfig`) without "Pending" Prefix
- **Decision:** 
  - Standardize configuration records without the redundant `Pending` prefix. A modifier configuration in a declarative framework is naturally deferred until realization.
  - Modifier mixins purely record properties into configuration data holders during declaration.
- **Rationale:** 
  - Naming classes `PendingCellConfig`, `PendingElementConfig`, etc., adds needless noise and artificial complexity. Simple, clean names (`CellConfig`, `ElementConfig`, `TableConfig`) match project rules: *"variable, function must explain itself"* and avoid backend-style verbosity.
  - When modifier methods like `.padding(Readable)` or `.width(Readable)` are called during DSL declaration, the parent container is currently on top of `OwnershipContext`.
  - Creating `Effect.of(...)` eagerly causes the parent to adopt the child's modifier effect (polluting the parent and creating multi-owner DAGs). Eagerly calling `element()` / `table()` forces half-baked Arc widget instantiation before children blocks run.
  - By deferring effect creation and Arc widget configuration to component realization time (inside `Component.element()`), `OwnershipContext.push(this)` guarantees that the component itself is the active, sole owner of all its modifier effects.
  - Completely eliminates all manual `((Component) this).own(e)` casts across mixins.
- **Alternatives considered:** Calling `((Component) this).own(e)` and unregistering from parent. Rejected as brittle, complex, and prone to scope leaks.

### 4. Remove manual `.element()` in `solim.UI`
- **Decision:** Factory methods in `solim.UI` (`button()`, `image()`, `badge()`, `video()`, `card()`, `row()`, etc.) return the component without calling `AttachmentStack.attachToParent(x.element())`.
- **Rationale:** The `Component` constructor already invokes `AttachmentBridge.registerPending(this)`. When the containing scope's `AttachmentStack.pop()` runs, `attachPendingComponents()` resolves `comp.element()` once, attaches it to the parent Table, and configures its Cell.

### 5. Restrict `Component.own()` and `adopt()` visibility
- **Decision:** Change visibility of `own()` and `adopt()` in `Component` and `DisposableOwner` to package-private (or internal to `solim.core`).
- **Rationale:** Mod code and consumers must rely exclusively on ambient ownership (`OwnershipContext`) and declarative primitives (`effect()`, `listen()`), preventing lifecycle bugs and accidental leaks.

## Risks / Trade-offs

- **[Risk] Standalone component creation without an ambient parent table**: If someone instantiates `new Button()` outside any `AttachmentStack` scope and never calls `.element()`, the Arc element is never realized.
  - *Mitigation*: That is the exact definition of lazy declarative components! Root boundaries that mount views (e.g., Dialogs or standalone HUD roots) explicitly call `.element()` or use `View.mount()` / `OwnershipScope.build()`.
- **[Risk] Unit tests asserting on `.cellConfig().applyMinWidth(element)` directly**: Some legacy tests might expect `cellConfig()` to immediately modify `element`.
  - *Mitigation*: Tests should test observable behavior on the parent Table / Cell, which is what `PendingCellConfig` and `attachPendingComponents` govern.
