# solim-ownership-tree Specification

## Purpose

Tree-shaped resource ownership for Solim components: scope-based adoption of factory-created resources, component nodes with parent links, a permanent session root so no node is parentless, and a declared disposal order of `onDispose()`, then children, then own non-component leaves. Created by archiving change solim-reactivity-hardening.

## Requirements

**Source: solim-reactivity-hardening**

### Requirement: OwnershipScope collects and adopts disposable resources
The runtime SHALL provide `solim.runtime.OwnershipScope` with `open()`, `adoptInto(Disposable owner)`, and `close()` semantics. An open scope SHALL collect every `Disposable` registered through `OwnershipContext` while the scope is active, regardless of whether that resource is a `Component`. `adoptInto` SHALL transfer all collected resources to the given owner's disposal list and mark them adopted. `close()` SHALL dispose every collected resource that was not adopted.

#### Scenario: Scope collects non-Component resources
- **WHEN** a scope is open and a `Computed`, `Effect`, or `Query` is registered through `OwnershipContext`
- **THEN** the scope collects it, whether or not it is a `Component`

#### Scenario: Adopted resources survive close
- **WHEN** a scope collects resources and `adoptInto(owner)` is called before `close()`
- **THEN** `close()` disposes nothing and the owner now holds every collected resource

#### Scenario: Unadopted resources are destroyed on close
- **WHEN** a scope collects resources that are never adopted and `close()` is called
- **THEN** each collected resource is disposed exactly once

#### Scenario: Scope disposes partially created resources when the block throws
- **WHEN** a scope is open, resources are registered, the enclosing block throws, and `close()` runs in a `finally`
- **THEN** every collected resource is disposed before the exception propagates

### Requirement: Factory-created resources are adopted rather than discarded
`StructuralReconciler` and `Fragment.capture` SHALL route factory or branch execution through an `OwnershipScope` instead of suspending auto-ownership by clearing the `OwnershipContext` stack. Resources registered during a factory invocation SHALL be adopted into the component that factory returns, so they are disposed with that component.

#### Scenario: Item factory resources are owned by the created component
- **WHEN** an item factory passed to `reactiveGrid` or `forEach` constructs a `Computed` and returns a component
- **THEN** the component is disposed when the reconciler removes that key, and the `Computed` is disposed at the same time and detached from its sources

#### Scenario: Conditional branch resources are owned by the branch component
- **WHEN** a `thenDo` or `elseDo` branch passed to `when` constructs a `Computed` and returns a component
- **THEN** the branch's `Computed` is disposed when the condition switches and the branch is rebuilt

#### Scenario: Reconcile is no longer owned by the ambient enclosing component
- **WHEN** a factory creates resources during reconciliation
- **THEN** those resources are attached to the created component, not to the component whose collection was being reconciled

### Requirement: Components are ownership nodes with parent links
Each `Component` SHALL act as a node in a tree, holding a reference to the node that adopted it. Non-component disposables SHALL remain leaves held in the node's existing flat disposal list, and `OwnershipContext.register` SHALL preserve its current append-and-return behaviour with no additional allocation. A permanent session root SHALL exist so that no node is ever parentless.

#### Scenario: register() allocates nothing new
- **WHEN** a `Disposable` is registered through `OwnershipContext.register` on an active node
- **THEN** it is appended to that node's existing flat disposal list and returned, with no wrapper object and no intervening tree node

#### Scenario: Adopted components record their parent
- **WHEN** a component is adopted into an owning component
- **THEN** the child exposes a reference to the parent, and the parent exposes the child among its children

#### Scenario: Session root guarantees no orphan
- **WHEN** a resource is registered with no active component scope
- **THEN** it is attached to the session root rather than being left parentless

#### Scenario: Ownership tree is navigable
- **WHEN** a resource or component is queried for its owner
- **THEN** its owning node and its ancestors are reachable, and a component can be tested for whether it is an ancestor of another component

### Requirement: Declared disposal ordering
`BaseComponent.dispose()` SHALL run `onDispose()` first, then dispose child components, then dispose its own non-component resources. Each disposal SHALL be attempted independently so that a failure in one does not prevent the rest.

#### Scenario: onDispose runs before children
- **WHEN** a component with children is disposed
- **THEN** the `onDispose()` hook executes while the child tree is still intact

#### Scenario: Children dispose before own resources
- **WHEN** a component owns a child component and a `Computed` that reads from that child, and the component is disposed
- **THEN** the child is disposed first, so the `Computed` cannot recompute against a disposed element

#### Scenario: Subclass fragment disposal remains idempotent
- **WHEN** `Dynamic` or `When` disposes its current fragment from `onDispose()` and its children pass also disposes that fragment
- **THEN** disposal completes without error and the fragment is disposed exactly once

#### Scenario: Failing child does not block remaining disposal
- **WHEN** one child component throws during disposal
- **THEN** the error is logged via `Log.err` and all remaining children and own resources are still disposed

### Requirement: Components own their own constructor-time reactive state
A component SHALL NOT declare `Computed`, `Query`, or other auto-registering reactive primitives in field initialisers. Reactive state belonging to a component SHALL be constructed inside `build()` so that it is registered against that component rather than against whichever component happened to construct it. Long-lived non-component state objects SHALL continue to construct reactive state in their constructors.

#### Scenario: View reactive state is owned by the view
- **WHEN** a `BaseComponent` declares derived values inside `build()` and is constructed inside another component's build scope
- **THEN** those derived values are owned by the view itself, not by the enclosing component

#### Scenario: Session-scoped state objects are unaffected
- **WHEN** a non-component state class constructs a `Computed` in its constructor during mod initialisation
- **THEN** the `Computed` is attached to the session root and lives for the session, which is its intended lifetime
