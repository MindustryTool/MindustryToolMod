## Why

The unreleased `reactor-alert` feature is better represented as **Grief Alert** (`grief-alert`) to clearly convey its anti-griefing purpose, and players responding to an alert need the exact tile coordinates of both the constructed explosive block and the affected core rather than a scalar distance so they can immediately locate the grief attempt on the map.

## What Changes

- **Rename feature from Reactor Alert to Grief Alert** (no backward compatibility or settings migration required as the feature has not reached production):
  - Rename package `mindustrytool.features.reactoralert` to `mindustrytool.features.griefalert` and classes `ReactorAlertFeature`, `ReactorAlertSettingsDialog`, and `ReactorAlertSettingsView` to `GriefAlertFeature`, `GriefAlertSettingsDialog`, and `GriefAlertSettingsView`.
  - Change feature metadata ID from `"reactor-alert"` to `"grief-alert"`, dialog name from `"reactorAlertSettingDialog"` to `"griefAlertSettingDialog"`, and per-block setting key prefix to `"mindustrytool.features.grief-alert.block."`.
  - Rename all `feature.reactor-alert.*` bundle keys across `assets/bundles/bundle*.properties` to `feature.grief-alert.*` with display name `Grief Alert` and settings title `Grief Alert Settings`.
- **Show target and core tile coordinates in the alert message**:
  - Replace the scalar tile distance in the alert message with the target block's tile coordinates `(targetX, targetY)` and the nearest team core's tile coordinates `(coreX, coreY)`:
    `Warning: {0} is building {1} at ({2}, {3}) near core ({4}, {5})!`

## Capabilities

### New Capabilities
- `grief-alert`: Warns when another player begins constructing an explosive reactor or generator within a configurable radius of a team core, reporting the builder, block name, target tile position, and core tile position.

### Modified Capabilities
- `reactor-alert`: Removed and replaced by `grief-alert`.

## Impact

- `mod/src/mindustrytool/features/reactoralert/` (removed) $\rightarrow$ `mod/src/mindustrytool/features/griefalert/` (`GriefAlertFeature.java`, `GriefAlertSettingsDialog.java`, `GriefAlertSettingsView.java`)
- `mod/src/mindustrytool/Main.java` (register `GriefAlertFeature`)
- `assets/bundles/bundle*.properties` (rename `feature.reactor-alert.*` keys to `feature.grief-alert.*` and update alert message format across all 8 bundle files)
- `openspec/specs/reactor-alert/spec.md` (replaced by `grief-alert`)
