## MODIFIED Requirements

### Requirement: Memory-safe ephemeral unregistration

`SessionRoot` SHALL allow top-level components to be unlinked upon disposal or overlay closure, preventing process-lifetime retention leaks, and fail fast without swallowing disposal exceptions.

#### Scenario: Disposed top-level component unregisters from SessionRoot
- **WHEN** a top-level component adopted by `SessionRoot` is disposed
- **THEN** it is removed from `SessionRoot.children()`, leaving no retained strong references in the root

#### Scenario: SessionRoot disposal fails loud
- **WHEN** `SessionRoot.dispose()` encounters an exception while disposing a child or resource
- **THEN** the exception is propagated immediately instead of caught and ignored

## REMOVED Requirements

### Requirement: Node ownership resolution assertion
**Reason**: `SolimAssert.assertOwned()` is a test-only utility checking internal implementation details rather than production framework assertions. Production code relies directly on `OwnershipContext` and `SessionRoot`.
**Migration**: Tests assert component lifecycle and observable reactive updates directly.
