## ADDED Requirements

### Requirement: Prop declares its required capability and value type

`Prop` SHALL be generic over two type parameters, `Prop<Requires, V>`: `Requires` is the host capability a property needs and `V` is the property's value type. A property factory SHALL produce a property whose value type matches the setter it applies, so an incompatible value is a compile error rather than a `ClassCastException` at application time. The value SHALL NOT be stored or passed as `Object`, and applying a property SHALL NOT narrow the value with a cast. Value-less properties SHALL be represented by a dedicated value-less form rather than a boolean flag riding alongside a `null` value, and a value-less property SHALL still apply even though it carries no payload.

#### Scenario: Value type is enforced at the factory

- **WHEN** a property that applies a `Float` is declared with a value that is not a `Float` or `Readable<Float>`
- **THEN** the code does not compile, because the factory's value type parameter rejects it

#### Scenario: Applying a property does not cast the value

- **WHEN** any `solim-core` property factory applies its value
- **THEN** the value flows through a typed setter with no `(Readable<Float>)`, `(Readable<Boolean>)`, `(Readable<Color>)`, `num(...)`, or `(Integer)` narrowing at the prop boundary

#### Scenario: Value-less property still applies

- **WHEN** a value-less property such as `growX()` or `top()` is passed to `props(...)`
- **THEN** it applies its mutation even though it carries no value, and it is not skipped as if its value were `null`

### Requirement: Property target dispatch is cast-free and label-independent

A property SHALL resolve its Arc target (`Element`, `Table`, or the pending `Cell` configuration) through a typed host view selected by the property's required capability, so the target is not cast from `Object`. A property's diagnostic `label` SHALL NOT determine behaviour: alignment and every other property SHALL carry its own value-typed applier instead of being recognised by comparing label strings.

#### Scenario: Alignment is not dispatched by label

- **WHEN** `top()`, `bottom()`, `left()`, `right()`, `center()`, or `align(...)` is applied
- **THEN** the alignment is recorded by the property's own typed applier, and no code compares `prop.label()` to decide the behaviour

#### Scenario: No target cast

- **WHEN** a property is applied to a table-backed host, an element-only host, or a host with a pending cell configuration
- **THEN** the target is obtained from the typed host view for the property's capability, without casting an `Object` target
