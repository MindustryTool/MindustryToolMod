## Context

Autoplay controls the player unit autonomously by prioritizing tasks in user-configured order. Each task implements `AutoplayTask`, which defines an arbitration check (`update(Unit unit) -> boolean`) and exposes an `AIController` subclass (`BaseAutoplayAI`).

A crash occurred during `SelfHealTask.update(unit)` because it called `ai.moveTo()` before `ai.unit(unit)` was bound by `AutoplayFeature`. In addition, Mindustry's vanilla `AIController.moveTo()` does not null-check `this.unit`, assuming that controllers are always pre-bound to units before invoking navigation. A full audit identified similar latent null dereferences across other tasks (tile nulls, floor nulls, unhandled entity removals).

## Goals / Non-Goals

**Goals:**
- Eliminate all NPE crash hazards across `BaseAutoplayAI` and all 8 autoplay tasks.
- Enforce strict separation between arbitration (`task.update()`) and execution (`ai.updateMovement()`).
- Safeguard against invalid coordinates, missing tiles, and null floors.
- Ensure clean transient state transitions (e.g. stopping weapon fire or mining) when switching tasks or yielding control.

**Non-Goals:**
- Redesigning task prioritization or arbitration logic.
- Introducing new tasks or modifying existing AI pathfinding behavior.
- Replacing Mindustry's retained `AIController` architecture.

## Decisions

### 1. Guard BaseAutoplayAI against null units and targets
In `BaseAutoplayAI`, both `moveTo` overloads and delegating methods must explicitly check:
```java
if (unit == null || target == null) return;
```
*Rationale*: Mindustry's `AIController` accesses `this.unit.isFlying()` and `target.getX()` unconditionally. Intercepting this at the base class level provides universal protection across all current and future tasks.

### 2. Pure arbitration in `SelfHealTask.update()`
Remove `ai.moveTo(repair, 50f)` from `SelfHealTask.update()`.
*Rationale*: `SelfHealAI.updateMovement()` already implements moving toward the closest repair point when active. The call in `update()` was redundant and executed prematurely during arbitration.

### 3. Coordinate calculation without tile dereferences
In `SelfBuildTask` and `RebuildTask`, calculate world distances using `plan.x * Vars.tilesize` and `plan.y * Vars.tilesize` rather than `plan.tile()`. Guard `req.tile()` before calling `moveTo(req.tile(), ...)`.
*Rationale*: `plan.tile()` calls `Vars.world.tile(x, y)`, which returns `null` for off-map coordinates or ungenerated boundary sectors. Arithmetic coordinate conversion never throws.

### 4. Guard `unit.floorOn()` in `MiningTask`
Guard `unit.floorOn() != null` before inspecting floor properties (`isDuct`, `damageTaken`, `isDeep`) for boosting.
*Rationale*: Off-map flight or sector boundaries can return `null` from `floorOn()`, crashing ground/flying transition logic.

### 5. Centralized transient action cleanup in `AutoplayFeature`
When transitioning tasks, or when autoplay yields to manual override or disable:
- If previous task was `AttackTask`, ensure `unit.isShooting(false)` is set.
- If previous task was `MiningTask`, ensure `unit.mineTile = null` is set.
*Rationale*: Without explicit reset, a unit transitioning from attacking to fleeing/healing may continue firing weapons along its last aim vector.

## Risks / Trade-offs

- **[Risk] Movement skipped when unit/target is null** → *Mitigation*: Skipping movement for frames where `unit == null` or `target == null` is the intended behavior in Mindustry; the controller simply waits until the entity or position is valid.
- **[Risk] Performance impact of null checks** → *Mitigation*: Reference null checks (`== null`) have near-zero CPU overhead and avoid costly exception creation and stack unwinding.
