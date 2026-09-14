# solim-card Specification

## Purpose
Clickable, styled card container component with declarative child composition, fluent chained reactive property bindings, and click event bubbling control.
## Requirements
### Requirement: Declarative Card Container Component
The framework SHALL provide a `Card` component wrapping a clickable, styled container element. It SHALL support declarative child composition via `ParentStack`, custom click handling, and event bubbling control.

#### Scenario: Building card with children
- **WHEN** `card(() -> { text("Title"); text("Subtitle"); })` is executed
- **THEN** a `Card` instance is created and attached to the current parent, with the child elements added inside the card container.

#### Scenario: Card click action
- **WHEN** a card is configured with `.onClick(Runnable)` and clicked directly
- **THEN** the provided click handler is executed.

### Requirement: Chained Reactive Property Bindings on Card
The `Card` component SHALL provide fluent modifier methods for reactive and static properties including `.width(Readable<Float>)`, `.width(float)`, `.height(Readable<Float>)`, `.height(float)`, `.prefHeight(float)`, `.color(Readable<Color>)`, `.color(Color)`, `.style(ButtonStyle)`, `.name(String)`, and `.padding(float)`. The reactive bindings SHALL be automatically managed and disposed by the component lifecycle.

#### Scenario: Reactive width and color binding
- **WHEN** `card(...).width(cardWidth).color(cardColor)` is rendered and the underlying signals update
- **THEN** the card element's width and color are updated in place and layout is invalidated without reconstructing the card.

### Requirement: Click Event Bubbling Control in Card
The `Card` component and child widgets SHALL support event bubbling control such that interactive child controls can consume click events without triggering the card's click handler.

#### Scenario: Clicking child button inside card
- **WHEN** an interactive control inside a clickable card is clicked and stops event propagation
- **THEN** the child control's handler executes and the card's `onClick` handler is not triggered.

### Requirement: Card children default to top-left
The `Card` inner container SHALL default children to top-left alignment via both `defaults().top().left()` and per-cell `cell.top().left()` at attach time, matching `Row`/`Column`. Explicit `.top()`, `.left()`, `.right()`, `.bottom()`, or `.center()` modifiers SHALL continue to override the default.

#### Scenario: Bare card stacks from top-left
- **WHEN** `card(() -> { text("Title"); text("Subtitle"); })` is rendered in a larger area without alignment modifiers
- **THEN** child cells carry top-left alignment and children stack from the top-left of the card

