## ADDED Requirements

### Requirement: AttachmentStack is the canonical visual-attachment stack

The system SHALL provide `solim.runtime.AttachmentStack` as the single ambient stack answering WHERE a child `Element` attaches, owning the `Table` + `Attacher` + pending-components state with `push/pop/current/isolate/capture/attachToParent` semantics identical to the former `ParentStack`.

#### Scenario: Containers attach through AttachmentStack

- **WHEN** a layout container executes its `children(Runnable)` block
- **THEN** child elements attach to the current `AttachmentStack` parent via its `Attacher`, and the stack is empty at test teardown

#### Scenario: Old ParentStack name no longer exists

- **WHEN** the codebase is searched for `ParentStack` outside archived change history and the legacy `old/` folder
- **THEN** no references remain (types, imports, messages, docs, or active specs)

### Requirement: OwnershipContext is the canonical lifecycle-ownership scope

The system SHALL provide `solim.runtime.OwnershipContext` as the single ambient scope answering WHO disposes a `Disposable`, with `push/pop/current/register/registerChild/withoutAutoOwnership` semantics identical to the former `ComponentContext`, including capture filtering to `Component` roots only.

#### Scenario: Build-time resources are owned automatically

- **WHEN** an `Effect`, `Binding`, or child `Component` is created inside `BaseComponent.element()`
- **THEN** it is registered with the active `OwnershipContext` owner and disposed LIFO with that owner

#### Scenario: Reconciler orphans are independent of the ambient owner

- **WHEN** `StructuralReconciler` creates a new keyed component inside `OwnershipContext.withoutAutoOwnership` + `AttachmentStack.isolate`
- **THEN** the new component is neither attached to the ambient parent nor owned by the ambient component

### Requirement: DebugAttachmentStack owns all attachment profiling state

The system SHALL provide `solim.runtime.DebugAttachmentStack` as the profiling-enabled `AttachmentStack` variant owning all slow-span and flame-trace state, with the base `AttachmentStack` containing zero tracing branches and static entry points delegating to the installed singleton.

#### Scenario: Profiling disabled costs nothing

- **WHEN** tracing/profiling is disabled
- **THEN** the active singleton is the base `AttachmentStack` and no trace buffer is allocated

#### Scenario: Slow-span label uses the new name

- **WHEN** a container subtree exceeds the slow threshold
- **THEN** the recorded `PerfSpan` component label is `AttachmentStack`
