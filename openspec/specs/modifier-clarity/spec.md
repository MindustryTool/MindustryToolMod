# modifier-clarity Specification

## Purpose
TBD - created by archiving change solim-architecture-refactor. Update Purpose after archive.
## Requirements
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

### Requirement: Fluent ordering convention is consistent
All Solim container components SHALL support the following fluent ordering convention:
1. Container/self configuration (before `children()`)
2. `children()` — declares children
3. Parent-layout configuration (after `children()`)

#### Scenario: Post-children parent-layout modifiers work
- **WHEN** `.grow()` is called after `.children(() -> { ... })`
- **THEN** the growth modifier is applied to the container's parent cell and does not affect children

### Requirement: Duplicate modifiers between ElementModifiers and LayoutModifiers are removed
Any modifier present in both `ElementModifiers` and `LayoutModifiers` that targets the same object SHALL be de-duplicated. One authoritative location SHALL exist per modifier.

#### Scenario: No duplicate opacity modifier
- **WHEN** searching the Solim source for `opacity` modifier implementations
- **THEN** exactly one implementation exists, in the correct modifier class

