## MODIFIED Requirements

### Requirement: Universal LayoutModifiers rounded and border support
All Solim layout containers implementing LayoutModifiers (including Card, Column, Row, Grid, SolimStack, and Scroll) SHALL provide .rounded(...) and .border(...) modifiers.

#### Scenario: Layout container rounded styling
- **WHEN** column().rounded(12, Pal.darkMetal).border(1.5f, Pal.accent) is declared
- **THEN** the column's underlying table receives a background RoundedDrawable configured with radius 12 and border stroke 1.5

#### Scenario: Reactive color updates on container
- **WHEN** a reactive Readable<Color> is supplied to .rounded(radius, colorSignal) and the signal changes value
- **THEN** the container's RoundedDrawable updates its fill color without recreating the container or underlying textures
