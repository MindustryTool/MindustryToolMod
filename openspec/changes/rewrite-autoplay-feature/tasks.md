## 1. Feature & Core Engine Setup

- [x] 1.1 Create `AutoplayFeature.java` extending `Feature` with `enabledByDefault = false` and remove `.development(true)`.
- [x] 1.2 Define `ConfigValue<T>` fields in `AutoplayFeature` for `followUnit`, `overrideCooldown`, `taskOrder`, and `disabledTasks`.
- [x] 1.3 Add all required i18n translation keys with descriptive comments directly above them to `assets/bundles/bundle.properties`.
- [x] 1.4 Implement player input override detection and cooldown timer (pausing autoplay during user input and for the configured duration after release).
- [x] 1.5 Implement overhead rendering in `Trigger.draw` (rendering current task icon and dashed line to target objective).

## 2. Autoplay Tasks & AI Logic

- [x] 2.1 Define `AutoplayTask` interface and `BaseAutoplayAI` with movement helpers and target position tracking.
- [x] 2.2 Implement `SelfHealTask` with repair point existence check, configurable HP threshold slider, and safe fallback.
- [x] 2.3 Implement `FleeTask` with HP threshold / unarmed trigger, enemy unit and turret threat evaluation, and retreat towards allied core/base.
- [x] 2.4 Implement `AttackTask` with automatic unarmed check, dynamic range detection, building/unit targeting, and kiting behavior.
- [x] 2.5 Implement `RepairTask` supporting healing weapons, repair beams, and build beams to fix damaged structures and heal allied units.
- [x] 2.6 Implement `FollowAssistTask` supporting building, attacking, and mining assistance with player selection.
- [x] 2.7 Implement `SelfBuildTask` with closest-plan prioritization, core material validation to prevent deadlocks, and "Deconstruct All Derelicts" utility button.
- [x] 2.8 Implement `RebuildTask` with closest-plan prioritization and core material validation.
- [x] 2.9 Implement `MiningTask` with core capacity checking, item selection filter, and direct core transfer via `Call.transferInventory`.

## 3. Solim UI & Dialogs

- [x] 3.1 Create `AutoplaySettingsDialog.java` extending `SolimDialog`.
- [x] 3.2 Create `AutoplaySettingsView.java` using declarative Solim UI with reorderable task list (Up/Down buttons), enable toggles, live status labels, and task settings expandable panels.
