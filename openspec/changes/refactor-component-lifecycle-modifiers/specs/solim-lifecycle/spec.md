## MODIFIED Requirements

### Requirement: Component as child via ElementResolver
Layouts and parents SHALL accept children that are `Element` or `Component`; `Component` children SHALL be automatically resolved to `component.element()` without requiring callers to write `new Header().element()`. Components instantiated inside a `children()` block SHALL be automatically attached to the current parent via the pending attachment mechanism, and a component is never moved between ownership contexts to make it attach.

#### Scenario: Direct Component child without component() call
- **WHEN** `column(() -> { new Header(); new ChatPanel(); })` where `Header` and `ChatPanel` extend `Component`
- **THEN** parent column contains their resolved `Element`s in order, without any `component()` call

#### Scenario: Mixed Element and Component children
- **WHEN** `column(() -> { text("Hello"); new UserCard(user); image(tex); })`
- **THEN** all three children are attached correctly without requiring manual `.element()` calls

#### Scenario: Explicit component() still valid
- **WHEN** `column(() -> { component(new Header()); })`
- **THEN** the element is attached exactly once; no duplicate child is added

### Requirement: Ambient Lifecycle Ownership in Component Build
The Solim framework SHALL automatically track child components, disposables, and Solim controls instantiated or attached during a component's `build()` execution without requiring explicit ownership invocations. Ambient ownership SHALL be the only ownership path for application code and framework code alike. A component's constructor SHALL allocate plain values only, because the component is not the active ambient owner while its own constructor runs.

#### Scenario: Child component created in build context
- **WHEN** a component instantiates child components or Solim controls within its `build()` method
- **THEN** the child components and controls are automatically registered with the parent component's disposable registry and disposed when the parent is disposed

#### Scenario: Constructor allocates plain values only
- **WHEN** a component's constructor body runs
- **THEN** the component is not the active ambient owner, so any owned resource it would create belongs to that resource's own construction site rather than the component

#### Scenario: Clean stack unwinding on build failure
- **WHEN** a component's `build()` method throws an exception during construction
- **THEN** the ambient build context stack is cleanly unwound and does not leak context to subsequent component builds

#### Scenario: No manual ownership call exists to misuse
- **WHEN** framework or application code needs to bind a resource to the current component
- **THEN** no ownership method can be called on the component, because ownership is expressed solely by the ambient context the resource is created in

### Requirement: Legacy ownership APIs are removed or deprecated
`registerDisposable(...)`, `ownChild(...)`, `own(...)`, and any other ownership alias method SHALL be removed outright. There SHALL be no deprecation shim and no replacement method.

#### Scenario: No ownership method resolves
- **WHEN** any module attempts to call `own(...)`, `ownChild(...)`, or `registerDisposable(...)`
- **THEN** compilation fails with a missing symbol, and the author binds the resource by creating it inside the component's `build()` scope

#### Scenario: No deprecation warning stands in for removal
- **WHEN** code calls a legacy ownership alias
- **THEN** no deprecation shim remains that lets the call compile and behave like `own()`

## REMOVED Requirements

### Requirement: Internal-only ownership primitive
**Reason**: Superseded by outright removal. Hiding `own()` behind package access was an intermediate step; the method is now deleted entirely, so there is no visibility level left to confine.
**Migration**: Bind resources by creating them inside a component's `build()`, where the component is already the active ambient owner.

### Requirement: Single own() method on BaseComponent
**Reason**: This requirement mandates the existence of `own(T extends Disposable)` and its return-the-disposable contract, which directly contradicts the removal of `own()`.
**Migration**: Use `Effect.of(...)` or `OwnershipContext.register(...)` inside the component's `build()`; both return the created resource, so field assignment still works without an ownership wrapper.

## ADDED Requirements

### Requirement: Single lazy realization upon parent attachment
Component `element()` SHALL be invoked lazily when the component is attached to its parent container by `AttachmentStack.pop()`. Factory methods in `solim.UI` SHALL NOT eagerly realize `element()`; `AttachmentStack.attachPendingComponents()` SHALL be the sole mechanism that resolves a pending component's element.

#### Scenario: Factory chaining before realization
- **WHEN** `button("Click").minWidth(100f).size(40f).padding(unit(2))` is called inside a `children()` block
- **THEN** `element()` is not invoked by the factory while the parent `children()` scope is still open; it is resolved when that scope closes and attaches its pending children

#### Scenario: Element realized exactly once
- **WHEN** a component is realized during attachment and `element()` is requested again later
- **THEN** the cached element is returned and `build()` is not invoked a second time

### Requirement: Immutable component ownership and no reparenting
`Component` SHALL NOT provide a `reparentTo` method. A component's lifecycle and ownership SHALL remain immutable once instantiated. When a new parent requires a component structure, a new component instance SHALL be instantiated.

#### Scenario: Components do not mutate parent ownership
- **WHEN** a component is created in an ownership scope
- **THEN** it cannot be reassigned or adopted into another owner via `reparentTo`.

#### Scenario: Element resolution does not reparent
- **WHEN** `ElementResolver` resolves a `Component` child to an `Element`
- **THEN** it resolves the element without moving the component to a different owner.

### Requirement: Build ownership frame is unwound deterministically
A component SHALL install itself as the active ambient owner for exactly the duration of its `build()` and SHALL unwind that frame deterministically when `build()` returns or throws, so the enclosing scope is restored and the `OwnershipContext` LIFO invariant holds.

#### Scenario: Scope teardown stays balanced
- **WHEN** a `build()` returns
- **THEN** the enclosing owner is the active ambient owner again and the stack is not left holding an orphan frame

#### Scenario: A throwing build still unwinds
- **WHEN** a `build()` throws
- **THEN** the ambient frame and the build-phase marker are both cleared, and the exception propagates to the caller

#### Scenario: The component is the owner only during build
- **WHEN** a resource is created before or after the `build()` window for that component
- **THEN** the component does not claim ownership of it, so the resource belongs to the scope that was actually ambient at creation time
