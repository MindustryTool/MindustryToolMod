# unified-border-background Specification

## Purpose
Order-independent composition of background drawables/colors, continuous-curvature rounded corners, and border strokes on Solim elements with automatic geometry safety clamping.

## Requirements
### Requirement: Order-Independent Background and Border Composition
The system SHALL preserve and compose background drawables, background fill colors, corner radius, and border strokes regardless of the order in which .background(...), .rounded(...), and .border(...) are called on a Solim container or element.

#### Scenario: Background called after border
- **WHEN** developer calls .border(1f, Color.gray) followed by .background(Styles.black6) on a Solim container
- **THEN** the element renders the Styles.black6 background with the 1px gray border stroke overlay intact

#### Scenario: Border called after background
- **WHEN** developer calls .background(Styles.black6) followed by .border(1f, Color.gray) on a Solim container
- **THEN** the element renders the Styles.black6 background with the 1px gray border stroke overlay intact

### Requirement: Fluent Background Color Overloads
The system SHALL support setting background color directly on Solim layout containers via .background(Color) and .background(Readable<Color>) overloads on LayoutModifiers and ElementModifiers.

#### Scenario: Set static background color on Column
- **WHEN** developer calls column().background(Color.royal)
- **THEN** the column renders with a royal blue background fill

#### Scenario: Set reactive background color on Row
- **WHEN** developer binds row().background(colorSignal)
- **THEN** the row background updates automatically when colorSignal changes value

### Requirement: Dynamic Corner Radius Geometry Clamping
The system SHALL automatically clamp the rendered corner radius in RoundedDrawable so that it never exceeds half the rendered width or height (min(width, height) / 2), preventing NinePatch negative slice height inversions and line artifacts.

#### Scenario: Oversized radius on small element
- **WHEN** developer specifies rounded(40) on an element with height of 36px
- **THEN** the rendered radius is clamped to 18px, forming a smooth pill shape without inverted lines or overlapping crossover artifacts
