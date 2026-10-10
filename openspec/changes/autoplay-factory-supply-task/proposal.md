## Why

In Mindustry (especially in wireless/logic-less schematics, compact factory clusters around Cores or Vaults/Containers, and early-to-mid game bases), players frequently need to manually ferry input resources from Cores or Containers into nearby production factories (`GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, `PowerGenerator`, `Drill`) and collect produced output items back into Cores or Containers. Doing this manually is repetitive, and naive automated transfers easily exceed Mindustry's strict multiplayer server deposit and block-interaction rate limits (`player.itemDepositRate` and `Administration.allowAction` ratekeepers), resulting in players getting kicked for packet or interaction spam.

## What Changes

- Add a new `FactorySupplyTask` (`"factory-supply"`) to `AutoplayFeature`, positioned by default immediately above `MiningTask` (`RebuildTask` → `FactorySupplyTask` → `MiningTask`).
- Discover allied storage buildings (`CoreBuild` prioritized over standalone `StorageBuild` Containers/Vaults, ordered by nearest to the player unit) and service enabled production buildings within drop range (`Vars.itemTransferRange = 220f`, or where an overlap point exists within drop range of both storage and factory).
- Support all five production building categories:
  1. `GenericCrafter` (including `AttributeCrafter`, `HeatCrafter`)
  2. `Separator`
  3. `UnitFactory`, `Reconstructor`, `UnitAssembler`
  4. `PowerGenerator` (fuel-consuming generators and reactors)
  5. `Drill`, `BeamDrill`, `WallCrafter`
- Cycle through eligible factories in round-robin order; for each factory, supply its needed input items from the prioritized Core/Container in round-robin order and immediately withdraw produced output items from the factory to deposit back into the prioritized Core/Container.
- Enforce server-safe rate limiting and in-flight pending state tracking across all `Call.requestItem`, `Call.transferInventory`, and `Call.dropItem` RPC calls (`>= max(30 ticks, Rules.itemDepositCooldown * 60f)`) so the client never exceeds `itemDepositRate` (2 deposits/s) or `Administration` block interaction limits (25 actions/6s limit, 60 actions/6s kick).
- Provide task configuration settings displaying all eligible production blocks as filter chips with **Select All** and **Clear All** actions.

## Capabilities

### Modified Capabilities
- `autonomous-gameplay-ai`: Add the `FactorySupplyTask` requirement covering Core-over-Container priority, drop-range factory discovery, round-robin input supply and product collection, anti-kick RPC rate limiting, and block filter settings with Select All / Clear All.

## Impact

- `mindustrytool.features.autoplay.tasks.FactorySupplyTask`: New task and AI implementation (`FactorySupplyAI`).
- `mindustrytool.features.autoplay.AutoplayFeature`: Register `FactorySupplyTask` in `DEFAULT_ORDER` and `taskMap`.
- `assets/bundles/bundle.properties`: Add translation keys and comments for the new task name, statuses, and settings controls.
