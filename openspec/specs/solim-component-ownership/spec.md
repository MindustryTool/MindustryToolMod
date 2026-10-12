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

#### Scenario: Disposal order is on the base class
- **WHEN** a component with children and own leaves is disposed
- **THEN** `onDispose()` runs first, then child components in reverse adoption order, then own leaves in reverse registration order, and each disposal is attempted independently

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

A reactive primitive SHALL be registered with the component that configures it, without per-method bookkeeping. Ownership of such a resource SHALL be automatic and unconditional.

#### Scenario: A reactive modifier needs no self-registration
- **WHEN** a component configures itself from a `Readable`, for example `icon(icon).color(readable)`
- **THEN** the installed `Effect` is owned by that component with no explicit registration call at the call site

#### Scenario: Forgetting to own is not expressible
- **WHEN** a reactive modifier is added to a component
- **THEN** there is no per-method step that can be omitted, because ownership is inherited rather than written

#### Scenario: A children block owns what it creates
- **WHEN** a component's `children(Runnable)` block runs
- **THEN** the component is the active ownership scope for the duration, so anything created inside is owned by it

#### Scenario: The component returned by a factory receives factory output
- **WHEN** a collection factory constructs resources around the component it returns
- **THEN** those resources are adopted by that component and disposed with it, whether or not the component type is known to the reconciler

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

