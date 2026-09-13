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
