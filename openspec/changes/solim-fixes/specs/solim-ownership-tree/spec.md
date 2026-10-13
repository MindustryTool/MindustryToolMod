## ADDED Requirements

### Requirement: Ownership and attachment stacks are restored by depth truncation
Both the reactive tracking stack and the attachment stack SHALL restore themselves to their entry depth on exit from a suspended block by truncating to the recorded depth, discarding any entries the block pushed without popping. Neither SHALL achieve restoration by copying or snapshotting the stack. Restoration cost SHALL be proportional to the entries pushed by the block, not to the depth at which it was entered. Both stacks SHALL assert main-thread execution on mutation.

#### Scenario: Unbalanced push is discarded on exit
- **WHEN** a suspended block pushes an entry and does not pop it
- **THEN** after the block returns, the stack depth equals the depth recorded on entry and the leaked entry is discarded

#### Scenario: Balanced block leaves the stack unchanged
- **WHEN** a suspended block pushes and pops the same entries
- **THEN** the surrounding stack is exactly as it was before the call

#### Scenario: Restoration is independent of entry depth
- **WHEN** a suspended block at deep stack nesting pushes one entry and pops it
- **THEN** the work performed on exit is proportional to the entries the block pushed, not to the pre-existing depth

#### Scenario: Nested suspensions restore correctly
- **WHEN** a suspended block is entered from within another suspended block
- **THEN** each block restores to its own recorded depth on its own exit, in reverse order

#### Scenario: Main-thread execution is enforced on push and pop
- **WHEN** a stack push or pop is invoked from a thread other than the registered main thread
- **THEN** an `IllegalStateException` is raised

### Requirement: Ownership scope push and pop are balanced
The ownership stack SHALL verify that each pop removes the entry most recently pushed. In debug builds, a pop that would remove a different entry than the one most recently pushed SHALL raise an `IllegalStateException` rather than silently corrupting ownership for subsequent registrations.

#### Scenario: Balanced push and pop is accepted
- **WHEN** an owner is pushed and subsequently popped in the same nesting order
- **THEN** the pop returns that owner and no error is raised

#### Scenario: Out-of-order pop is rejected in debug builds
- **WHEN** two owners are pushed and they are popped in the reverse order
- **THEN** an `IllegalStateException` identifying the mismatch is raised

#### Scenario: Pop on an empty stack is tolerated
- **WHEN** a pop is performed with no owner pushed
- **THEN** it returns no owner and raises no error

### Requirement: Orphaned components are attributed to the session root
A component constructed with no ambient ownership scope SHALL be adopted by the session root so that it remains reachable for disposal, and SHALL be removed from that root when it is later adopted by a real owner or disposed.

#### Scenario: Scopeless component is reachable
- **WHEN** a component is constructed with no active ownership scope
- **THEN** the session root holds a reference to it

#### Scenario: Adoption by a real owner releases the session root
- **WHEN** a session-rooted component is later adopted by a component
- **THEN** the session root no longer holds it and the new owner is responsible for disposal

#### Scenario: Disposal releases the session root reference
- **WHEN** a session-rooted component is disposed
- **THEN** the session root no longer holds it

## MODIFIED Requirements

### Requirement: Ambient ownership registration
Resources created while a component is building SHALL be registered against the component that is building, without the call site performing an explicit adoption step. Registration SHALL be identity-idempotent. Registration SHALL resolve to the active component when one is present and to the session root when none is present.

#### Scenario: Implicit adoption during build
- **WHEN** a reactive primitive is constructed inside a component's `build()`
- **THEN** it is adopted by that component without an explicit adoption call at the construction site

#### Scenario: Implicit adoption falls back to the session root
- **WHEN** a component is constructed with no active build scope
- **THEN** the component is adopted by the session root

#### Scenario: Repeated registration is harmless
- **WHEN** the same resource is registered twice against the same owner
- **THEN** it is stored once