## ADDED Requirements

### Requirement: Dynamic query keys rebind the cache observer
When a `Query` is constructed with a key supplier that can resolve to different keys over time, the `Query` SHALL remain subscribed for cache invalidation to every key it actually fetches under. When a fetch resolves to a key different from the currently observed one, the `Query` SHALL attach its invalidation listener to the new key and detach it from the previous key.

#### Scenario: Invalidation of the active key triggers refetch
- **WHEN** a `Query` built with a key supplier has fetched under key K and `QueryCache.invalidate(K)` is called
- **THEN** the `Query` refetches

#### Scenario: Key change rebinds the observer
- **WHEN** the key supplier resolves to K1 and the `Query` fetches, and the supplier later resolves to K2 and the `Query` fetches again
- **THEN** the invalidation listener is attached to K2 and detached from K1

#### Scenario: Previous key is no longer observed after a change
- **WHEN** the key supplier has resolved to K2
- **THEN** `QueryCache.invalidate(K1)` does not trigger a refetch

#### Scenario: Constant key binds once
- **WHEN** a `Query` is constructed with a fixed key rather than a supplier
- **THEN** exactly one cache observer attachment is made for that key and it is detached on disposal

#### Scenario: Disposal releases every observed key
- **WHEN** a `Query` that has fetched under multiple keys is disposed
- **THEN** each observed key has its observer detached and any unobserved entry is scheduled for eviction

## MODIFIED Requirements

### Requirement: Query reactive primitive
`Query<T>` SHALL be a reactive primitive in `solim-core/src/solim/reactive/` alongside `Signal`, `Computed`, and `Effect`. It SHALL implement `Disposable` and `Readable<T>` (where `get()` returns the current data value). It SHALL integrate with `OwnershipContext.register()` for automatic lifecycle ownership, the same as `Computed` and `Effect`. Its cached-data presence check SHALL distinguish a completed fetch that produced `null` from a fetch that has never completed, so that a legitimately empty result does not suppress future fetching.

#### Scenario: Query listed as reactive primitive
- **WHEN** the Solim reactive module is inspected
- **THEN** `Query` SHALL be available alongside `Signal`, `Computed`, `Effect`, and `Mutation` as a first-class reactive primitive

#### Scenario: Query implements Readable
- **WHEN** `query.get()` is called
- **THEN** it SHALL return the current data value (equivalent to `query.data().get()`) and participate in reactive dependency tracking

#### Scenario: Empty result does not suppress fetching
- **WHEN** a query's fetcher resolves `null` and the query is later re-enabled or its key becomes stale
- **THEN** the query fetches again rather than treating the cached `null` as fresh data