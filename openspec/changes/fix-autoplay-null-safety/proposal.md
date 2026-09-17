## Why

Autoplay crashed with a `NullPointerException` in `AIController.moveTo()` because `SelfHealTask.update()` invoked `ai.moveTo()` during task arbitration before `ai.unit(unit)` was set. Furthermore, auditing the autoplay codebase revealed multiple latent null dereferences across `BaseAutoplayAI` (missing guards on null `unit` and `target`), `SelfBuildTask` and `RebuildTask` (dereferencing `plan.tile()` on off-map coordinates), `MiningTask` (dereferencing `unit.floorOn()` off-map), and unmanaged transient unit states (such as firing or mining persisting across task switches).

Hardening null safety and state lifecycle across all 8 tasks and the base controller ensures rock-solid autonomous gameplay under all map conditions, spectator states, respawn cycles, and off-map coordinates.

## What Changes

- **Fix `SelfHealTask` arbitration crash**: Remove illegal `ai.moveTo()` call from `SelfHealTask.update()`; delegate movement strictly to `SelfHealAI.updateMovement()`.
- **Harden `BaseAutoplayAI` movement**: Add null guards for `unit == null` and `target == null` / `pos == null` in all `moveTo` and movement delegates before delegating to `AIController`.
- **Safe coordinate lookups in `SelfBuildTask` & `RebuildTask`**: Compute distances using world coordinates (`x * Vars.tilesize, y * Vars.tilesize`) rather than `plan.tile()`, and guard against null `req.tile()` in movement.
- **Null-safe floor checking in `MiningTask`**: Guard `unit.floorOn() != null` before checking floor flags (duct, damage, deep floor) when deciding unit boosting.
- **Null-safe assist targets in `FollowAssistTask`**: Guard `plan.tile()` and `mineTile.drop()` null checks.
- **Target validity in `AttackTask` & `FleeTask`**: Validate that target entities are not only alive (`!dead()`) but currently active/added (`isAdded()`).
- **Autoplay lifecycle & state cleanup**:
  - Check `unit != null && unit.isValid()` instead of just `unit.dead()`.
  - Reset transient unit states (e.g. `unit.isShooting(false)`, clearing mine tile) when switching active tasks or when player overrides / disables autoplay.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `autonomous-gameplay-ai`: Strengthen null safety across task arbitration, base AI movement, coordinate resolution, and task transition lifecycle.

## Impact

- `mindustrytool.features.autoplay.AutoplayFeature`
- `mindustrytool.features.autoplay.tasks.BaseAutoplayAI`
- `mindustrytool.features.autoplay.tasks.SelfHealTask`
- `mindustrytool.features.autoplay.tasks.SelfBuildTask`
- `mindustrytool.features.autoplay.tasks.RebuildTask`
- `mindustrytool.features.autoplay.tasks.MiningTask`
- `mindustrytool.features.autoplay.tasks.FollowAssistTask`
- `mindustrytool.features.autoplay.tasks.AttackTask`
- `mindustrytool.features.autoplay.tasks.FleeTask`
- Unit tests in `AutoplayFeatureTest`
