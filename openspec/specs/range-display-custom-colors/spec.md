# range-display-custom-colors Specification

## Purpose

Semantic range colors and independent per-category visibility controls for tactical support and defense buildings (overdrives, menders, point defense) in the range display feature, with an optional team-color override and preserved out-of-ammo dimming. Created by archiving change range-display-custom-colors.

## Requirements

**Source: range-display-custom-colors**

### Requirement: Semantic Range Colors for Support and Defense Blocks
The range display feature SHALL render range indicators using semantic colors for designated tactical support and defense blocks when custom colors are active:
- Overdrives (`OverdriveProjector`) SHALL be rendered in Orange (`Pal.accent`).
- Menders and healing blocks (`MendProjector`, `RegenProjector`, `RepairTower`, `RepairTurret`) SHALL be rendered in Green (`Pal.heal`).
- Point-defense turrets (`PointDefenseTurret`, including Segment) and `ShockwaveTower` SHALL be rendered in White (`Color.white`).

#### Scenario: Rendering overdrive with custom colors
- **WHEN** an Overdrive Projector is rendered and team color override is disabled
- **THEN** its range circle is drawn with the orange color (`Pal.accent`)

#### Scenario: Rendering mender with custom colors
- **WHEN** a Mend Projector or Regen Projector is rendered and team color override is disabled
- **THEN** its range boundary is drawn with the green color (`Pal.heal`)

#### Scenario: Rendering Segment turret with custom colors
- **WHEN** a Segment turret has power/ammo and team color override is disabled
- **THEN** its range circle is drawn with white color (`Color.white`)

#### Scenario: Out of ammo turret dimming
- **WHEN** a turret (including Segment) lacks ammo or required resources to fire
- **THEN** its range indicator is rendered in gray (`Color.gray`) regardless of custom color mappings

### Requirement: Team Color Override Option
The range display feature SHALL provide an option (`use-team-color-for-special`) defaulting to `false`. When enabled, special support and defense blocks SHALL use their respective team color (`build.team.color`) instead of custom semantic colors.

#### Scenario: Team color override enabled
- **WHEN** `use-team-color-for-special` is set to `true`
- **THEN** Overdrives, Menders, and Point Defense turrets are drawn using `build.team.color`

### Requirement: Independent Category Visibility Controls
The range display feature SHALL provide separate configuration toggles to enable or disable range visualization for:
- Overdrives (`draw-overdrive-range`, default: `true`)
- Menders and healing blocks (`draw-mender-range`, default: `true`)
- Point defense blocks (`draw-point-defense-range`, default: `true`)

#### Scenario: Overdrive range disabled
- **WHEN** `draw-overdrive-range` is toggled to `false`
- **THEN** range boundaries for Overdrive Projectors are not rendered

#### Scenario: Point defense range disabled
- **WHEN** `draw-point-defense-range` is toggled to `false`
- **THEN** range boundaries for Segment and Shockwave Tower are not rendered

### Requirement: Settings UI and Localization
The range display settings view SHALL expose controls for toggling overdrive ranges, mender ranges, point defense ranges, and team color override, with localized labels and descriptions in `bundle.properties`.

#### Scenario: Interacting with range settings dialog
- **WHEN** user opens the Range Display settings dialog
- **THEN** switches for Overdrives, Menders, Projectile Defense, and Team Color Override are visible and reactive