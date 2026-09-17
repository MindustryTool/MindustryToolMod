## Context

`AutoplayFeature.update()` currently implements manual override in two stages: on any touch or key press it yields control and arms `resumeTime = Time.time + cooldown * 60f`, then a second block keeps yielding until the cooldown expires. The cooldown length comes from the `override-cooldown` `ConfigValue` (slider 0.5s–5.0s, default 2.0s) rendered in the settings `globalSection`. The behavior predates the current instant-yield + transient-state-cleanup logic, which already makes takeover feel immediate; the timed lockout that follows is what players report as confusing.

## Goals / Non-Goals

**Goals:**
- Delete the `override-cooldown` setting, its UI, its bundle keys, and all `resumeTime` logic.
- Keep instant yield on manual input (controller reset + `resetUnitState`) exactly as today.
- Resume autoplay on the first frame with no input, with no timed lockout.

**Non-Goals:**
- Changing *what counts* as manual input (still any touch or key press; narrowing to movement intent is a separate change).
- Migrating or preserving already-persisted `override-cooldown` values.
- Touching any other autoplay task, setting, or the follow-unit toggle.

## Decisions

### 1. Pure deletion, no replacement behavior
- **Decision**: Remove the setting, state field, both `update()` blocks' cooldown halves, slider UI, bundle keys, and test assertion. No new code beyond closing the gaps (unused imports such as `Time` if it becomes unused).
- **Rationale**: The requested end state is "no cooldown", and the remaining yield path already stands alone — the trigger block keeps working with its first half intact.
- **Alternative considered**: Deprecating the setting while keeping the machinery defaulted to zero. Rejected: dead code with a hidden zero is worse than deletion, and the persisted value is inert once unread.

### 2. Keep the instant-yield half of the trigger block
- **Decision**: The `if (Core.input.isTouched() || ...)` block keeps yielding control, resetting transient state, and clearing `currentTask`; only the `resumeTime` arming lines go away, and the entire `if (Time.time < resumeTime)` block is deleted.
- **Rationale**: Yield-on-input is the desired behavior being preserved; only the lockout is removed.

### 3. Delete bundle keys, don't stub them
- **Decision**: Remove `feature.autoplay.settings.override-cooldown` and `.description` from `bundle.properties`.
- **Rationale**: No code references them afterward, and the project's i18n rule forbids orphan keys drifting from the UI. Other locales fall back to the default bundle, so nothing else needs editing.

## Risks / Trade-offs

- **[Risk] Players who relied on a long lockout to tinker mid-game lose that buffer** → *Mitigation*: Disabling the feature (quick access toggle) is the explicit, visible way to take over; instant resume is the documented new behavior.
- **[Risk] Rapid yield/resume flutter while input flickers (e.g. key repeat edge)** → *Mitigation*: Yield and resume are both idempotent and frame-local (controller assignment guarded by `!= Vars.player`, `currentTask` null-checked), so flutter costs nothing but a frame of AI each.
- **[Risk] Orphaned persisted `override-cooldown` values linger on devices** → *Mitigation*: `ConfigGroup` only reads registered keys; orphan data is never loaded and harmless.
