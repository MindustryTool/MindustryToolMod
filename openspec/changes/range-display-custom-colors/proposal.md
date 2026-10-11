## Why

In Mindustry battles, distinguishing defensive and utility building ranges at a glance is critical. Currently, all support buildings and turrets share the player's or building's team color, making it difficult to differentiate between offensive fire, healing/regeneration bubbles, overdrive boosts, and projectile interception ranges (such as Segment or Shockwave Tower). Providing standardized semantic colors (Orange for overdrives, Green for menders, White for projectile interception) along with fine-grained group toggles and a team-color fallback dramatically improves tactical readability.

## What Changes

- **Semantic Color Coding**:
  - Overdrives (`OverdriveProjector`) display in **Orange** (`Pal.accent`).
  - Menders and healing blocks (`MendProjector`, `RegenProjector`, `RepairTower`, `RepairTurret`) display in **Green** (`Pal.heal`).
  - Projectile defense blocks (`PointDefenseTurret` like Segment, and `ShockwaveTower`) display in **White** (`Color.white`).
- **Group Enable/Disable Configuration**:
  - Separate toggle for Overdrives (`draw-overdrive-range`, default: `true`).
  - Separate toggle for Menders & Repair Blocks (`draw-mender-range`, default: `true`).
  - Separate toggle for Projectile Defense (`draw-point-defense-range`, default: `true`).
- **Team Color Fallback Option**:
  - Setting option `use-team-color-for-special` (default: `false`) to allow users to force these special blocks to render in standard team colors instead of custom semantic colors.
- **Out-of-Ammo / Inoperable State Preservation**:
  - Blocks unable to function (such as turrets lacking ammo/power) still dim to `Color.gray`, ensuring status feedback is preserved.
- **UI Settings Integration**:
  - Expose the new toggles in `RangeDisplaySettingsView` using standard Solim WebStyles cards and controls.
  - Add i18n bundle entries with comments for all new user-facing strings.

## Capabilities

### New Capabilities
- `range-display-custom-colors`: Color mapping and visibility toggles for specialized tactical buildings (overdrives, menders, projectile defense) in the range display feature.

### Modified Capabilities
<!-- None -->

## Impact

- Affects `RangeDisplayFeature.java` (config options, cached frame values, block classification, and `drawBuildingRange` coloring).
- Affects `RangeDisplaySettingsView.java` (settings UI controls).
- Affects `bundle.properties` (translation keys).
- Affects tests in `RangeDisplayFeatureTest.java`.
