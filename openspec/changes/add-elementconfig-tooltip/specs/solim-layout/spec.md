## ADDED Requirements

### Requirement: ElementConfig tooltip modifier support
`ElementConfig<SELF>` SHALL provide default `.tooltip(@Nullable String tip)`, `.tooltip(@Nullable Readable<String> tip)`, and `.tooltip(@Nullable Cons<Table> tooltipBuilder)` methods that attach an Arc `Tooltip` listener to the component's underlying Arc `Element`.

When setting a new tooltip, any existing `Tooltip` listener on the element SHALL be removed to prevent duplicate listeners. Passing `null` or an empty string SHALL remove existing tooltip listeners without adding a new one.

Reactive tooltips (`Readable<String>`) SHALL bind a `Label` to the tooltip container and register the update `Effect` with `ComponentContext` for ambient lifecycle disposal.

#### Scenario: Attaching static tooltip to a component
- **WHEN** a component implementing `ElementConfig` calls `.tooltip("My tooltip")`
- **THEN** a `Tooltip` listener is attached to its element, showing "My tooltip" on hover

#### Scenario: Replacing an existing tooltip
- **WHEN** a component calls `.tooltip("First")` followed by `.tooltip("Second")`
- **THEN** the first `Tooltip` listener is removed and only the second `Tooltip` listener remains attached

#### Scenario: Clearing a tooltip
- **WHEN** a component calls `.tooltip((String) null)` or `.tooltip("")` on an element with an existing tooltip
- **THEN** the existing `Tooltip` listener is removed from the element

#### Scenario: Attaching reactive tooltip to a component
- **WHEN** a component calls `.tooltip(tooltipSignal)` with a `Readable<String>`
- **THEN** a `Tooltip` containing a reactive label is attached to the element and updates when the signal emits a new value

#### Scenario: Custom tooltip builder configuration
- **WHEN** a component calls `.tooltip(t -> t.background(Styles.black6).add("Custom"))`
- **THEN** a `Tooltip` is attached and configured via the provided `Cons<Table>` builder
