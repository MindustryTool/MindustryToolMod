## MODIFIED Requirements

### Requirement: Shared reactive-source contract
The system SHALL provide a shared reactive-source contract in `solim-api` implemented by `Signal`, `Computed`, `MapSignal`, and `MapSignal.KeyReadable`, exposing polymorphic observer attach and detach so dependency wiring does not rely on concrete-type discovery. `Signal` and `Computed` SHALL participate in this contract exclusively through the `ReactiveObserver` channel. They SHALL NOT provide a parallel listener-callback channel alongside the observer channel.

#### Scenario: New primitive adds no dispatch branches
- **WHEN** a new reactive source implementing the contract is read inside a `Computed` or `Effect`
- **THEN** the observer attaches and detaches through the contract with no additional type branches in `Computed` or `Effect`

#### Scenario: Disposal detaches through the contract
- **WHEN** a `Computed` or `Effect` depending on any contract source is disposed
- **THEN** it detaches from every dependency through the contract and leaves no observers behind

#### Scenario: Signal notifies only through observer channel
- **WHEN** `signal.set(newValue)` is called
- **THEN** only registered `ReactiveObserver`s are notified; there is no separate listener-callback list to iterate

#### Scenario: Computed notifies only through observer channel
- **WHEN** a computed's upstream dependency changes and the computed has downstream observers
- **THEN** only registered `ReactiveObserver`s are notified via `invalidate()`; no listener callbacks fire

### Requirement: Unified reactive tracking
Reactive reads through the contract SHALL share one tracking path: reactive reads register the current observer via `ReactiveContext`, non-reactive `peek()` reads never register, build-time reads warn, and store-held sources never register with `OwnershipContext`.

#### Scenario: Reactive read tracks once
- **WHEN** `Signal.get()`, `MapSignal.get()`, `MapSignal.size()`, `MapSignal.isEmpty()`, or `readable(key).get()` is called inside a `Computed` or `Effect`
- **THEN** the caller is registered exactly once for the read source and invalidated when that source changes

#### Scenario: Peek stays untracked
- **WHEN** `peek()` is called inside or outside a reactive context
- **THEN** no dependency is registered and no warning is emitted

#### Scenario: Store sources survive unmounts
- **WHEN** a component reading a store-held source unmounts and disposes its observer
- **THEN** the source itself remains live and serves subsequent subscribers
