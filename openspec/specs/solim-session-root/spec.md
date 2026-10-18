# solim-session-root Specification

## Purpose
Provides a permanent, memory-safe process-level `SessionRoot` as a fallback `DisposableOwner` for unparented components and establishes ownership resolution verification.

## Requirements

### Requirement: SessionRoot provides top-level process ownership

The system SHALL provide `SessionRoot` as a process-lifetime `DisposableOwner` registered with `OwnershipContext` when no ambient component is building, ensuring every unparented component has a valid queryable root owner.

#### Scenario: Unparented top-level component resolves to SessionRoot
- **WHEN** a component is constructed outside any active container or scope
- **THEN** its owner resolves to `SessionRoot`, and `isAncestorOf` confirms root lineage

### Requirement: Memory-safe ephemeral unregistration

SessionRoot SHALL allow top-level components to be unlinked upon disposal or overlay closure, preventing process-lifetime retention leaks, and fail fast without swallowing disposal exceptions.

#### Scenario: Disposed top-level component unregisters from SessionRoot
- **WHEN** a top-level component adopted by SessionRoot is disposed
- **THEN** it is removed from SessionRoot.children(), leaving no retained strong references in the root

#### Scenario: SessionRoot disposal fails loud
- **WHEN** SessionRoot.dispose() encounters an exception while disposing a child or resource
- **THEN** the exception is propagated immediately instead of caught and ignored
