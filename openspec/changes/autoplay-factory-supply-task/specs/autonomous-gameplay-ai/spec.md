## ADDED Requirements

### Requirement: Task 9 - Factory Supply
The Factory Supply task (`FactorySupplyTask`) MUST transfer required input items from nearby Cores or Containers into eligible production factories within drop range and collect produced output items from those factories back into Cores or Containers using round-robin scheduling and server-safe rate limiting.
- The task MUST be positioned by default above `MiningTask` (`RebuildTask` → `FactorySupplyTask` → `MiningTask`) in `AutoplayFeature.DEFAULT_ORDER`.
- The task MUST support eligible production buildings across `GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, item-consuming `PowerGenerator`, `Drill`, `BeamDrill`, and `WallCrafter`.
- When selecting a storage building to withdraw input items from or deposit produced output items into, the task MUST prioritize allied `CoreBuild`s (and core-linked `StorageBuild`s) over standalone `StorageBuild`s (Containers/Vaults), falling back to the nearest standalone `StorageBuild` only when no `CoreBuild` is in range or the `CoreBuild` lacks the required item / capacity.
- The task MUST only service production factories that are within drop range (`Vars.itemTransferRange`) of a candidate Core or Container (such that the unit can position itself within `Vars.itemTransferRange` of the storage and factory), and MUST respect `build.allowDeposit()` when depositing items.
- The task MUST cycle through eligible in-range factories in round-robin order; for each factory, it MUST supply missing input items (`factory.acceptStack(item, 1, unit) > 0`) in round-robin order and immediately withdraw available produced output items (`GenericCrafter.outputItems`, `Separator.results`, `WallCrafter.output`, or non-consumed drill outputs) to return to the prioritized Core or Container.
- The task MUST NEVER withdraw input ingredients from a production factory unless that item is explicitly an output product of that factory.
- All item transfer RPC calls (`Call.requestItem`, `Call.transferInventory`, `Call.dropItem`) MUST be coordinated exclusively through `FactorySupplyAI` with a shared cooldown timer (`>= Math.max(30f, Vars.state.rules.itemDepositCooldown * 60f)`) and an in-flight pending state guard with timeout fallback so the client never exceeds multiplayer server `itemDepositRate` or `Administration` block interaction rate limits.
- The task MUST provide settings displaying all eligible production blocks as filter chips alongside **Select All** and **Clear All** actions.

#### Scenario: Prioritizing Core over Container within drop range
- **WHEN** both a friendly `CoreBuild` and a standalone `StorageBuild` (Container/Vault) are within drop range of a production factory and both contain the required input item (or have space for the factory's output item)
- **THEN** `FactorySupplyTask` withdraws the input item from (and deposits the output product into) the `CoreBuild` rather than the Container

#### Scenario: Falling back to nearest Container when Core is out of range or lacks item
- **WHEN** no `CoreBuild` is within drop range of a production factory (or the `CoreBuild` lacks the required input item / is full for the output product) and a standalone `StorageBuild` within drop range has the item (or space)
- **THEN** `FactorySupplyTask` uses the nearest standalone `StorageBuild` to supply the factory or store its output product

#### Scenario: Round-robin input supply and product collection across factories
- **WHEN** multiple production factories within drop range require input items and produce output items
- **THEN** `FactorySupplyTask` cycles through the factories in round-robin order, drops needed input items in round-robin order, and collects produced output items back into the Core or Container without starving sibling factories or pulling input ingredients back out of the factory

#### Scenario: Rate-limited item transfers prevent multiplayer server kicks
- **WHEN** `FactorySupplyAI` executes consecutive `Call.requestItem` and `Call.transferInventory` operations
- **THEN** each RPC call waits for the in-flight state acknowledgment (or fallback timeout) and enforces at least `max(30 ticks, Rules.itemDepositCooldown * 60f)` cooldown between actions so server interaction and deposit rate limits are never exceeded

#### Scenario: Selecting and clearing production blocks in task settings
- **WHEN** the player clicks **Clear All** or **Select All** or toggles individual production block chips in the Factory Supply task settings
- **THEN** the persisted block filter updates reactively and `FactorySupplyTask` only services buildings whose block type is currently selected
