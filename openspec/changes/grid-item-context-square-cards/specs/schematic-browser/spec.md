## MODIFIED Requirements

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card inside a preview card container whose height dynamically matches its width (1:1 square aspect ratio) via a reactive preview height signal. The schematic title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts:
1. Like button with like count (heart icon + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview container occupies the upper area with width and height equal (1:1 square ratio) and title centered along the bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

#### Scenario: Dynamic square aspect ratio adjustment
- **WHEN** the parent grid cell width changes due to window resize or column count reflow
- **THEN** the preview card container height SHALL update to match the new width
