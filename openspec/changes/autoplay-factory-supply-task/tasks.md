## 1. Factory Supply Task & AI Implementation

- [x] 1.1 Create `FactorySupplyTask` and `FactorySupplyAI` in `mod/src/mindustrytool/features/autoplay/tasks/FactorySupplyTask.java` implementing `AutoplayTask` and `BaseAutoplayAI`
- [x] 1.2 Implement eligible production block detection (`GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, item-consuming `PowerGenerator`, `Drill`, `BeamDrill`, `WallCrafter`) and accurate input vs. output item resolution
- [x] 1.3 Implement storage discovery prioritizing `CoreBuild` (and core-linked `StorageBuild`) over standalone `StorageBuild` Containers/Vaults within drop range (`Vars.itemTransferRange = 220f`)
- [x] 1.4 Implement round-robin factory and item scheduling (drop needed inputs in round-robin order, then immediately withdraw produced outputs to deposit into prioritized Core/Container)
- [x] 1.5 Implement server-safe RPC rate limiting (`>= max(30f, Vars.state.rules.itemDepositCooldown * 60f)` ticks) and in-flight pending state tracking in `FactorySupplyAI` for `Call.requestItem`, `Call.transferInventory`, and fallback `Call.dropItem`- [x] 1.6 Implement block filter settings UI in `FactorySupplyTask.buildSettings` with **Select All** and **Clear All** buttons and reactive block filter chips

## 2. Registration & Localization

- [x] 2.1 Register `FactorySupplyTask` in `AutoplayFeature` (`DEFAULT_ORDER` immediately above `MiningTask`, and `taskMap`)
- [x] 2.2 Add translation keys with descriptive comments in `assets/bundles/bundle.properties` for `FactorySupplyTask` name, status messages, and settings labels (`Select All`, `Clear All`, filter header)

## 3. Verification & Testing

- [x] 3.1 Verify compilation across all modules with `./gradlew compileJava`
- [x] 3.2 Verify translation bundle rules with `./gradlew checkBundles`
- [x] 3.3 Run unit tests with `./gradlew test`

## 4. Conservative Mode Follow-Up

- [x] 4.1 Add `factory-supply.conservative` toggle (enabled by default) that only supplies inputs below `requiredAmount` and only collects outputs at `block.itemCapacity`
- [x] 4.2 Add translation keys for the conservative toggle in `bundle.properties` and all locale bundles
- [x] 4.3 Re-verify compilation, bundles, and autoplay tests
- [x] 4.4 Reduce RPC spacing to `max(24 ticks, itemDepositCooldown * 40f + 2f)` with 150-tick pending timeout per verified `v160.7` deposit/interact/double-error server limits
- [x] 4.5 Render remaining RPC cooldown seconds above the player unit while `FactorySupplyTask` is active
