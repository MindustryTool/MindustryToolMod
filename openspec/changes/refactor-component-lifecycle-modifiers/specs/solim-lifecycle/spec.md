## MODIFIED Requirements

### Requirement: Component as child via ElementResolver
Layouts and parents SHALL accept children that are `Element` or `Component`; `Component` children SHALL be automatically resolved to `component.element()` without requiring callers to write `new Header().element()`. Components instantiated inside a `children()` block SHALL be automatically attached to the current parent via the pending attachment mechanism without re-parenting across ownership contexts.

#### Scenario: Direct Component child without component() call
- **WHEN** `column(() -> { new Header(); new ChatPanel(); })` where `Header` and `ChatPanel` extend `Component`
- **THEN** parent column contains their resolved `Element`s in order, without any `component()` call

#### Scenario: Mixed Element and Component children
- **WHEN** `column(() -> { text("Hello"); new UserCard(user); image(tex); })`
- **THEN** all three children are attached correctly without requiring manual `.element()` calls

#### Scenario: Explicit component() still valid
- **WHEN** `column(() -> { component(new Header()); })`
- **THEN** the element is attached exactly once; no duplicate child is added

## ADDED Requirements

### Requirement: Immutable component ownership and no reparenting
`Component` SHALL NOT provide a `reparentTo` method. A component's lifecycle and ownership SHALL remain immutable once instantiated. When a new parent requires a component structure, a new component instance SHALL be instantiated.

#### Scenario: Components do not mutate parent ownership
- **WHEN** a component is created in an ownership scope
- **THEN** it cannot be reassigned or adopted into another owner via `reparentTo`.

### Requirement: Single lazy realization upon parent attachment
Component `element()` SHALL be invoked lazily when the component is attached to its parent container by `AttachmentStack.pop()`. Factory methods in `solim.UI` and modifier methods in `CellConfig` SHALL NOT eagerly realize `element()`.

#### Scenario: Modifier chaining before realization
- **WHEN** `button("Click").minWidth(100f).size(40f)` is called inside a `children()` block
- **THEN** `element()` is not invoked until the parent `children()` scope ends and attaches its pending children.
