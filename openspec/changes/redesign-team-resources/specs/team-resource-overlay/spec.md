## ADDED Requirements

### Requirement: Minimalist Flow-Wrap Resource Display
The Team Resource overlay SHALL display core item indicators in a flow-wrapped layout without individual card backgrounds, borders, or excessive padding, allowing items to sit closely adjacent to one another.

#### Scenario: Borderless items wrap to next row
- **WHEN** multiple items are tracked and reach the horizontal width limit of the HUD
- **THEN** items wrap cleanly to the subsequent row without container card borders or margins

### Requirement: Uniform 3-Significant-Digit Amount Formatting
The system SHALL format resource amounts using at most 3 significant digits with standard SI unit suffixes (`999`, `1.2k`, `15k`, `150k`, `1.2M`) to guarantee uniform horizontal column widths.

#### Scenario: Resource values formatted compactly
- **WHEN** an item count is 100, 1200, 15000, 150000, or 1200000
- **THEN** the formatted amounts are displayed as `100`, `1.2k`, `15k`, `150k`, and `1.2M` respectively

### Requirement: Subscript Rate Indicator with Stagnant Suppression
The system SHALL render resource delta rates ($\Delta$/s) positioned at the bottom-right of the item amount in a reduced font size, colored green for positive rates, red for negative rates, and suppressed entirely when the rate is zero.

#### Scenario: Positive rate display
- **WHEN** core copper increases by 15 units per second
- **THEN** `+15` is displayed in green (`Pal.heal`) at the bottom-right of the amount text

#### Scenario: Negative rate display
- **WHEN** core lead decreases by 4 units per second
- **THEN** `-4` is displayed in red (`Pal.remove`) at the bottom-right of the amount text

#### Scenario: Stagnant rate suppression
- **WHEN** core titanium amount remains unchanged ($\Delta = 0$)
- **THEN** no rate text or rate icon is rendered for titanium

### Requirement: Ever-Existed Core Item Tracking
The system SHALL persistently track and display any item that has been present in the team's core at least once during the current match. Items that reach zero count SHALL remain visible on the HUD displaying `0`.

#### Scenario: Item persists when count drops to zero
- **WHEN** a team acquires silicon in the core and subsequently consumes all silicon so count becomes 0
- **THEN** silicon continues to be displayed in the resource overlay with amount `0`

#### Scenario: Unacquired items are omitted
- **WHEN** a match begins and thorium has never entered the team core
- **THEN** thorium is not rendered in the resource overlay

### Requirement: Unified Multi-Grid Power Bar
The system SHALL consolidate power network metrics into a single unified bar representing stored battery energy relative to total capacity. If multiple distinct power networks exist, the bar SHALL partition into contiguous sub-bars proportioned by network capacity.

#### Scenario: Multi-network partitioning
- **WHEN** a team has two separate power grids (Grid A with 80k capacity, Grid B with 20k capacity)
- **THEN** the bar displays two segments proportioned 80% and 20% separated by a subtle divider

#### Scenario: Power segment labeling
- **WHEN** a power segment width is sufficient to accommodate text
- **THEN** the segment renders `<Stored> ± <Net Balance>` (e.g. `15k +2.4k` or `20k -500`)

#### Scenario: Adaptive label suppression on narrow segments
- **WHEN** a power segment's pixel width is narrower than its text label plus padding
- **THEN** the text label is omitted for that segment while the fill color remains visible

#### Scenario: Discharge color feedback
- **WHEN** a power network's net balance is negative (discharging stored batteries)
- **THEN** the fill color for that network's sub-bar displays red (`Pal.remove`) instead of the standard power bar color

### Requirement: Minimalist Unit Count Display
The overlay SHALL display team units without individual card borders, rendering unit icons closely packed with unit count values positioned at the bottom-right.

#### Scenario: Unit count rendering
- **WHEN** active unit counts are enabled and a team controls 12 Daggers and 4 Monos
- **THEN** the overlay renders Dagger and Mono icons adjacent to each other with `12` and `4` at the bottom-right of their respective icons without card borders

### Requirement: Zero-Allocation 60-Second Hover History Sparklines
The system SHALL maintain a 60-second historical ring buffer sampled at 1Hz for each tracked resource amount and power network, and display a sparkline chart inside a tooltip when hovering over an item or the power bar.

#### Scenario: Resource hover sparkline tooltip
- **WHEN** the user hovers the cursor over a resource item
- **THEN** a tooltip appears showing the resource name, current exact count, rate, and a 60-second line graph of core amount over time with min/max bounds

#### Scenario: Power hover sparkline tooltip
- **WHEN** the user hovers over a power bar segment
- **THEN** a tooltip appears showing the network ID, stored power, capacity, net balance, and a 60-second line graph of stored energy over time

#### Scenario: Zero-allocation steady state sampling
- **WHEN** the 1Hz history update executes during normal gameplay
- **THEN** ring buffers record new values without allocating heap objects on the frame or timer thread
