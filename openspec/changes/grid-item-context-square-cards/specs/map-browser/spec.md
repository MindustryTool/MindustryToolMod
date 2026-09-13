## MODIFIED Requirements

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card inside a preview card container whose height dynamically matches its width (1:1 square aspect ratio) via a reactive preview height signal. The map title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons using vanilla Mindustry icons with stat counts (heart icon + likes opening details, `Icon.chatSmall` + comments opening details, `Icon.downloadSmall` + downloads triggering download, `Icon.play` triggering play). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview container occupies the upper area with width and height equal (1:1 square ratio) and title centered along the bottom, followed by a compact action button row

#### Scenario: Dynamic square aspect ratio adjustment
- **WHEN** the parent grid cell width changes due to window resize or column count reflow
- **THEN** the preview card container height SHALL update to match the new width
