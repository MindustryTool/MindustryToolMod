# wave-preview Specification

## Purpose
Preview upcoming wave unit compositions directly on the HUD with domain-split units and configurable lookahead.
## Requirements
### Requirement: Parity wave composition

The system SHALL compute upcoming-wave unit counts from `Vars.state.rules.spawns`, skipping groups with null types and zero amounts, applying the campaign difficulty enemy-spawn multiplier (boss-effect groups truncated, others rounded, minimum 1), multiplying each group's spawned count by the number of active map spawn points that match the group (`group.spawn == -1` matches all active dropzone tiles or attack-mode wave cores; `group.spawn != -1` matches only a spawn point with the exact matching tile position), and accumulating total unit counts per `UnitType`.

#### Scenario: Spawner count scales unit amounts
- **WHEN** the map has multiple active dropzones and a spawn group specifies `group.spawn == -1`
- **THEN** the computed unit count for that group equals the base spawned amount multiplied by the total number of active dropzones

#### Scenario: Targeted spawner matching
- **WHEN** a spawn group specifies a specific `group.spawn` position
- **THEN** the computed unit count equals the base spawned amount if that position matches an active dropzone/core, or zero if no matching dropzone/core exists

#### Scenario: Attack mode core spawning fallback
- **WHEN** the map has zero dropzone tiles, but rules specify `wavesSpawnAtCores` in attack mode with active enemy cores
- **THEN** units scale by the number of active enemy cores matching the spawn group

#### Scenario: Zero spawners yields zero units
- **WHEN** the map has zero dropzones and no attack-mode core spawning
- **THEN** zero units are accumulated for the upcoming wave

#### Scenario: Null and empty groups skipped
- **WHEN** spawn groups carry null types or compute zero amounts
- **THEN** they contribute nothing to the displayed composition

### Requirement: Injected panel placement

The system SHALL render the panel inside the vanilla `waves/editor` → `waves` table on enable and world load, with defensive null handling that logs an error and skips injection when either node is absent.

#### Scenario: Panel injects on enable

- **WHEN** the feature is enabled with the vanilla waves nodes present
- **THEN** the panel row is added to the waves table and populated

#### Scenario: Missing vanilla nodes handled

- **WHEN** the waves/editor or waves node cannot be found
- **THEN** an error is logged and no injection is attempted

#### Scenario: Panel detaches on disable

- **WHEN** the feature is disabled
- **THEN** the injected panel is removed and disposed

### Requirement: Event-driven refresh

The system SHALL recompute composition on world load and on wave advance with no periodic polling loop, and SHALL show the panel only while `Vars.ui.hudfrag.shown` is true, `Vars.state.isGame()` is true, and `Vars.state.rules.waves` is true.

#### Scenario: Composition refreshes per wave

- **WHEN** the game advances to a new wave
- **THEN** the panel recomputes and displays the newly upcoming composition

#### Scenario: Panel hidden without waves

- **WHEN** the map rules disable waves or the game is not active
- **THEN** the panel is not visible

### Requirement: Domain sections in wave-major layout

The system SHALL group displayed units into ground, air, and naval sections with units matching none of these rendered in the ground section, arranged wave-major with one section per upcoming wave containing its domain rows, each domain row keeping health-sorted icon×count presentation.

#### Scenario: Domains split per wave

- **WHEN** an upcoming wave mixes ground, air, and naval units
- **THEN** each domain renders its own icon×count rows inside that wave's section

#### Scenario: Unmatched units fall to ground

- **WHEN** a unit type matches neither flying nor naval classification
- **THEN** it renders in the ground section

### Requirement: Lookahead depth

The system SHALL persist a lookahead-depth setting (integer, default 1, maximum 5) controlling how many future waves render as consecutive wave-major sections, with depth 1 reproducing the legacy single-wave display.

#### Scenario: Default matches legacy display

- **WHEN** the depth setting has never been changed
- **THEN** exactly the next wave renders

#### Scenario: Depth widens preview

- **WHEN** the user raises depth to 3
- **THEN** the next three waves render as consecutive sections

#### Scenario: Depth persists

- **WHEN** the user changes depth and the game restarts
- **THEN** the panel still renders that many waves

### Requirement: Minimal settings dialog

The system SHALL expose a settings dialog with opacity, scale, and lookahead-depth rows bound declaratively to persisted config entries, with no position reset control.

#### Scenario: Appearance settings apply live

- **WHEN** the user adjusts opacity or scale
- **THEN** the injected panel updates without re-injection

### Requirement: Enabled by default with rewritten copy

The system SHALL default WavePreview to enabled and SHALL resolve name, description, and help from the bundle with translator comments, where help describes real usage instead of the legacy cannot-be-enabled wording.

#### Scenario: Updaters see the panel

- **WHEN** the mod updates with no prior WavePreview state
- **THEN** the feature is enabled and its help no longer claims it cannot be enabled

### Requirement: Empty wave state presentation

The system SHALL display a localized empty wave status indicator when an upcoming wave has zero total units across all domains, including when the map has zero active spawn points.

#### Scenario: Zero spawners shows empty indicator
- **WHEN** an upcoming wave has zero active dropzones or spawn points and no attack core spawning
- **THEN** the wave section displays a localized empty status label instead of blank space

#### Scenario: Zero units computed shows empty indicator
- **WHEN** an upcoming wave has active spawners but all spawn groups produce zero units
- **THEN** the wave section displays a localized empty status label

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

