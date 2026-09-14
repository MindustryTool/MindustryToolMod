# element-config-mixin Specification

## Purpose
TBD - created by archiving change refactor-element-config-mixins. Update Purpose after archive.
## Requirements
### Requirement: ElementConfig is a mixin interface with default methods
`ElementConfig<SELF>` SHALL be a public interface in `solim.modifier` with generic parameter `<SELF extends ElementConfig<SELF>>`. Implementing components SHALL provide `Element element()` to supply their root Arc Element.

#### Scenario: Component implements ElementConfig
- **WHEN** a Solim component `implements ElementConfig<MyComponent>` and provides `Element element()`
- **THEN** it inherits all ElementConfig default methods for free

### Requirement: ElementConfig provides size mutations
`ElementConfig` SHALL provide `width(float)`, `width(Readable<Float>)`, `height(float)`, `height(Readable<Float>)`, `size(float)`, `size(float, float)`, `size(Readable<Float>)`, `size(Readable<Float>, Readable<Float>)` that set the element's dimensions and update the parent cell if attached. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting element width
- **WHEN** `width(100f)` is called on an ElementConfig component
- **THEN** the element's width is set to 100f and the parent cell width is updated if attached

#### Scenario: Setting reactive element width
- **WHEN** `width(signal)` is called and signal changes from 100f to 200f
- **THEN** the element's width updates to 200f automatically

### Requirement: ElementConfig provides position mutations
`ElementConfig` SHALL provide `x(float)`, `x(Readable<Float>)`, `y(float)`, `y(Readable<Float>)`, `position(float, float)`, `position(Readable<Float>, Readable<Float>)` that set the element's local position.

#### Scenario: Setting element position
- **WHEN** `position(10f, 20f)` is called
- **THEN** the element's x is 10f and y is 20f

### Requirement: ElementConfig provides visibility
`ElementConfig` SHALL provide `visible(boolean)` and `visible(Readable<Boolean>)` that set element visibility and trigger gap re-spacing on parent Table if applicable.

#### Scenario: Hiding an element
- **WHEN** `visible(false)` is called
- **THEN** the element is hidden and parent Table re-spaces siblings

### Requirement: ElementConfig provides opacity
`ElementConfig` SHALL provide `opacity(float)`, `opacity(Readable<Float>)`, `alpha(float)`, `alpha(Readable<Float>)` that set element color alpha.

#### Scenario: Setting opacity
- **WHEN** `opacity(0.5f)` is called
- **THEN** the element's color alpha is 0.5f

### Requirement: ElementConfig provides naming
`ElementConfig` SHALL provide `name(String)` that sets the element's debug name.

#### Scenario: Setting element name
- **WHEN** `name("my-element")` is called
- **THEN** `element.name` equals `"my-element"`

