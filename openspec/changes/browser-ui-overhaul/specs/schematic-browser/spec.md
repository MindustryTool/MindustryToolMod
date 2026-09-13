## ADDED Requirements

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card. The schematic title SHALL be rendered inside a translucent dark overlay along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons:
1. Like button with like count (`♡ count`)
2. Comment button with comment count (`💬 count`)
3. Download/save button with download count (`⬇ count`)
4. Copy string button (`📋`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview image occupies the upper hero area with title overlaid on bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

### Requirement: Full-Screen Schematic Detail Dialog
`SchematicDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the preview image SHALL occupy 55% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the preview image SHALL occupy 45% of the screen height with details scrolling underneath.

#### Scenario: Maximized viewing area in landscape
- **WHEN** a schematic detail dialog is opened in landscape mode
- **THEN** the preview image expands to fill 55% of the dialog width and full dialog height

### Requirement: Zero Layout Shift Image Loading
Schematic preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Image loads without reflow
- **WHEN** a schematic thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion
