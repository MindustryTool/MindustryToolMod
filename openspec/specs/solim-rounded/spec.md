# solim-rounded Specification

## Purpose
TBD - created by archiving change add-squircle-background-and-border. Update Purpose after archive.
## Requirements
### Requirement: Procedural continuous-curvature rounded 9-patch generation
The system SHALL generate pure white 9-patch drawables for continuous-curvature rounded shapes using the L4 superellipse norm (x^4 + y^4 <= r^4) with sub-pixel anti-aliasing.

#### Scenario: Solid rounded generation
- **WHEN** a rounded drawable with radius 8 is requested
- **THEN** a NinePatchDrawable is produced with dimensions matching the radius and sub-pixel smoothed boundary alphas

#### Scenario: Hollow rounded border generation
- **WHEN** a rounded border with radius 12 and stroke width 2 is requested
- **THEN** a NinePatchDrawable is produced with an anti-aliased border stroke and transparent center

### Requirement: Rounded drawable caching
The system SHALL cache generated rounded NinePatchDrawables by integer radius and stroke thickness, reusing existing textures for matching parameters.

#### Scenario: Cache hit on repeated requests
- **WHEN** rounded drawable with radius 10 is requested multiple times
- **THEN** subsequent calls return the cached NinePatchDrawable instance without creating new textures

### Requirement: Composite RoundedDrawable with fill and border
The system SHALL provide a RoundedDrawable that composites a rounded background fill and an optional rounded border stroke in a single Drawable instance with independent colors. By default, the fill color SHALL be transparent (`Color.clear`) so that adding a border or corner radius alone does not inject an opaque background fill.

#### Scenario: Composite drawing
- **WHEN** a RoundedDrawable configured with fill color darkGray and border color accent is drawn
- **THEN** the fill 9-patch is rendered with darkGray followed by the border 9-patch rendered with accent in the current batch

#### Scenario: Default transparent background
- **WHEN** a RoundedDrawable is created without specifying a fill color or when border is applied to an element
- **THEN** the fill color SHALL default to Color.clear and no solid fill is rendered

### Requirement: Universal LayoutModifiers rounded and border support
All Solim layout containers implementing LayoutModifiers (including Card, Column, Row, Grid, SolimStack, and Scroll) SHALL provide .rounded(...) and .border(...) modifiers.

#### Scenario: Layout container rounded styling
- **WHEN** column().rounded(12, Pal.darkMetal).border(1.5f, Pal.accent) is declared
- **THEN** the column's underlying table receives a background RoundedDrawable configured with radius 12 and border stroke 1.5

#### Scenario: Reactive color updates on container
- **WHEN** a reactive Readable<Color> is supplied to .rounded(radius, colorSignal) and the signal changes value
- **THEN** the container's RoundedDrawable updates its fill color without recreating the container or underlying textures

### Requirement: Widget and dialog rounded support
Solim interactive widgets (Button, SolimTextField) and overlay components (SolimDialog, Popup, Badge) SHALL provide rounded and border configuration.

#### Scenario: Button rounded styling
- **WHEN** utton().rounded(8, Pal.gray).border(1f, Pal.accent) is declared
- **THEN** the button background style reflects the rounded fill and border

#### Scenario: Dialog rounded panel
- **WHEN** dialog().rounded(16, Pal.darkMetal).border(2f, Pal.accent) is declared
- **THEN** the dialog main window panel renders with the rounded background and border

### Requirement: Static ElementModifiers for arbitrary Arc elements
The system SHALL provide static utility methods in ElementModifiers to apply rounded backgrounds and borders to any Arc Table or Element.

#### Scenario: Direct element modification
- **WHEN** ElementModifiers.rounded(table, 10, Color.darkGray) is called on an Arc Table
- **THEN** the table's background is updated with a rounded drawable matching radius 10 and darkGray

