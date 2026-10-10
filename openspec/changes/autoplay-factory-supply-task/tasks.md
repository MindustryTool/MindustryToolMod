## 1. Factory Supply Task & AI Implementation

- [ ] 1.1 Create `FactorySupplyTask` and `FactorySupplyAI` in `mod/src/mindustrytool/features/autoplay/tasks/FactorySupplyTask.java` implementing `AutoplayTask` and `BaseAutoplayAI`
- [ ] 1.2 Implement eligible production block detection (`GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, item-consuming `PowerGenerator`, `Drill`, `BeamDrill`, `WallCrafter`) and accurate input vs. output item resolution
- [ ] 1.3 Implement storage discovery prioritizing `CoreBuild` (and core-linked `StorageBuild`) over standalone `StorageBuild` Containers/Vaults within drop range (`Vars.itemTransferRange = 220f`)
- [ ] 1.4 Implement round-robin factory and item scheduling (drop needed inputs in round-robin order, then immediately withdraw produced outputs to deposit into prioritized Core/Container)
- [ ] 1.5 Implement server-safe RPC rate limiting (`>= max(30f, Vars.state.rules.itemDepositCooldown * 60f)` ticks) and in-flight pending state tracking in `FactorySupplyAI` for `Call.requestItem`, `Call.transferInventory`, and fallback `Call.dropItem`
- [ ] 1.6 Implement block filter settings UI in `FactorySupplyTask.buildSettings` with **Select All** and **Clear All** buttons and reactive block filter chips

## 2. Registration & Localization

- [ ] 2.1 Register `FactorySupplyTask` in `AutoplayFeature` (`DEFAULT_ORDER` immediately above `MiningTask`, and `taskMap`)
- [ ] 2.2 Add translation keys with descriptive comments in `assets/bundles/bundle.properties` for `FactorySupplyTask` name, status messages, and settings labels (`Select All`, `Clear All`, filter header)

## 3. Verification & Testing

- [ ] 3.1 Verify compilation across all modules with `./gradlew compileJava`
- [ ] 3.2 Verify translation bundle rules with `./gradlew checkBundles`
- [ ] 3.3 Run unit tests with `./gradlew test`
