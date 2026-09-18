## MODIFIED Requirements

### Requirement: Responsive Card Grid Layout
The system SHALL dynamically compute column count based on fixed card width (`unit(58f)` / 232px) and gap spacing, without capping columns on wide screens, and SHALL center the content column on the horizontal X-axis matching the grid width.

#### Scenario: Screen orientation or resize change
- **WHEN** the viewport width changes due to window resize or mobile orientation change
- **THEN** the grid SHALL recalculate column count based on available width divided by fixed card slot size (`cardWidth + gap`) without rebuilding unaffected card elements

#### Scenario: Centered layout on horizontal axis
- **WHEN** the browser dialog is displayed on wide or high-resolution viewports
- **THEN** the content container holding the search header, scrollable grid, and footer SHALL have width equal to `cols * cardWidth + (cols - 1) * gap` and be horizontally centered within the full-screen dialog

#### Scenario: Touch-friendly targets on mobile
- **WHEN** rendered on mobile devices
- **THEN** all clickable buttons and card action triggers SHALL have a minimum touch target size of 40 units

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card inside a preview card container with fixed square dimensions (`unit(58f)` / 232px, 1:1 aspect ratio). The schematic title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts:
1. Like button with like count (heart icon + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview container occupies the upper area with a fixed square preview (`unit(58f)` / 232px) and title centered along the bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

#### Scenario: Square aspect ratio preservation
- **WHEN** the schematic card is rendered in the grid or rescaled on small viewports
- **THEN** the preview card container height SHALL match the card width to maintain a 1:1 square aspect ratio

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card inside a preview card container with fixed square dimensions (`unit(58f)` / 232px, 1:1 aspect ratio). The map title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons using vanilla Mindustry icons with stat counts (heart icon + likes opening details, `Icon.chatSmall` + comments opening details, `Icon.downloadSmall` + downloads triggering download, `Icon.play` triggering play). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview container occupies the upper area with a fixed square preview (`unit(58f)` / 232px) and title centered along the bottom, followed by a compact action button row

#### Scenario: Square aspect ratio preservation
- **WHEN** the map card is rendered in the grid or rescaled on small viewports
- **THEN** the preview card container height SHALL match the card width to maintain a 1:1 square aspect ratio
