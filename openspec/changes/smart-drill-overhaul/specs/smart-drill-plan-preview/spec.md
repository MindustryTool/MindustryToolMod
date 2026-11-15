# smart-drill-plan-preview Specification

## ADDED Requirements

### Requirement: Scenario definitions

The system SHALL let a scenario declare a floor of arbitrary size, a base floor type, rectangular ore deposits, scattered ore, and a wall band, without starting a game or loading content.

#### Scenario: Declaring a mixed scenario

- **WHEN** a scenario declares a base floor, a rectangular copper deposit, scattered lead, and a wall band
- **THEN** the scenario can be constructed and handed to the planner without a running game

#### Scenario: Multiple scenarios in one suite

- **WHEN** several scenarios are registered
- **THEN** each is planned and rendered independently

### Requirement: Floor image

The system SHALL render one image per scenario showing the floor, the ore deposits and the wall band at one pixel per tile, scaled up for review.

#### Scenario: Floor image distinguishes tile kinds

- **WHEN** a scenario contains stone, target ore, other ore and wall ore
- **THEN** each kind is drawn in a distinguishable colour in the floor image

### Requirement: Plan image

The system SHALL render one image per scenario showing the floor beneath the produced plan, with each planned block drawn in a colour that identifies its kind.

#### Scenario: Planned blocks are visible over the floor

- **WHEN** a plan contains drills, bridges and a stub
- **THEN** each is drawn in its own colour on top of the floor image

#### Scenario: Empty plan still renders

- **WHEN** a scenario produces no plan
- **THEN** the plan image is still written, showing only the floor

### Requirement: Output location and generation

The system SHALL write every image to `mod/build/smartdrill-preview/` and SHALL regenerate all images on every test run.

#### Scenario: Images are written on each run

- **WHEN** the scenario suite runs
- **THEN** every scenario's floor image and plan image exist under `mod/build/smartdrill-preview/` with deterministic names

### Requirement: Asserted invariants

Each scenario SHALL assert the invariants that must hold for any correct plan: every drill footprint contains target ore, every drill footprint is pure target ore, no two footprints overlap, and every planned position lies inside the rectangle.

#### Scenario: Footprint purity

- **WHEN** a plan is produced
- **THEN** every drill footprint contains at least one target ore tile and no tile of any other ore

#### Scenario: No overlapping footprints

- **WHEN** a plan is produced
- **THEN** no two planned drills share a footprint tile

#### Scenario: Positions stay inside the rectangle

- **WHEN** a plan is produced
- **THEN** every planned drill lies inside the target rectangle

### Requirement: Exact counts are not asserted

The scenario suite SHALL NOT assert exact drill or bridge counts, because the images are the review artifact and counts are an implementation detail.

#### Scenario: Strategy retuning does not break the suite

- **WHEN** a placement strategy is tuned so that a scenario yields a different number of drills
- **THEN** the suite still passes as long as the invariants hold and the images are regenerated