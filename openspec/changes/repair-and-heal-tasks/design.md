## Context

In `AutoplayFeature`, player units are controlled autonomously through an extensible priority-based task system (`AutoplayTask`). Currently, `RepairTask` (ID: `repair`) attempts to service both damaged friendly structures and damaged allied units. In practice, this causes units like `poly` to freeze:
1. `moveTo(target, healRange, 30f)` uses an aggressive deceleration band (`smooth = 30f`) with `keepDistance = true`. When the unit approaches `healRange`, the vector is zeroed or reversed, creating an oscillating dead zone before weapons can reliably engage.
2. Units with fixed weapons (`poly-weapon` has `rotate = false` and a 5° `shootCone`) fail to fire because `unit.lookAt(target)` was only invoked conditionally after already being within range, rather than continuously orienting the unit body towards the target.
3. Base structure repair and field unit healing are fundamentally distinct tactical behaviors. Players need to be able to enable/disable or re-prioritize repairing defenses versus tending to damaged mobile units.

## Goals / Non-Goals

**Goals:**
- Refactor `RepairTask` to exclusively repair damaged friendly buildings and blocks using vanilla Mindustry's `RepairAI` logic.
- Ensure flying units with fixed heal weapons (such as `poly`) properly approach to 65% of engagement range, naturally stop without counter-thrust jitter, orient toward the target building every tick, and fire heal weapons when in range.
- Create `HealUnitsTask` (ID: `heal-units`) as a distinct task dedicated to locating and healing damaged friendly units.
- Maintain Java 8 runtime compatibility and Solim UI declarative conventions.
- Provide complete internationalization for new display strings in `assets/bundles/bundle.properties` with descriptive translator comments.

**Non-Goals:**
- Altering manual player unit controls or overriding `Vars.player` outside of active autoplay.
- Reworking deconstruction or construction queuing (handled separately by `RebuildTask` and `SelfBuildTask`).
- Customizing weapon bullet mechanics or altering Mindustry game balance.

## Decisions

### 1. Replicating Vanilla `RepairAI` in `RepairTask`
- **Decision**: In `RepairAI.updateMovement()`, match Mindustry's native `mindustry.ai.types.RepairAI`:
  ```java
  if (target != null && target instanceof Building b && b.team == unit.team) {
      if (!target.within(unit, unit.type.range * 0.65f)) {
          moveTo(target, unit.type.range * 0.65f);
      }
      unit.lookAt(target);
  }
  ```
- **Rationale**: Vanilla Poly uses this exact pattern. Stopping `moveTo` when within `0.65f * range` prevents the controller's internal arrival/keepDistance logic from counter-thrusting or zeroing movement, while `unit.lookAt(target)` called every tick ensures the unit faces the block within `shootCone` (5°).
- **Alternative considered**: Adjusting `smooth` and `keepDistance` parameters on custom `moveTo`. Rejected because vanilla's conditional call is proven, robust, and handles all unit types identically to the base game.

### 2. Dedicated `HealUnitsTask` for Allied Mobile Units
- **Decision**: Introduce `HealUnitsTask` (ID: `heal-units`) positioned right after `RepairTask` in `DEFAULT_ORDER`.
- **Rationale**: Isolates fleet/unit recovery from structure repair. Units with healing abilities can be configured to prioritize saving damaged allied units over passive walls, or vice versa, according to user preference in `AutoplaySettingsDialog`.
- **Alternative considered**: Keeping combined logic in `RepairTask` with a boolean config. Rejected because the priority order system already provides clean reordering and enabling/disabling for tasks.

### 3. Capability Detection
- **Decision**: A unit is considered capable of healing if:
  `unit.type.canHeal || hasRepairField(unit) || hasRepairBeam(unit)`
- **Rationale**: Directly uses Mindustry's pre-computed `UnitType.canHeal` flag while supporting units with passive repair auras (`RepairFieldAbility`) or repair beam weapons (`RepairBeamWeapon`).

## Risks / Trade-offs

- **[Risk] Existing user configs have `task-order` saved without `heal-units`** → **Mitigation**: `AutoplayFeature.getEffectiveOrder()` already automatically appends any missing default task IDs from `DEFAULT_ORDER` into the active sequence, guaranteeing seamless migration without resetting user preferences.
- **[Risk] Target building dies or gets deconstructed during approach** → **Mitigation**: Target validation in `updateMovement` immediately clears `target` and yields if `b.dead()`, `!b.isValid()`, or health reaches `maxHealth()`.
- **[Risk] Fixed-weapon overshoot when moving fast** → **Mitigation**: Using `0.65f * range` allows ample deceleration room before reaching weapon minimums, and continuous `lookAt` ensures orientation aligns well before shooting range.
