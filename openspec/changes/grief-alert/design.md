## Context

`ReactorAlertFeature` (`mindustrytool.features.reactoralert`) currently listens to `BlockBuildBeginEvent` and alerts the local player when another player (or team unit) starts constructing an explosive reactor or generator within a configurable radius of a team core. Because this feature has not yet shipped to production, we can rename it to **Grief Alert** (`grief-alert`) in place without maintaining legacy setting keys or bundle keys. Additionally, displaying only the distance in tiles makes it harder for players to immediately spot which core is threatened and where the explosive block is being built.

## Goals / Non-Goals

**Goals:**
- Rename `ReactorAlertFeature`, `ReactorAlertSettingsDialog`, and `ReactorAlertSettingsView` to `GriefAlertFeature`, `GriefAlertSettingsDialog`, and `GriefAlertSettingsView` under `mindustrytool.features.griefalert`.
- Update the feature metadata ID to `"grief-alert"`, the dialog element name to `"griefAlertSettingDialog"`, the per-block setting key prefix to `"mindustrytool.features.grief-alert.block."`, and all bundle keys from `feature.reactor-alert.*` to `feature.grief-alert.*`.
- Show the tile coordinates of both the constructed target block and the nearest core of the builder's team in the alert message instead of the scalar distance.

**Non-Goals:**
- Migrating or preserving old `reactor-alert` setting keys (feature has not been released to production).
- Expanding grief detection beyond explosive reactors and power generators in this change.

## Decisions

1. **Direct Rename Without Legacy Alias**
   - *Decision*: Replace `mindustrytool.features.reactoralert` with `mindustrytool.features.griefalert` and update all keys directly (`feature.grief-alert.*`, `"mindustrytool.features.grief-alert.block." + block.name`).
   - *Rationale*: The feature is pre-production, so keeping migration shims would add dead code with zero user value.

2. **Track Nearest `CoreBuild` Reference During Distance Scan**
   - *Decision*: In `GriefAlertFeature.onBuildBegin`, track both `float nearest` (in tiles) and `@Nullable CoreBuild nearestCore` while iterating `Vars.state.teams.cores(event.team)`. When `nearest <= radius`, read target tile coordinates from `event.tile.x` and `event.tile.y` and core tile coordinates from `nearestCore.tileX()` and `nearestCore.tileY()`.
   - *Rationale*: `event.tile` for a `ConstructBuild` is already the building's center tile, and `nearestCore.tileX()` / `nearestCore.tileY()` return the core's tile coordinates directly.

3. **Pass Tile Coordinates as Strings to `Core.bundle.format`**
   - *Decision*: Format the alert via:
     ```java
     Core.bundle.format("feature.grief-alert.alert",
             builderName + "[white]",
             block.localizedName,
             String.valueOf(event.tile.x),
             String.valueOf(event.tile.y),
             String.valueOf(nearestCore.tileX()),
             String.valueOf(nearestCore.tileY()));
     ```
     with bundle template:
     `feature.grief-alert.alert=Warning: {0} is building {1} at ({2}, {3}) near core ({4}, {5})!`
   - *Rationale*: Passing `String.valueOf(...)` instead of boxed `Number` values prevents `MessageFormat` from inserting locale-dependent thousand separators (e.g. `1,024`) on large maps while keeping individual `{2}`–`{5}` placeholders clear for translators.

## Risks / Trade-offs

- **[Risk] Missing bundle key updates across localized bundle files** $\rightarrow$ Mitigation: Update all 8 `assets/bundles/bundle*.properties` files and verify with `./gradlew checkBundles`.
