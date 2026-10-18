# solim-component-ownership Specification

## Purpose
TBD - created by archiving change solim-ownership-unification. Update Purpose after archive.
## Requirements
### Requirement: Single base class

`Component` SHALL be an abstract class, not an interface, and SHALL carry every behaviour shared by all components: the parent link, the child list, the flat leaf list, resource adoption, and disposal ordering.

#### Scenario: A component cannot silently skip disposal
- **WHEN** a type extends `Component` without implementing disposal
- **THEN** it SHALL be a compile error; the class SHALL NOT inherit a no-op `dispose()`

#### Scenario: isDisposed reports the real state
- **WHEN** any component is asked whether it is disposed
- **THEN** it SHALL report its actual state, never an unconditional `false`

#### Scenario: Disposal order is on the base class and fails loud
- **WHEN** a component with children and own leaves is disposed
- **THEN** `onDispose()` runs first, then child components in reverse adoption order, then own leaves in reverse registration order, and any exception thrown during disposal SHALL propagate immediately without being swallowed or merely logged

#### Scenario: Owner and ancestry are queryable
- **WHEN** a component is queried for its owner, its children, or whether it is an ancestor of another component
- **THEN** the answer reflects the adoption tree that was recorded at runtime

### Requirement: Two element strategies, one class

`Component` SHALL support both an eagerly-constructed element and a lazily-built one through its constructor, and SHALL NOT require a second base class to distinguish them. `element()` SHALL be `final`.

#### Scenario: Eager components skip the build path
- **WHEN** a component is constructed with an existing element
- **THEN** `element()` returns it immediately without running `build()` and without opening an ownership scope

#### Scenario: Lazy components build exactly once
- **WHEN** `element()` is called on a component constructed without an element
- **THEN** `build()` runs once, its result is cached, and later calls return the cache without rebuilding

#### Scenario: A build that produces no element fails loudly
- **WHEN** `build()` leaves the element unset
- **THEN** `element()` throws `IllegalStateException` rather than returning null

### Requirement: Reactive resources auto-own

A reactive primitive SHALL be registered with the component that configures it, without per-method bookkeeping or ad-hoc fallback adapter classes. Ownership of such a resource SHALL be automatic and unconditional via the active ambient scope, whether that scope is `build()`, a `children()` block, or property application.

#### Scenario: A reactive modifier needs no self-registration

- **WHEN** a component configures itself from a `Readable`, for example `icon(icon).color(readable)`
- **THEN** the installed `Effect` is owned by that component automatically with no explicit `own(...)` or `ModifierOwnership` call at the call site

#### Scenario: Forgetting to own is not expressible

- **WHEN** a reactive modifier is added to a component
- **THEN** there is no per-method step that can be omitted, because ownership is inherited via the ambient scope rather than written manually

#### Scenario: A children block owns what it creates

- **WHEN** a component's `children(Runnable)` block runs
- **THEN** the component is the active ownership scope for the duration, so anything created inside is owned by it

#### Scenario: A property-created binding needs no self-registration

- **WHEN** a reactive property is applied to a host, either immediately or during realization
- **THEN** the binding it installs is owned by that host automatically, with no `withinOwnership`, `openSelfWindow`, or manual `OwnershipContext.push` involved

#### Scenario: The component returned by a factory receives factory output

- **WHEN** a collection factory constructs resources around the component it returns
- **THEN** those resources are adopted by that component and disposed with it, whether or not the component type is known to the reconciler

#### Scenario: A component declares its reactive state in one place

- **WHEN** a component needs a reactive binding
- **THEN** it is created with the component as the active ambient owner, and the component is its owner for the component's whole lifetime

### Requirement: No closures in the ownership path

The ambient ownership stack SHALL hold components directly. Registering an owner SHALL NOT allocate a callback.

#### Scenario: Registering an owner allocates nothing
- **WHEN** a component becomes the active ownership scope
- **THEN** no callback object is created

#### Scenario: Ownership is a single mechanism
- **WHEN** any component, fragment, or scope becomes an owner
- **THEN** it does so through the same adoption method, with no per-type variant

