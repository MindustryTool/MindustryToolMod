## ADDED Requirements

### Requirement: ReactiveGrid Item Context
`ReactiveGrid` SHALL provide a `GridItemContext` interface to child item factories, exposing reactive layout metrics including the computed usable item width and current column count.

#### Scenario: Item width matches rendered cell width
- **WHEN** `ReactiveGrid` is rendered and its backing table width changes
- **THEN** `GridItemContext.itemWidth()` SHALL reactively reflect the usable cell content width (`(gridWidth / columnCount) - gap`)

#### Scenario: Column count updates reactively
- **WHEN** the grid's column count signal emits a new value
- **THEN** `GridItemContext.columnCount()` SHALL emit the updated column count

### Requirement: ReactiveGrid Overloaded Item Factory
`ReactiveGrid` and the `solim.UI` facade SHALL support overloaded factory signatures accepting a `BiFunction<T, GridItemContext, Component>` while maintaining full backward compatibility for existing single-argument `Function<T, Component>` factories.

#### Scenario: Item factory receives context
- **WHEN** a `ReactiveGrid` is constructed with a `BiFunction<T, GridItemContext, Component>` item factory
- **THEN** each reconciled item component SHALL be instantiated with the item data and the grid's `GridItemContext`

#### Scenario: Backward compatible construction
- **WHEN** a `ReactiveGrid` is constructed with a single-parameter `Function<T, Component>`
- **THEN** existing items SHALL be reconciled and rendered without errors
