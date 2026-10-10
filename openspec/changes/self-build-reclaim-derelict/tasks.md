## 1. SelfBuildTask Derelict Reclaim & Affordability Logic

- [ ] 1.1 Update `SelfBuildTask.canAffordPlan` in `mod/src/mindustrytool/features/autoplay/tasks/SelfBuildTask.java` to treat `plan.isDerelictRepair()`, `plan.isRotation(unit.team)`, `Vars.state.rules.infiniteResources`, and `unit.team.rules().infiniteResources` as affordable (`true`) without requiring core items
- [ ] 1.2 Implement `reclaimDerelicts()` and coordinate-based plan replacement in `SelfBuildTask.java` so clicking **Reclaim All Derelicts** or **Deconstruct All Derelicts** replaces any existing non-rebuild `BuildPlan` at `(b.tileX(), b.tileY())` and queues eligible derelict reclaim plans (`Vars.state.rules.derelictRepair`, `b.block.allowDerelictRepair`, `Build.validPlaceIgnoreUnits`)
- [ ] 1.3 Add the **Reclaim All Derelicts** button (`WebStyles.secondary()`, `height(unit(10))`, `growX()`) stacked vertically in `SelfBuildTask.buildSettings(AutoplayFeature)`

## 2. Localization & Verification

- [ ] 2.1 Add `feature.autoplay.settings.self-build.reclaim-derelict` and `feature.autoplay.settings.self-build.reclaim-derelict.tooltip` with descriptive comments in `assets/bundles/bundle.properties`
- [ ] 2.2 Run `./gradlew checkBundles compileJava` to verify bundle rules and compilation
