## ADDED Requirements

### Requirement: Total wave health presentation

The system SHALL compute the total health of each upcoming wave by summing `(unit.amount * unit.type.health)` across all unit entries in that wave, and SHALL display this total health formatted compactly (integers under 1,000; `k` suffix for 1,000–999,999; `M` suffix for 1,000,000+) inline within the wave header title. Empty waves with zero units or zero health SHALL omit the health display.

#### Scenario: Total health aggregated across domains
- **WHEN** an upcoming wave has ground, air, and naval units
- **THEN** total wave health equals the sum of `(amount * type.health)` for all units across all three domains

#### Scenario: Compact formatted HP displayed inline
- **WHEN** an upcoming wave has non-zero total health
- **THEN** the wave section header displays the wave number and formatted HP inline (e.g. `Wave 15 (45.2k HP)` in multi-wave mode or `15 (45.2k HP)` in single-wave mode)

#### Scenario: Empty wave omits HP
- **WHEN** an upcoming wave produces zero units across all domains
- **THEN** the wave header displays the wave number without an HP suffix, and shows the empty status indicator

### Requirement: Interactive collapse toggle

The system SHALL allow the player to click anywhere on the wave preview panel to toggle between an expanded view (rendering all configured lookahead wave sections) and a collapsed view (rendering only the immediate upcoming wave). When the configured lookahead depth is 1 or only 1 wave is available, clicking SHALL have no effect. The collapsed state SHALL be transient in memory and SHALL reset to expanded whenever a new world or game loads.

#### Scenario: Clicking panel toggles to single wave
- **WHEN** lookahead depth is greater than 1 and the preview is expanded
- **THEN** clicking anywhere on the wave preview panel collapses the view to render only the immediate upcoming wave

#### Scenario: Clicking collapsed panel restores full lookahead
- **WHEN** the preview is collapsed to 1 wave
- **THEN** clicking anywhere on the wave preview panel expands the view back to the full lookahead depth

#### Scenario: Toggling disabled when depth is 1
- **WHEN** the configured lookahead depth is 1
- **THEN** clicking the panel does not change the view and exactly 1 wave remains displayed

#### Scenario: Reset on world load
- **WHEN** a new world loads or map transition occurs
- **THEN** the collapsed state resets to false and full configured lookahead depth is displayed
