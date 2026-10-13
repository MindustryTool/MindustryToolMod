## MODIFIED Requirements

### Requirement: Reactive resources auto-own

A reactive primitive SHALL be registered with the component that configures it, without per-method bookkeeping or ad-hoc fallback adapter classes. Ownership of such a resource SHALL be automatic and unconditional via active ambient build scope.

#### Scenario: A reactive modifier needs no self-registration
- **WHEN** a component configures itself from a `Readable`, for example `icon(icon).color(readable)`
- **THEN** the installed `Effect` is owned by that component automatically with no explicit `own(...)` or `ModifierOwnership` call at the call site

#### Scenario: Forgetting to own is not expressible
- **WHEN** a reactive modifier is added to a component
- **THEN** there is no per-method step that can be omitted, because ownership is inherited via ambient `build()` context rather than written manually

#### Scenario: A children block owns what it creates
- **WHEN** a component's `children(Runnable)` block runs
- **THEN** the component is the active ownership scope for the duration, so anything created inside is owned by it

#### Scenario: The component returned by a factory receives factory output
- **WHEN** a collection factory constructs resources around the component it returns
- **THEN** those resources are adopted by that component and disposed with it, whether or not the component type is known to the reconciler

## ADDED Requirements

### Requirement: Encapsulated ownership methods
`Component` SHALL NOT expose `own()` or `adopt()` to external modules or consumers as public mutable APIs. Ownership adoption SHALL be internal to the Solim lifecycle framework.

#### Scenario: External code cannot manually adopt or own
- **WHEN** application code or external callers instantiate a `Component`
- **THEN** they cannot call `own()` or `adopt()` directly on the component; resource binding happens via ambient context registration.

### Requirement: Strict single ownership for all Disposables
A `Disposable` SHALL only ever be owned by at most one `DisposableOwner` at any given time. Multi-owner DAGs or concurrent adoptions of the same `Disposable` SHALL NOT be permitted.

#### Scenario: Attempting to adopt an already-owned Disposable
- **WHEN** a `Disposable` is already adopted by an active `DisposableOwner`
- **AND** another `DisposableOwner` attempts to adopt the same `Disposable`
- **THEN** the adoption SHALL either be rejected / disallow multiple concurrent owners or cleanly detach from the previous owner, ensuring the resource is disposed exactly once when its true owner is destroyed.
