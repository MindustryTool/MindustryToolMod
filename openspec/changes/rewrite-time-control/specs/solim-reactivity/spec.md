## ADDED Requirements

### Requirement: Signals provides reactive net state
`Signals` SHALL provide shared reactive net-state accessors (`active()`, `server()`, `client()`, `singlePlayer()`, `localHosting()`, `clientPlaying()`) following the same contract as `isPortrait()`: static initialization of current values, event-driven updates posted to the app thread, and set-only-on-change so equal values do not emit. Full predicates and drivers are specified in the `net-signals` capability; this requirement only extends the ambient-state contract to cover net state.

#### Scenario: Net signals follow the isPortrait contract
- **WHEN** two features subscribe to `Signals.server()`
- **THEN** both receive new values on change and neither is notified when a refresh yields an equal value

#### Scenario: Net signals initialize without a game running
- **WHEN** `Signals` class is loaded in the menu with no session
- **THEN** `active()`, `server()`, and `client()` peek as false and `singlePlayer()` peeks as true
