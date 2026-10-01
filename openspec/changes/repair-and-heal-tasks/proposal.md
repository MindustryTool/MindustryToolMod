## Why

In `AutoplayFeature`, the existing `RepairTask` attempts to handle both damaged structures and damaged allied units in a single task, leading to stalled unit movement, oscillation due to aggressive braking smoothing (`smooth = 30f`), and failure to shoot at damaged blocks with fixed weapons like Poly's missile mounts (which require continuous orientation within a narrow 5° `shootCone`). Furthermore, players cannot independently prioritize base structure maintenance versus field fleet healing.

Separating block repair and unit healing into two dedicated tasks and aligning block repair with vanilla Mindustry's native `RepairAI` (`poly` behavior) restores reliable automatic repairing and provides clean tactical control.

## What Changes

- **Refactor `RepairTask` (ID: `repair`)**: Focus exclusively on repairing damaged friendly buildings/blocks across the map using `Units.findDamagedTile`.
- **Adopt Vanilla Poly `RepairAI` Mechanics**:
  - Check healing capabilities using `unit.type.canHeal`, `RepairBeamWeapon`, or `RepairFieldAbility`.
  - Only invoke `moveTo(target, unit.type.range * 0.65f)` when outside 65% of weapon range, letting the unit naturally hover in place once in range without deceleration oscillation.
  - Continuously rotate the unit towards the target building via `unit.lookAt(target)` every frame to satisfy fixed weapon `shootCone` constraints.
  - Control weapons and aim directly when within `unit.type.range`.
- **Add `HealUnitsTask` (ID: `heal-units`)**: A new dedicated autoplay task targeting damaged allied units within search range, moving into healing range and firing healing weapons or utilizing repair auras.
- **Update Task Hierarchy & Order**:
  - Insert `HealUnitsTask` immediately after `RepairTask` in the default task priority order (`SelfHeal -> Flee -> Attack -> Repair -> HealUnits -> FollowAssist -> SelfBuild -> Rebuild -> Mine`).
  - Update `AutoplayFeature` default lists and task registration.
- **i18n & Assets**:
  - Add bundle keys with descriptive translator comments for `HealUnitsTask` (name, status messages).
  - Add an appropriate icon for unit healing.

## Capabilities

### New Capabilities
- `heal-units-task`: Dedicated autonomous task that identifies, approaches, and heals damaged allied mobile units using healing weapons and repair fields.

### Modified Capabilities
- `autonomous-gameplay-ai`: Refactor Task 4 (`RepairTask`) to be exclusively building-focused with vanilla `RepairAI` movement and firing mechanics, update task order from 8 to 9 tasks, and update strip/settings integration.

## Impact

- `mindustrytool.features.autoplay.tasks.RepairTask`: Refactored to mirror vanilla `RepairAI` for buildings only.
- `mindustrytool.features.autoplay.tasks.HealUnitsTask`: New task class implementing `AutoplayTask`.
- `mindustrytool.features.autoplay.AutoplayFeature`: Register new task, update `DEFAULT_ORDER`.
- `assets/bundles/bundle.properties`: New translation keys for task name and status.
