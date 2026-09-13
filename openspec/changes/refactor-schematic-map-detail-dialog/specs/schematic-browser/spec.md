## MODIFIED Requirements

### Requirement: Schematic Detail Inspection
The system SHALL provide a SchematicDetailDialog showing high-resolution preview image, author, dimensions, item requirements breakdown, tags, and description. All detail sections SHALL use `WebStyles.previewCard()` containers with consistent `gap(unit(1))` spacing. The preview image SHALL use viewport-relative sizing via `dvw()`/`dvh()` with values large enough for comfortable inspection (50% of screen height in portrait, 60% of dialog width in landscape). Action buttons SHALL use `WebStyles.cardActionText()` styling matching SchematicCard's stat buttons.

#### Scenario: View schematic requirements
- **WHEN** schematic detail dialog is presented
- **THEN** it SHALL display required construction items with item icons and amounts inside a `WebStyles.previewCard()` container with `gap(unit(1))` spacing

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the preview image SHALL render above the scrollable details in a single-column stack with height of 50% screen height via `dvh(50f)`
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the preview image and details SHALL render side-by-side in a two-column row with the image occupying 60% of dialog width via `dvw(60f)`

#### Scenario: Consistent section styling
- **WHEN** any detail section is rendered (author, stats, tags, requirements, description)
- **THEN** it SHALL be wrapped in a `WebStyles.previewCard()` styled container with `gap(unit(1))` internal spacing

#### Scenario: Action buttons match card style
- **WHEN** the Copy and Save action buttons are rendered in the detail dialog
- **THEN** they SHALL use `WebStyles.cardActionText()` styling with `height(unit(9))` and `growX()`, matching SchematicCard's stat button appearance

### Requirement: Full-Screen Schematic Detail Dialog
`SchematicDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the preview image SHALL occupy 60% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the preview image SHALL occupy 50% of the screen height with details scrolling underneath.

#### Scenario: Maximized viewing area in landscape
- **WHEN** a schematic detail dialog is opened in landscape mode
- **THEN** the preview image expands to fill 60% of the dialog width and full dialog height

#### Scenario: Large preview in portrait
- **WHEN** a schematic detail dialog is opened in portrait mode
- **THEN** the preview image occupies 50% of the screen height via `dvh(50f)`
