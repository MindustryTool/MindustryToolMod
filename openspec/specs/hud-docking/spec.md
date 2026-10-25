# hud-docking Specification

## Purpose
TBD - created by archiving change hud-docking. Update Purpose after archive.
## Requirements
### Requirement: Dock target definition and registry protocol
The docking framework SHALL define a `DockTarget` abstraction representing dockable boundaries. Each target SHALL provide a unique string identifier, an axis-aligned bounding box `(x, y, width, height)` in screen coordinates, an active visibility state, and supported docking sides (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`, `INSIDE`). The framework SHALL provide a registry that allows dynamic registration, lookup, and spatial querying of dock targets.

#### Scenario: Registering and resolving dock targets
- **WHEN** a dock target with identifier `"minimap"` and valid bounding coordinates is registered
- **THEN** querying the registry for candidate targets within a proximity range returns the minimap target and its bounding box

#### Scenario: Inactive or hidden target query
- **WHEN** a registered target has visibility or active state false
- **THEN** spatial snap queries SHALL ignore the inactive target

### Requirement: Magnetic snap proximity and breakaway hysteresis
The `DockEngine` SHALL compute distance between the dragged HUD bounding box and registered `DockTarget` edges. When an edge comes within a snap threshold (default 16px), the engine SHALL snap the candidate position flush to the target edge with a configured gap offset. To break free from a snapped state, the user drag gesture MUST exceed a breakaway distance threshold (default 24px) from the snap point.

#### Scenario: Snapping to target edge when within threshold
- **WHEN** a user drags a HUD within 16px of a registered target's bottom edge
- **THEN** the HUD position snaps to align flush with the target's bottom edge plus gap offset

#### Scenario: Breakaway drag releases snap
- **WHEN** a user drags the handle more than 24px away from the active snap position
- **THEN** the snap lock is released and the HUD resumes free-floating movement

### Requirement: Visual snap cues with guide line and ghost silhouette
While an active drag is within snap threshold of a target, the docking system SHALL render visual docking cues. The cue SHALL include a translucent accent silhouette (`Pal.accent` at 0.25 alpha) showing the HUD's projected snapped bounding box and a solid 2px accent line along the shared docking seam.

#### Scenario: Visual cues rendered during snap hover
- **WHEN** a HUD is dragged into snap proximity of a dock target
- **THEN** a translucent silhouette box and an accent guideline appear at the snapped location until touch release or breakaway

#### Scenario: Visual cues removed on touch up or breakaway
- **WHEN** the drag gesture is released or dragged beyond breakaway distance
- **THEN** the visual ghost silhouette and guidelines are removed from rendering

### Requirement: Semantic anchor persistence and responsive re-alignment
When a HUD drag is released while snapped to a target, the docking system SHALL record a semantic anchor (`targetId`, `edge`, `offset`). When the screen resizes, orientation changes (portrait/landscape), or the target element moves, the system SHALL re-calculate and update the HUD coordinates to remain anchored to the target.

#### Scenario: Target repositioning moves anchored HUD
- **WHEN** a target element's screen bounds move due to a viewport resize
- **THEN** the anchored HUD automatically updates its position to stay aligned to the target edge

#### Scenario: Anchored target hidden fallback
- **WHEN** the anchored target element becomes hidden or removed from the stage
- **THEN** the HUD maintains its last resolved screen position and remains visible

### Requirement: Mindustry game UI target registry
The mod SHALL provide a `MindustryDockRegistry` that registers:
1. Screen edges (`TOP`, `BOTTOM`, `LEFT`, `RIGHT`) and corners
2. Vanilla Mindustry HUD elements: Minimap frame, Wave info / editor (`waves/editor`), Placement / block category bar (`blocks`), and Core items display
3. Active sibling mod HUDs (`TimeControlHud`, `TeamResourceHud`, `ArmyControlHud`, `QuickAccessHud`)
The registry SHALL periodically or reactively synchronize bounding rectangles with current game UI layouts.

#### Scenario: Snapping against vanilla minimap
- **WHEN** a user drags `TimeControlHud` near the bottom of the vanilla minimap
- **THEN** `TimeControlHud` snaps to the bottom edge of the minimap and stores an anchor to `"minimap"`

#### Scenario: Snapping against sibling mod HUD
- **WHEN** a user drags `TimeControlHud` near the right edge of `QuickAccessHud`
- **THEN** `TimeControlHud` snaps to align flush with the side of `QuickAccessHud`

