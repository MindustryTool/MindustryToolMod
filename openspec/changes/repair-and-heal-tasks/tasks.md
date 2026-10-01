## 1. Internationalization & Assets

- [ ] 1.1 Add bundle keys and descriptive comments for `HealUnitsTask` (`feature.autoplay.task.heal-units`, `feature.autoplay.status.no-damaged-allies`, `feature.autoplay.status.healing-allies`) in `assets/bundles/bundle.properties`
- [ ] 1.2 Configure icon for `HealUnitsTask` with fallback to native Mindustry icons

## 2. Refactor RepairTask for Buildings

- [ ] 2.1 Update target search in `RepairTask.update(Unit unit)` to focus exclusively on damaged friendly buildings using `Units.findDamagedTile` and fallback ally search, checking capability via `unit.type.canHeal`, `RepairBeamWeapon`, or `RepairFieldAbility`
- [ ] 2.2 Refactor `RepairTask.RepairAI.updateMovement()` to only call `moveTo` when outside 65% of weapon range (`unit.type.range * 0.65f`), eliminating deceleration oscillation
- [ ] 2.3 Implement continuous `unit.lookAt(target)` orientation in `RepairAI` every tick so fixed weapon mounts (such as Poly's missile mounts) align within their narrow 5° firing cone
- [ ] 2.4 Aim and fire heal weapons via `unit.aim(target)` and `unit.controlWeapons(inRange)` when the building is within `unit.type.range`

## 3. Implement HealUnitsTask for Allied Units

- [ ] 3.1 Create `HealUnitsTask` (ID: `heal-units`) implementing `AutoplayTask`, evaluating unit healing capability and targeting the closest damaged friendly mobile unit
- [ ] 3.2 Implement `HealUnitsTask.HealUnitsAI` with safe approach distance, continuous orientation, and weapon engagement for mobile targets

## 4. Integration & Wiring

- [ ] 4.1 Register `HealUnitsTask` in `AutoplayFeature` and insert `HealUnitsTask.ID` immediately after `RepairTask.ID` in `DEFAULT_ORDER`
- [ ] 4.2 Ensure task priority synchronization, settings reorder, and HUD/popup task strip smoothly handle the 9 tasks

## 5. Verification

- [ ] 5.1 Compile the project with Gradle to verify Java 8 compatibility and zero classpath/syntax errors
- [ ] 5.2 Run automated test suite to verify no regressions in Autoplay or Solim systems
