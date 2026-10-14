## MODIFIED Requirements

### Requirement: Reactive resources auto-own

A reactive primitive SHALL be registered with the component that configures it, without per-method bookkeeping or ad-hoc fallback adapter classes. Ownership of such a resource SHALL be automatic and unconditional via the active ambient `build()` scope.

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

#### Scenario: A component declares its reactive state in one place
- **WHEN** a component needs a reactive binding
- **THEN** it is created in `build()`, and the component is its owner for the component's whole lifetime

## REMOVED Requirements

### Requirement: Encapsulated ownership methods
**Reason**: Superseded by outright removal. `own()` is no longer hidden behind narrower visibility; it does not exist, and there is no renamed alias or package-private substitute. `adopt()` is retained as the framework-internal adoption primitive on `DisposableOwner` because `Component`, `SessionRoot`, and `OwnershipScope` implement it across `solim-api` and `solim-runtime`, and is guarded against use in application code.

### Requirement: Component is the ambient owner of its own construction
**Reason**: A component is not the ambient owner while its own constructor runs, so making it one was rejected in favour of confining all owned state to `build()`. Construction-time self-ownership would have left two windows in which a component can own resources, which is the ambiguity this change removes.
**Migration**: Move every constructor-created `Effect`, `Computed`, `TwoWayBinding`, and owned child component into `build()`. Constructors allocate plain values only.

## ADDED Requirements

### Requirement: No public ownership mutation API
`Component` SHALL NOT expose `own()`. Ownership of a resource SHALL be established only by the ambient `OwnershipContext`, never by a call written on the owning component. There SHALL be no replacement method, no renamed alias, and no package-private substitute.

#### Scenario: External code cannot manually own
- **WHEN** application code, mod code, or tests instantiate a `Component`
- **THEN** no ownership method can be invoked on it directly; resource binding happens through ambient context registration during `build()`

#### Scenario: Modifier methods register nothing by hand
- **WHEN** a modifier in `CellConfig`, `ElementConfig`, or `TableConfig` installs a reactive `Effect`
- **THEN** the modifier contains no self-registration call, because the owning component is already the active ambient owner

### Requirement: Reactive primitives are constructed only in build()
An `Effect` or a `Computed` SHALL only be constructed while a `Component` is executing its `build()`. Construction at any other site SHALL throw `IllegalStateException` rather than silently binding the primitive to whichever scope happens to be ambient. Because `Signal.map()` and `Readable.map()` return a `Computed`, deriving a value is likewise legal only in `build()`.

#### Scenario: Creating an Effect in a constructor fails loudly
- **WHEN** a component constructor calls `Effect.of(...)`
- **THEN** construction throws `IllegalStateException` identifying that reactive primitives must be created in `build()`

#### Scenario: Creating a Computed in a modifier fails loudly
- **WHEN** a component modifier derives a value with `.map(...)` outside `build()`
- **THEN** construction throws `IllegalStateException` rather than producing a `Computed` owned by an ambient scope

#### Scenario: Creation inside build() is the sanctioned path
- **WHEN** a component's `build()` creates an `Effect`, a `Computed`, or a `.map(...)` derivation
- **THEN** the component is the active ambient owner and adopts the primitive, with no manual registration

#### Scenario: The guard does not depend on the runtime module
- **WHEN** the `Effect` and `Computed` creation guards are evaluated
- **THEN** they read build-phase state exposed by `Component` itself, so neither `solim-core` reactive primitives nor `solim-api` depend on the internal `solim-runtime` module

### Requirement: Strict single ownership for all Disposables
A `Disposable` SHALL only ever be owned by at most one `DisposableOwner` at any given time. Multi-owner DAGs or concurrent adoptions of the same `Disposable` SHALL NOT be permitted.

#### Scenario: Attempting to adopt an already-owned Disposable
- **WHEN** a `Disposable` is already adopted by an active `DisposableOwner`
- **AND** another `DisposableOwner` attempts to adopt the same `Disposable`
- **THEN** the adoption SHALL either be rejected / disallow multiple concurrent owners or cleanly detach from the previous owner, ensuring the resource is disposed exactly once when its true owner is destroyed.
