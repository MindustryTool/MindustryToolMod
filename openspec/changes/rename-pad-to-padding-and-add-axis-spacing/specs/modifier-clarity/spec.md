## MODIFIED Requirements

### Requirement: Modifier categories are non-overlapping and clearly defined
Every fluent modifier SHALL target exactly one of: (A) the component's own Arc Element, (B) the component's cell in its parent layout, or (C) the component as a container (its children's defaults). No modifier SHALL appear in both `ElementModifiers` and `LayoutModifiers` with the same effect. All padding modifiers SHALL use the canonical `padding` prefix, and parent-cell margins SHALL support two-axis `marginX` and `marginY` modifiers.

#### Scenario: Self modifiers affect the Element only
- **WHEN** `.visible(false)`, `.opacity(0.5f)`, or `.color(color)` is called
- **THEN** only the component's own Arc Element is modified; no parent cell is touched

#### Scenario: Parent-layout modifiers affect the cell only
- **WHEN** `.growX()`, `.margin(8f)`, `.marginX(12f)`, `.marginY(6f)`, or `.align(Align.left)` is called
- **THEN** only the component's cell in its parent layout is modified

#### Scenario: Container modifiers affect child defaults only
- **WHEN** `.padding(16f)`, `.paddingX(12f)`, `.paddingY(8f)`, or `.gap(8f)` is called on a container
- **THEN** only the container's child layout defaults are modified
