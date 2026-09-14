## ADDED Requirements

### Requirement: Select input facade
`UI` SHALL provide a static facade `select(Signal<T> signal, List<T> options)` that constructs a `SolimSelect`, automatically attaches its element to the active parent in `ParentStack`, and returns the component for chained modifier calls. No `switch()` facade SHALL exist (`switch` is a Java keyword); `switchToggle(Signal<Boolean>)` remains the switch facade.

#### Scenario: Attaching select via UI facade
- **WHEN** `select(signal, options)` is called inside a `children()` block
- **THEN** a `SolimSelect` is created showing the current signal value, its element attached to the parent table, and the `SolimSelect` instance returned
