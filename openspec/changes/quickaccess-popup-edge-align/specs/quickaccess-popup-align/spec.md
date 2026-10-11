## ADDED Requirements

### Requirement: Popup edge alignment follows HUD half

The system SHALL horizontally align QuickAccess popups to the HUD edge based on HUD position: when the HUD bar center is on the left half of the stage, the popup left edge aligns to the HUD left edge; when on the right half (center at or beyond stage midpoint), the popup right edge aligns to the HUD right edge.

#### Scenario: HUD on left half left-aligns

- **WHEN** `barCenterX < stageW / 2`
- **THEN** `desiredX` equals `barX` (popup left edge at HUD left edge) before clamping.

#### Scenario: HUD on right half right-aligns

- **WHEN** `barCenterX >= stageW / 2`
- **THEN** `desiredX` equals `barX + barW - menuW` (popup right edge at HUD right edge) before clamping.

#### Scenario: Midpoint boundary is deterministic

- **WHEN** `barCenterX` exactly equals `stageW / 2`
- **THEN** the popup right-aligns (right-half branch wins).

### Requirement: Vertical placement unchanged

The system SHALL keep the existing vertical rule: popup above the HUD (`barY - menuH - gap`) when the HUD center is in the upper half, otherwise below (`barTop + gap`).

#### Scenario: Upper-half HUD opens above

- **WHEN** `barCenterY >= stageH / 2`
- **THEN** the popup anchors above the HUD regardless of left/right alignment.

#### Scenario: Lower-half HUD opens below

- **WHEN** `barCenterY < stageH / 2`
- **THEN** the popup anchors below the HUD regardless of left/right alignment.

### Requirement: Uniform helper behavior with stage safety

The system SHALL apply edge alignment uniformly inside `QuickAccessPopupHelper.show()` for all callers with no per-popup option, clamp the result inside the stage, and preserve the null-bar stage-center fallback.

#### Scenario: All popups share alignment

- **WHEN** any of GodMode, TimeControl, Autoplay, DisplayGroup, or SchematicGrid popups opens via the helper
- **THEN** the same left/right rule applies with no caller-specific override.

#### Scenario: Aligned popup stays on screen

- **WHEN** the edge-aligned position would overflow the stage
- **THEN** `x` is clamped to `[0, stageW - menuW]` (or `0` when `menuW >= stageW`) and `y` to `[0, stageH - menuH]` (or `0` when `menuH >= stageH`).

#### Scenario: Null bar falls back to stage center

- **WHEN** `quickAccessBar` is null
- **THEN** the popup anchors at stage center as today, with no left/right rule applied.
