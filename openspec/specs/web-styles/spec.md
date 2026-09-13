# web-styles Specification

## Purpose
Provides dedicated, rounded-border button styles with continuous curvature and multi-state variants styled with the channel-list blue/indigo palette for web and browser components without mutating Arc global styles.

## Requirements

### Requirement: WebStyles Multi-State Rounded Button Styling
The system SHALL provide isolated `TextButtonStyle` and `ButtonStyle` instances styled with the channel-list blue palette (`Color(0.45f, 0.35f, 0.9f, 0.8f)`), distinct `up`, `down`, `over`, and `disabled` visual states, and continuous rounded borders using `RoundedDrawable`.

#### Scenario: Normal and interactive states
- **WHEN** a button styled with WebStyles is rendered in normal, hovered, or pressed state
- **THEN** it SHALL render with a rounded border and the corresponding channel-list blue tint without mutating `arc.scene.ui.Button.ButtonStyle` or `mindustry.ui.Styles` globals

#### Scenario: Disabled state styling
- **WHEN** a WebStyles button has its enabled state set to false
- **THEN** it SHALL render with a muted dark background, dark gray rounded border, and disabled font/image color

### Requirement: WebStyles Factory Methods and Composition
`WebStyles` SHALL provide factory methods (such as `WebStyles.button()`, `WebStyles.webButton()`) returning reusable button styles, and SHALL allow composing and deriving customized button styles using `.from(baseStyle)`.

#### Scenario: Pre-built cached style retrieval
- **WHEN** `WebStyles.webButton()` is called multiple times
- **THEN** it returns a shared, pre-built `ButtonStyle` with zero new object allocations

#### Scenario: Extending a base style via composition
- **WHEN** `button.style(s -> s.from(WebStyles.button()).border(Color.scarlet))` is invoked
- **THEN** the button inherits all base properties from `WebStyles.button()` but overrides the border color with `Color.scarlet`
