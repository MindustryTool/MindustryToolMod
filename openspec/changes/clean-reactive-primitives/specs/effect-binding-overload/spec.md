## ADDED Requirements

### Requirement: Effect.of(Readable, Cons) binding overload
`Effect` SHALL provide a static factory `Effect.of(Readable<T> source, Cons<T> target)` that creates an `Effect` whose body reads `source.get()` and passes the value to `target`. The resulting `Effect` SHALL auto-register with the ambient `OwnershipContext` exactly once, exactly as all other `Effect.of()` overloads do. It SHALL track `source` as a reactive dependency and re-run whenever `source` changes.

#### Scenario: Effect tracks the readable as a dependency
- **WHEN** `Effect.of(signal, label::setText)` is called inside a component `build()`
- **THEN** the effect executes immediately with the current value, the signal is recorded as a dependency, and the effect re-runs on the next frame whenever the signal value changes

#### Scenario: Auto-owned by ambient component
- **WHEN** `Effect.of(signal, target)` is called inside a component `build()`
- **THEN** the resulting Effect is adopted by the enclosing component exactly once and disposed when that component is disposed

#### Scenario: Effect disposed stops updating target
- **WHEN** the owning component is disposed
- **THEN** subsequent signal changes no longer invoke the target consumer

#### Scenario: Computed readable tracks and updates
- **WHEN** `Effect.of(computed, element::setColor)` is called and the computed's upstream signal changes
- **THEN** the computed recomputes lazily during the effect's re-run and the new value is passed to the consumer

#### Scenario: No double registration
- **WHEN** `Effect.of(source, target)` is called
- **THEN** `OwnershipContext.register()` is called exactly once for the resulting Effect, identical to `Effect.of(Runnable)` behavior
