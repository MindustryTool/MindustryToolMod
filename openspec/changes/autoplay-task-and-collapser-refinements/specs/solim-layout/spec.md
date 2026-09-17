## ADDED Requirements

### Requirement: SolimCollapser layout container
`SolimCollapser` SHALL wrap Arc's `arc.scene.ui.layout.Collapser` as a Solim component supporting children declaration, reactive expansion/collapse signals, smooth animation transitions (defaulting to 0.2s), and collapsing `prefHeight` and `minHeight` to 0 when collapsed so no leftover whitespace is allocated in parent table cells.

#### Scenario: Collapsed state occupies zero height
- **WHEN** a `SolimCollapser` is rendered in a collapsed state
- **THEN** its reported `prefHeight` and `minHeight` are 0, and touchable is disabled

#### Scenario: Reactive expansion
- **WHEN** the bound expanded signal changes from `false` to `true`
- **THEN** the `SolimCollapser` expands to fit its content with animated transition
