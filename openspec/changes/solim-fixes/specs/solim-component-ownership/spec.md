## ADDED Requirements

### Requirement: Component re-parenting is explicit and detaches the Arc element
A `Component` SHALL expose an explicit re-parenting operation that moves it between owners in the ownership tree and detaches its built Arc `Element` from any live parent `Table` before the move. Implicit adoption SHALL route through this operation so that an already-owned component passed to an attachment helper cannot leave its `Element` orphaned in a previous container. Detachment SHALL fail fast: an Arc-side failure to detach SHALL propagate to the caller rather than being swallowed, leaving a component registered with a new owner while its element remains in an old table.

#### Scenario: Re-parenting moves ownership and detaches the element
- **WHEN** a built component with a live parent element is re-parented to a new owner
- **THEN** it is removed from the previous owner's child list, added to the new owner's child list, and its element is removed from the previous parent table

#### Scenario: Previous owner no longer disposes the component
- **WHEN** a component has been re-parented and the previous owner is disposed
- **THEN** the component is not disposed by that previous owner

#### Scenario: New owner disposes the component
- **WHEN** a re-parented component is disposed by its new owner
- **THEN** its element is not left attached to a container the previous owner still owns

#### Scenario: Re-parenting to null detaches without a new owner
- **WHEN** a component is re-parented to no owner
- **THEN** it is removed from its current owner's child list and its element is detached, and the component is eligible for disposal by its caller

#### Scenario: Implicit adoption of an owned component is safe
- **WHEN** an already-owned component is passed to a declarative attachment helper
- **THEN** the helper performs the explicit re-parenting rather than silently re-registering it while leaving its element in place

### Requirement: Modifier bindings are owned by the component they configure
A reactive binding installed by a fluent modifier (size, position, visibility, opacity, margin, padding, gap, border, background, and the equivalent cell and table modifiers) SHALL be owned by the component the modifier was invoked on, regardless of which ambient ownership scope happened to be active at the call site. Ownership SHALL NOT vary with the calling context.

#### Scenario: Modifier binding lifetime follows its own component
- **WHEN** a component's size is bound to a reactive value and that component is disposed
- **THEN** the binding effect is disposed with the component and no further updates are applied

#### Scenario: Parent disposal does not outlive the child
- **WHEN** a component configured with a reactive modifier is created inside a parent's build and the child is disposed before the parent
- **THEN** the binding effect is disposed at that point and is not retained until parent disposal

#### Scenario: Parent disposal still disposes a live child
- **WHEN** a component configured with a reactive modifier is created inside a parent's build and the parent is disposed without the child being disposed first
- **THEN** the binding effect is disposed through the child's own disposal during the parent's cascade

#### Scenario: Modifier invoked outside any build scope is still owned
- **WHEN** a reactive modifier is invoked on a component with no ambient ownership scope
- **THEN** the binding effect is owned by that component

### Requirement: Ownership adoption is idempotent and order-preserving
Adopting a resource into an owner SHALL be idempotent: adopting the same resource instance more than once SHALL register it exactly once. Disposal SHALL still occur in reverse registration order. The idempotence check SHALL be based on instance identity rather than value equality.

#### Scenario: Double adoption registers once
- **WHEN** the same resource instance is adopted into the same owner twice
- **THEN** it appears once in the owner's resource list and is disposed once

#### Scenario: Disposal order is reverse registration
- **WHEN** three resources are adopted in order A, B, C and the owner is disposed
- **THEN** they are disposed in the order C, B, A

#### Scenario: Identity, not equality, distinguishes resources
- **WHEN** two distinct resource instances are `equals` to each other and both are adopted
- **THEN** both are registered and both are disposed

## REMOVED Requirements

### Requirement: Modifier ownership is decided by the ambient scope
**Reason**: Superseded by "Modifier bindings are owned by the component they configure". `ModifierOwnership` decided ownership from `OwnershipContext.current()`, which made the lifetime of a modifier-created binding depend on invisible ambient state at the call site. This requirement is replaced by deterministic, component-local ownership.