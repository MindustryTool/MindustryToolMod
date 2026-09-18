## MODIFIED Requirements

### Requirement: Responsive Card Grid Layout
The system SHALL dynamically compute column count, content width, and capacity based on viewport dimensions (`dvw`, `dvh`), budgeting symmetrical scrollbar gutters on both sides of the card grid to ensure the scrollbar never clips cards or obscures action buttons.

#### Scenario: Screen orientation change on mobile
- **WHEN** mobile screen orientation changes from portrait to landscape
- **THEN** the grid SHALL recalculate column count from 1-2 columns to 2-3 columns without rebuilding unaffected card elements

#### Scenario: Touch-friendly targets on mobile
- **WHEN** rendered on mobile devices
- **THEN** all clickable buttons and card action triggers SHALL have a minimum touch target size of 40 units

#### Scenario: Symmetrical scrollbar gutter budgeting
- **WHEN** column count and content width are calculated for the browser grid
- **THEN** available width SHALL deduct both horizontal padding and symmetrical scrollbar gutters (`SCROLLBAR_GUTTER * 2`), and the rightmost column action buttons SHALL remain unobscured by the scrollbar track and knob

#### Scenario: Safe vertical overhead prevents spurious scrollbar
- **WHEN** available height is calculated for capacity and page sizing
- **THEN** the system SHALL subtract a safe vertical overhead of `unit(50f)` (200px), ensuring single-page item capacity does not exceed viewport height
