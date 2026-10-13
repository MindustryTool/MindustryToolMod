## MODIFIED Requirements

### Requirement: Cached data presence is distinct from value nullity
`CacheEntry` SHALL track whether a fetch has completed separately from whether the fetched value is non-null. A fetcher that legitimately resolves `null` (an empty search result, a not-found mapped to null) SHALL mark the entry as fetched, so that the presence of a cached entry SHALL NOT be used to infer that a non-null value exists.

#### Scenario: Cached null counts as fetched
- **WHEN** a fetch completes with a `null` result and the entry is stored
- **THEN** the entry reports that a fetch has completed and that it is not stale for the configured stale time

#### Scenario: Never-fetched entry is distinct from cached null
- **WHEN** an entry has never received a fetch result and a separate entry has received a `null` result
- **THEN** the two entries report different presence states

#### Scenario: Clearing resets fetch presence
- **WHEN** an entry is cleared
- **THEN** it reports that no fetch has completed, its value is `null`, and its update timestamp is reset

#### Scenario: Staleness is evaluated only for fetched entries
- **WHEN** staleness is evaluated for an entry that has never completed a fetch
- **THEN** the entry is reported as stale

### Requirement: Cache observer attachment is idempotent
Attaching an invalidation listener to a cache key SHALL increment the entry's active-observer count exactly once per distinct listener. Attaching the same listener to the same key more than once SHALL NOT inflate the refcount and SHALL NOT cause the listener to be invoked more than once per invalidation.

#### Scenario: Duplicate attach does not inflate the refcount
- **WHEN** the same listener is attached to the same key twice
- **THEN** the entry's active-observer count is `1`

#### Scenario: Detach balances a duplicated attach
- **WHEN** a listener was attached twice and is detached once
- **THEN** the active-observer count reaches zero and garbage-collection eviction becomes schedulable

#### Scenario: Invalidation invokes each listener once
- **WHEN** the key is invalidated and a listener is registered once
- **THEN** the listener's invalidation callback is invoked exactly once

#### Scenario: Re-attaching inside the GC window cancels eviction
- **WHEN** a listener attaches to a key whose eviction is already scheduled
- **THEN** the pending eviction task is cancelled and the entry survives