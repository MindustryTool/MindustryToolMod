## ADDED Requirements

### Requirement: Declarative QueryView in Browser Dialogs
`BrowserState` SHALL expose `Query<List<T>>` directly and SHALL NOT maintain duplicate `items`, `loading`, and `error` signals or bridge effects.
- `SchematicBrowserDialog` and `MapBrowserDialog` SHALL render content using declarative `query(state.query()).loading(...).error(...).data(...)`.

#### Scenario: Declarative query rendering in schematic browser
- **WHEN** `SchematicBrowserDialog` builds its content view
- **THEN** it SHALL use `query(state.query())` to declaratively render loading, error, and schematic card grid states

#### Scenario: Declarative query rendering in map browser
- **WHEN** `MapBrowserDialog` builds its content view
- **THEN** it SHALL use `query(state.query())` to declaratively render loading, error, and map card grid states
