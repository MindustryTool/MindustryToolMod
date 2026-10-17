## MODIFIED Requirements

### Requirement: Unified alignment direction and bitmask composition

All Solim layout containers (`Column`, `Row`, `Card`, `Grid`, `Wrap`, `SolimCollapser`, `Scroll`), reactive containers (`When`, `Dynamic`, `ForEach`, `QueryView`), interactive controls (`Button`, `Checkbox`, `Switch`, `SolimSelect`, `SolimSlider`, `SolimTextField`), and display primitives (`Text`, `SolimImage`, `NetworkImage`) SHALL support directional alignment properties `top()`, `bottom()`, `left()`, `right()`, `center()`, and their compositions (`top()` + `left()`, `top()` + `right()`, `bottom()` + `left()`, `bottom()` + `right()`, `top()` + `center()`, `bottom()` + `center()`, `left()` + `center()`, `right()` + `center()`) declared through `.props(...)`. Alignment properties SHALL configure the component's parent cell constraints (`PendingCellConfig.align`) and, for container components, configure inner container alignment and child cell defaults. Alignment SHALL be applied through an `AlignmentCapability` implemented by the host, and dispatch SHALL NOT use an `instanceof` ladder over concrete host types. When opposite directional alignments are applied sequentially (e.g. `top()` then `bottom()`, or `left()` then `right()`), the latter alignment SHALL override the opposing direction without bit corruption.

#### Scenario: Alignment composition on container

- **WHEN** `column().props(top(), center())` or `column().props(center(), top())` is configured
- **THEN** the column's parent cell alignment and table default alignment evaluate to `(Align.top | Align.center)`

#### Scenario: Alignment override replaces opposing direction

- **WHEN** a component applies `top()` and subsequently applies `bottom()`
- **THEN** the resulting alignment bitmask contains `Align.bottom` and does NOT contain `Align.top`

#### Scenario: Alignment dispatch is polymorphic

- **WHEN** an alignment property is applied to a container that hosts children versus a display primitive that does not
- **THEN** the container updates both its parent cell alignment and its inner table defaults through its `AlignmentCapability`, and the primitive updates only its parent cell alignment, with no `instanceof` check over concrete host types
