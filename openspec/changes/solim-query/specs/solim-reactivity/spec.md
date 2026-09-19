## ADDED Requirements

### Requirement: Query reactive primitive
`Query<T>` SHALL be a reactive primitive in `solim-core/src/solim/reactive/` alongside `Signal`, `Computed`, and `Effect`. It SHALL implement `Disposable` and `Readable<T>` (where `get()` returns the current data value). It SHALL integrate with `ComponentContext.register()` for automatic lifecycle ownership, the same as `Computed` and `Effect`.

#### Scenario: Query listed as reactive primitive
- **WHEN** the Solim reactive module is inspected
- **THEN** `Query` SHALL be available alongside `Signal`, `Computed`, `Effect`, and `Mutation` as a first-class reactive primitive

#### Scenario: Query implements Readable
- **WHEN** `query.get()` is called
- **THEN** it SHALL return the current data value (equivalent to `query.data().get()`) and participate in reactive dependency tracking

### Requirement: Mutation reactive primitive
`Mutation<T, R>` SHALL be a reactive primitive in `solim-core/src/solim/reactive/` alongside `Signal`, `Computed`, `Effect`, and `Query`. It SHALL implement `Disposable` and integrate with `ComponentContext.register()` for automatic lifecycle ownership.

#### Scenario: Mutation listed as reactive primitive
- **WHEN** the Solim reactive module is inspected
- **THEN** `Mutation` SHALL be available as a first-class reactive primitive

### Requirement: UI facade methods
The `solim.UI` facade SHALL expose static methods for creating `Query` and `Mutation` instances, consistent with the existing pattern for `signal()`, `computed()`, and `effect()`.

#### Scenario: UI.query facade
- **WHEN** `UI.query(key, fetcher)` is called in a declarative Solim component
- **THEN** it SHALL create and return a `Query<T>` instance, equivalent to `Query.of(key, fetcher)`

#### Scenario: UI.mutation facade
- **WHEN** `UI.mutation(mutator)` is called in a declarative Solim component
- **THEN** it SHALL create and return a `Mutation<T, R>` instance, equivalent to `Mutation.of(mutator)`
