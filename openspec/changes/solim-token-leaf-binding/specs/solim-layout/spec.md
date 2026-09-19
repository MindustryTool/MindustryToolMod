## ADDED Requirements

### Requirement: SolimToken provides structured element metadata envelope
`SolimToken` SHALL be provided in package `solim.core` and stored on `Element.userObject` to encapsulate Solim-specific metadata without bare string or raw object collisions. It SHALL provide fields for `@Nullable Component component`, `@Nullable PendingCellConfig cellConfig`, `boolean expanding`, and `@Nullable Object userPayload`. When wrapping an element that already contains a non-Solim `userObject`, `SolimToken.getOrCreate(Element)` SHALL preserve the existing object in `userPayload`.

#### Scenario: Preserving external user payload
- **WHEN** an element has an existing non-Solim object assigned to `element.userObject` and `SolimToken.getOrCreate(element)` is called
- **THEN** `element.userObject` becomes a `SolimToken` instance whose `userPayload` matches the original object

#### Scenario: Tagging and checking expanding element
- **WHEN** `SolimToken.setExpanding(element, true)` is called
- **THEN** `SolimToken.isExpanding(element)` returns `true` and any existing `component` reference in the token remains intact

### Requirement: ParentStack mounts components with direct cell configurator
`ParentStack` SHALL pass `(Cell<?> cell, Element child, @Nullable Component component)` to the active `CellConfigurator`. When pending components are attached, the owning `Component` SHALL be passed directly without requiring backward element traversal.

#### Scenario: Applying cell constraints from component during attachment
- **WHEN** a component with pending cell constraints is mounted via `ParentStack`
- **THEN** `CellConfigurator` receives the `Component` directly and applies constraints to the parent `Cell` without relying on `child.userObject`

### Requirement: GapContainer respace via SolimToken
`GapContainer.respace(Table)` SHALL inspect `SolimToken.getComponent(table)` to locate the container and recompute gap spacing. Layout attachers (`Column`, `Row`, `Card`, `Grid`) and modifier callbacks SHALL invoke `GapContainer.respace(table)` without checking or casting raw `table.userObject`.

#### Scenario: Respacing container when child size changes
- **WHEN** `GapContainer.respace(table)` is called where `table` is associated with a `SolimToken` whose component implements `GapContainer`
- **THEN** the container's `respace()` method is invoked

## MODIFIED Requirements

### Requirement: PendingCellConfig provides find helper
`PendingCellConfig` SHALL provide a static `find(Object)` method that resolves a `PendingCellConfig` from an object chain: returns the object if it's a `PendingCellConfig`, calls `cellConfig()` if it's a `CellConfig`, or inspects `SolimToken` if it's an `Element`. Untyped legacy `userObject` recursion SHALL NOT be performed.

#### Scenario: Finding PendingCellConfig from Element
- **WHEN** `PendingCellConfig.find(element)` is called where `element` is associated with a `SolimToken` containing a `CellConfig` or `PendingCellConfig`
- **THEN** the resolved `PendingCellConfig` is returned
