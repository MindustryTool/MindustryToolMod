# table-config-mixin Specification

## Purpose
Defines the `TableConfig<SELF>` mixin interface for operations that configure an Arc Table's content defaults and child spacing.

## Requirements

### Requirement: TableConfig is a mixin interface with default methods
`TableConfig<SELF>` SHALL be a public interface in `solim.modifier` with generic parameter `<SELF extends TableConfig<SELF>>`. Implementing components SHALL provide `Table table()` to supply their underlying Arc Table.

#### Scenario: Component implements TableConfig
- **WHEN** a Solim component `implements TableConfig<MyComponent>` and provides `Table table()`
- **THEN** it inherits all TableConfig default methods for free

### Requirement: TableConfig provides alignment methods
`TableConfig` SHALL provide `align(int)`, `top()`, `bottom()`, `left()`, `right()`, `center()` that configure the Table's content alignment.

#### Scenario: Aligning table content
- **WHEN** `top()` is called on a TableConfig component
- **THEN** the table's alignment is set to top

### Requirement: TableConfig provides margin methods
`TableConfig` SHALL provide `margin(float)`, `margin(Readable<Float>)`, `margin(float, float, float, float)`, `margin(Readable<Float>, Readable<Float>, Readable<Float>, Readable<Float>)`, `marginTop(float)`, `marginTop(Readable<Float>)`, `marginBottom(float)`, `marginBottom(Readable<Float>)`, `marginLeft(float)`, `marginLeft(Readable<Float>)`, `marginRight(float)`, `marginRight(Readable<Float>)` that set the Table's margins. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting table margin
- **WHEN** `margin(8f)` is called
- **THEN** the table has 8f margin on all sides

### Requirement: TableConfig provides padding methods
`TableConfig` SHALL provide `padding(float)`, `padding(Readable<Float>)`, `padding(float, float, float, float)`, `padding(Readable<Float>, Readable<Float>, Readable<Float>, Readable<Float>)`, `paddingTop(float)`, `paddingTop(Readable<Float>)`, `paddingBottom(float)`, `paddingBottom(Readable<Float>)`, `paddingLeft(float)`, `paddingLeft(Readable<Float>)`, `paddingRight(float)`, `paddingRight(Readable<Float>)`, `paddingX(float)`, `paddingX(Readable<Float>)`, `paddingY(float)`, `paddingY(Readable<Float>)` that set the Table's margins (padding on a Table IS its margins). All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting table padding
- **WHEN** `padding(12f)` is called
- **THEN** the table has 12f margin on all sides (Table padding = margin)

### Requirement: TableConfig provides gap and respace
`TableConfig` SHALL provide `gap(float)`, `gap(Readable<Float>)`, and `respace()` for configuring inter-sibling spacing. Readable overloads SHALL install reactive Effects.

#### Scenario: Setting gap
- **WHEN** `gap(8f)` is called on a Row
- **THEN** adjacent children have 8f spacing along the primary axis