### Requirement: Ambient ownership crosses the module boundary

`OwnershipContext` and the main-thread assertion SHALL live in the same module as `Component`, because a component installs itself as owner when it builds.

#### Scenario: A component can install itself as owner
- **WHEN** a component's `build()` runs
- **THEN** it can push itself onto the ambient ownership stack without a module cycle

#### Scenario: Downstream modules still see one ownership type
- **WHEN** a module below `solim-core` references a component as a type
- **THEN** the type remains available from the contract module

### Requirement: Factory and collection cell bindings are fully owned

Collections and dynamic factories SHALL own any reactive cell constraints and temporary empty state components they construct, preventing orphaned bindings upon item removal or data changes.

#### Scenario: ForEach cell constraint effects are disposed with the item
- **WHEN** a `ForEach` item with reactive cell constraints is reconciled and subsequently removed
- **THEN** the effects created by `applyToCell` are disposed alongside the item component

#### Scenario: ReactiveGrid empty state temporary bindings are disposed
- **WHEN** a `ReactiveGrid` transitions from empty to populated
- **THEN** any bindings and components created by `empty(Runnable)` or `empty(Prov)` are disposed

### Requirement: No public ownership mutation API

`Component` SHALL NOT expose `own()`, `withinOwnership(...)`, `openSelfWindow()`, or `closeSelfWindow()`. Ownership of a resource SHALL be established only by the ambient `OwnershipContext`, never by a call written on the owning component. There SHALL be no replacement method, no renamed alias, and no package-private substitute.

#### Scenario: External code cannot manually own

- **WHEN** application code, mod code, or tests instantiate a `Component`
- **THEN** no ownership method and no ownership window can be invoked on it directly; resource binding happens through ambient context registration during `build()`, during property application, or through a self-adopting primitive such as `listen(...)`

#### Scenario: Modifier methods register nothing by hand

- **WHEN** a modifier in `CellConfig`, `ElementConfig`, or `TableConfig` installs a reactive `Effect`
- **THEN** the modifier contains no self-registration call, because the owning component is already the active ambient owner

#### Scenario: A primitive that must own adopts to its host

- **WHEN** framework code needs a resource to belong to the component it is called on while no ambient window is active
- **THEN** the primitive adopts the resource to that component directly instead of pushing an ownership window

#### Scenario: Framework internals use the registration path, not a window

- **WHEN** framework code outside a component method needs a resource owned during a live cell configuration
- **THEN** it uses `OwnershipContext.push`/`pop` internals rather than a window method exposed on `Component`

### Requirement: Strict single ownership for all Disposables
A `Disposable` SHALL only ever be owned by at most one `DisposableOwner` at a time. Adopting a `Disposable` already held by a live owner SHALL throw `IllegalStateException` rather than silently re-owning it. Ownership SHALL be detectable without global mutable state, so that no registration can outlive its owner.

#### Scenario: Adopting an already-owned Disposable is rejected
- **WHEN** a `Disposable` is already adopted by a live owner reachable from `SessionRoot`
- **AND** another owner attempts to adopt the same `Disposable`
- **THEN** adoption throws `IllegalStateException` naming both owners and the resource, and the resource stays with its original owner

#### Scenario: A collector is not treated as an owner
- **WHEN** a resource is created inside an `OwnershipScope` and later moved onto a component
- **THEN** the move is not a violation, because an `OwnershipScope` is a transient collector, is not reachable as a live owner, and has already relinquished the resource

#### Scenario: A reactive resource is not registered with two owners
- **WHEN** a reconciler produces a binding for a reconciled item
- **THEN** the binding is adopted by exactly one component, rather than being registered in both a reconciler-owned binding list and the item component

#### Scenario: Non-component helpers do not allocate reactive primitives
- **WHEN** a static helper outside the component model would return a `Computed`
- **THEN** it is re-expressed as a plain value helper, so no reactive primitive is allocated by a call site that has no owner to adopt it

