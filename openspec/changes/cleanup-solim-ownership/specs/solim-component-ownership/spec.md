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

### Requirement: Single base class

`Component` SHALL be an abstract class, not an interface, and SHALL carry every behaviour shared by all components: the parent link, the child list, the flat leaf list, resource adoption, and fail-fast disposal ordering without swallowing exceptions.

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
