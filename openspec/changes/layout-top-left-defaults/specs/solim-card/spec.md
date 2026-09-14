## ADDED Requirements

### Requirement: Card children default to top-left
The `Card` inner container SHALL default children to top-left alignment via both `defaults().top().left()` and per-cell `cell.top().left()` at attach time, matching `Row`/`Column`. Explicit `.top()`, `.left()`, `.right()`, `.bottom()`, or `.center()` modifiers SHALL continue to override the default.

#### Scenario: Bare card stacks from top-left
- **WHEN** `card(() -> { text("Title"); text("Subtitle"); })` is rendered in a larger area without alignment modifiers
- **THEN** child cells carry top-left alignment and children stack from the top-left of the card
