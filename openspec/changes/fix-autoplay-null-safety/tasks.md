## 1. Base AI & Arbitration Fixes

- [x] 1.1 Add null guards for `unit == null` and `target == null` / `pos == null` across `BaseAutoplayAI` movement methods (`moveTo`, `circle`)
- [x] 1.2 Remove premature `ai.moveTo(repair, 50f)` call from `SelfHealTask.update()` to ensure pure arbitration

## 2. Task-Specific Null Hardening

- [x] 2.1 Update `SelfBuildTask` and `SelfBuildAI` to use tile coordinate distance calculations and guard against null `req.tile()`
- [x] 2.2 Update `RebuildTask` and `RebuildAI` to guard `unit.team.data()` and null `req.tile()`
- [x] 2.3 Update `MiningTask` to null-check `unit.floorOn()` before querying terrain properties for boosting
- [x] 2.4 Update `FollowAssistTask` to guard against null `plan.tile()` and null `mineTile.drop()`
- [x] 2.5 Update `AttackTask` and `FleeTask` to verify target entity addition/validity (`isAdded()`)

## 3. Autoplay Lifecycle & State Transitions

- [x] 3.1 Update `AutoplayFeature.update()` to validate `unit.isValid()` and clean up transient unit states (`unit.isShooting(false)`, clearing mine target) when switching tasks or yielding control

## 4. Verification & Testing

- [ ] 4.1 Add unit tests in `AutoplayFeatureTest` verifying `BaseAutoplayAI` null safety when `unit` or `target` is null
- [ ] 4.2 Add unit tests verifying `SelfHealTask.update(unit)` runs safely without movement side-effects or unattached AI crashes
- [ ] 4.3 Run test suite via `./gradlew test` to ensure zero regressions
