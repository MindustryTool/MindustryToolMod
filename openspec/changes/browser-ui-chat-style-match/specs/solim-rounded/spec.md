## MODIFIED Requirements

### Requirement: Composite RoundedDrawable with fill and border
The system SHALL provide a RoundedDrawable that composites a rounded background fill and an optional rounded border stroke in a single Drawable instance with independent colors. By default, the fill color SHALL be transparent (`Color.clear`) so that adding a border or corner radius alone does not inject an opaque background fill.

#### Scenario: Composite drawing
- **WHEN** a RoundedDrawable configured with fill color darkGray and border color accent is drawn
- **THEN** the fill 9-patch is rendered with darkGray followed by the border 9-patch rendered with accent in the current batch

#### Scenario: Default transparent background
- **WHEN** a RoundedDrawable is created without specifying a fill color or when border is applied to an element
- **THEN** the fill color SHALL default to Color.clear and no solid fill is rendered
