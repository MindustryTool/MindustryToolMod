## ADDED Requirements

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card. The map title SHALL be rendered inside a translucent dark overlay along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons (`♡ likes`, `💬 comments`, `⬇ downloads`, `▶ play`). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview image occupies the upper hero area with title overlaid on bottom, followed by a compact action button row

### Requirement: Full-Screen Map Detail Dialog
`MapDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the map preview image SHALL occupy 55% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the map preview image SHALL occupy 45% of the screen height with details scrolling underneath.

#### Scenario: Maximized map viewing area
- **WHEN** a map detail dialog is opened in landscape mode
- **THEN** the map preview expands to fill 55% of dialog width and 100% of dialog height

### Requirement: Zero Layout Shift Map Loading
Map preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Map loads without reflow
- **WHEN** a map thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion
