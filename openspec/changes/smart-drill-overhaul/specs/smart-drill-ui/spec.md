# smart-drill-ui Specification

## ADDED Requirements

### Requirement: Compact docked panel

The system SHALL present one docked panel for the whole planning flow, replacing the previous two-step popup of a direction pad followed by a drill grid. The panel SHALL be docked to the bottom-left of the HUD and SHALL be built from declarative Solim components.

#### Scenario: Single panel for the whole flow

- **WHEN** the player taps an ore tile with the feature enabled
- **THEN** one panel opens showing the target, the facing row, the area readout, the drill row and the action row together

#### Scenario: Panel is declarative

- **WHEN** the panel is constructed
- **THEN** it is composed of Solim components and does not build a raw `arc.scene` table

### Requirement: Panel rows are conditional on the target

The system SHALL show the facing row for every target, SHALL show the drill row only with drills valid for the target ore, and SHALL show the rig row only when the highlighted drill has bound schematics.

#### Scenario: Beam drill target

- **WHEN** the target is a wall ore
- **THEN** the drill row offers only beam drills and the facing row controls the direction they face

#### Scenario: No suitable drill

- **WHEN** no unlocked drill can mine the target ore
- **THEN** the drill row is empty and the panel reports that no drill is available

#### Scenario: Drill with no bound schematic

- **WHEN** the highlighted drill has no bound schematic
- **THEN** the rig row is not shown

### Requirement: Facing row

The system SHALL provide a four-direction facing row that sets both the bridge chain exit direction and, for beam drills, the direction the bodies face.

#### Scenario: Changing facing re-plans

- **WHEN** the player picks a different facing direction
- **THEN** the plan is recomputed with the bridge chain exiting in that direction

### Requirement: Draggable coverage rectangle

The system SHALL draw the target rectangle in the world and SHALL allow it to be resized only by dragging its edge handles, so that dragging elsewhere continues to pan the camera. The plan SHALL be recomputed when a drag is released.

#### Scenario: Dragging an edge handle

- **WHEN** the player drags an edge handle
- **THEN** the rectangle resizes along that axis and the plan is recomputed on release

#### Scenario: Dragging outside the handles

- **WHEN** the player drags anywhere other than an edge handle
- **THEN** the camera pans and the rectangle is unchanged

#### Scenario: No size ceiling

- **WHEN** the player keeps expanding the rectangle
- **THEN** expansion is never refused for being too large

### Requirement: Auto-detected initial rectangle

The system SHALL open with the rectangle auto-detected from the target ore: the bounding box of the flooded region, clamped to 10x10 tiles.

#### Scenario: Auto-detect on open

- **WHEN** the panel opens on a fresh target
- **THEN** the rectangle matches the clamped bounding box of the flooded ore

### Requirement: Live plan readout

The system SHALL show the current drill count and the efficiency readout for the current rectangle and selection, and SHALL warn when the drill count is large.

#### Scenario: Readout updates after resize

- **WHEN** the rectangle changes
- **THEN** the drill count and efficiency readout reflect the new rectangle

#### Scenario: Large plan warning

- **WHEN** the drill count exceeds the warning threshold
- **THEN** a warning is shown and `Place` remains available

### Requirement: Ghost preview

The system SHALL draw the current plan as a ghost preview in the world without adding anything to the player's build queue. The ghost SHALL be culled to the camera view.

#### Scenario: Nothing is queued

- **WHEN** a plan exists and the player has not pressed `Place`
- **THEN** no build plan is present in the player's build queue

#### Scenario: Off-screen plans are skipped

- **WHEN** a planned tile lies outside the camera view bounds
- **THEN** no sprite is drawn for it

#### Scenario: Ghost pass is guarded

- **WHEN** the feature is disabled
- **THEN** the draw handler returns before reading any state

### Requirement: Place and Cancel

The system SHALL provide an explicit `Place` action that converts the ghost preview into real build plans, and a `Cancel` action that discards them.

#### Scenario: Placing

- **WHEN** the player presses `Place`
- **THEN** every planned build is added to the player's build queue and the panel closes

#### Scenario: Cancelling

- **WHEN** the player presses `Cancel`
- **THEN** no build plan is added and the panel closes

### Requirement: Taps are ignored while the panel is open

The system SHALL ignore world taps while the panel is open, except for drags that begin on a rectangle edge handle.

#### Scenario: Tapping another ore tile

- **WHEN** the player taps a different ore tile while the panel is open
- **THEN** the target does not change and the panel stays open

#### Scenario: Retargeting

- **WHEN** the player wants a different target
- **THEN** they close the panel and tap the new tile

### Requirement: Auto-disable after placing

The system SHALL disable the feature after a successful `Place` when the `autoDisable` setting is enabled.

#### Scenario: Auto-disable enabled

- **WHEN** the player presses `Place` and `autoDisable` is on
- **THEN** the feature is disabled after the plans are queued

#### Scenario: Auto-disable disabled

- **WHEN** the player presses `Place` and `autoDisable` is off
- **THEN** the feature remains enabled

### Requirement: Removed maxTiles setting

The system SHALL NOT expose a maximum-tile setting. The settings view SHALL offer only the `autoDisable` toggle and a reset action.

#### Scenario: Settings dialog contents

- **WHEN** the player opens the feature settings dialog
- **THEN** no maximum-tile slider is present and the rectangle is the only bound on the plan size