# Ambient Naming

## Purpose

Defines canonical naming and responsibilities for Solim's visual attachment stack (`AttachmentStack`) and lifecycle ownership scope (`OwnershipContext`), replacing legacy `ParentStack` and `ComponentContext`.

## Requirements

### Requirement: AttachmentStack is the canonical visual-attachment stack

The system SHALL provide `solim.runtime.AttachmentStack` as the single ambient stack answering WHERE a child `Element` attaches, owning the `Table` + `Attacher` + pending-components state with `push/pop/current/isolate/capture/attachToParent` semantics identical to the former `ParentStack`.

#### Scenario: Containers attach through AttachmentStack

- **WHEN** a layout container executes its `children(Runnable)` block
- **THEN** child elements attach to the current `AttachmentStack` parent via its `Attacher`, and the stack is empty at test teardown

#### Scenario: Old ParentStack name no longer exists

- **WHEN** the codebase is searched for `ParentStack` outside archived change history and the legacy `old/` folder
- **THEN** no references remain (types, imports, messages, docs, or active specs)

### Requirement: OwnershipContext is the canonical lifecycle-ownership scope

The system SHALL provide `solim.runtime.OwnershipContext` as the single ambient scope answering WHO disposes a `Disposable`, with `push/pop/current/register/registerChild` semantics identical to the former `ComponentContext`. `OwnershipContext` SHALL NOT filter registrations by type: every registered `Disposable` is collected by the active scope, and capture filtering to `Component` roots only is removed. Suspension of ambient collection SHALL be expressed by `OwnershipScope`, not by clearing the `OwnershipContext` stack.

#### Scenario: Build-time resources are owned automatically

- **WHEN** an `Effect`, `Binding`, `Computed`, or child `Component` is created inside `BaseComponent.element()`
- **THEN** it is registered with the active `OwnershipContext` owner and disposed with that owner

#### Scenario: Non-Component resources are no longer filtered out of captures

- **WHEN** a capture or factory scope collects resources
- **THEN** `Computed`, `Effect`, `Query`, and `Subscription` instances are collected alongside `Component` instances, rather than being dropped

#### Scenario: Reconciler output is owned by the reconciled component

- **WHEN** `StructuralReconciler` creates a new keyed component through a factory that also constructs a `Computed`
- **THEN** both the component and the `Computed` are owned by the reconciled component's ownership node, and neither is attached to the ambient component whose collection triggered reconciliation

### Requirement: DebugAttachmentStack owns all attachment profiling state

The system SHALL provide `solim.runtime.DebugAttachmentStack` as the profiling-enabled `AttachmentStack` variant owning all slow-span and flame-trace state, with the base `AttachmentStack` containing zero tracing branches and static entry points delegating to the installed singleton.

#### Scenario: Profiling disabled costs nothing

- **WHEN** tracing/profiling is disabled
- **THEN** the active singleton is the base `AttachmentStack` and no trace buffer is allocated

#### Scenario: Slow-span label uses the new name

- **WHEN** a container subtree exceeds the slow threshold
- **THEN** the recorded `PerfSpan` component label is `AttachmentStack`
