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

The system SHALL provide `solim.core.OwnershipContext` as the single ambient scope answering WHO disposes a `Disposable`, with `push/pop/current/register/registerChild` semantics. `OwnershipContext` SHALL reside in the same module as `Component` so a component can install itself as owner. It SHALL NOT filter registrations by type: every registered `Disposable` is collected by the active scope. Suspension of ambient collection SHALL be expressed by `OwnershipScope`, not by clearing the stack.

#### Scenario: Build-time resources are owned automatically

- **WHEN** an `Effect`, `Binding`, `Computed`, or child `Component` is created inside a component's build
- **THEN** it is registered with that component and disposed with it

#### Scenario: Non-Component resources are no longer filtered out of captures

- **WHEN** a capture or factory scope collects resources
- **THEN** `Computed`, `Effect`, `Query`, and `Subscription` instances are collected alongside components, rather than being dropped

#### Scenario: Reconciler output is owned by the reconciled component

- **WHEN** a keyed collection creates a component through a factory that also constructs a `Computed`
- **THEN** both the component and the `Computed` are owned by the created component, and neither is attached to the component whose collection triggered reconciliation

### Requirement: DebugAttachmentStack owns all attachment profiling state

The system SHALL provide `solim.runtime.DebugAttachmentStack` as the profiling-enabled `AttachmentStack` variant owning all slow-span and flame-trace state, with the base `AttachmentStack` containing zero tracing branches and static entry points delegating to the installed singleton.

#### Scenario: Profiling disabled costs nothing

- **WHEN** tracing/profiling is disabled
- **THEN** the active singleton is the base `AttachmentStack` and no trace buffer is allocated

#### Scenario: Slow-span label uses the new name

- **WHEN** a container subtree exceeds the slow threshold
- **THEN** the recorded `PerfSpan` component label is `AttachmentStack`

