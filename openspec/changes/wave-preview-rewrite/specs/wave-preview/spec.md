## ADDED Requirements

### Requirement: Parity wave composition

The system SHALL compute upcoming-wave unit counts from `Vars.state.rules.spawns`, skipping groups with null types and zero amounts, applying the campaign difficulty enemy-spawn multiplier (boss-effect groups truncated, others rounded, minimum 1), accumulating per `UnitType` exactly as the legacy implementation did.

#### Scenario: Next-wave counts match legacy formula

- **WHEN** the panel recomputes during a hosted wave game
- **THEN** per-type counts equal the legacy `getSpawned`-based computation including campaign scaling

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
