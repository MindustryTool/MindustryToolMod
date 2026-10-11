## Context

The Autoplay feature (`AutoplayFeature`) evaluates enabled `AutoplayTask` instances in priority order each tick and delegates movement/actions to the active task's `BaseAutoplayAI`.
Players frequently build production factories (`GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, `PowerGenerator`, `Drill`, `BeamDrill`, `WallCrafter`) within item transfer range (`Vars.itemTransferRange = 220f`) of a team `CoreBuild` or `StorageBuild` (Container/Vault).

In Mindustry `v160.7`, multiplayer servers enforce two strict rate limits on item transfers:
1. **Deposit Ratekeeper (`player.itemDepositRate` in `InputHandler.transferInventory`)**: Allows at most 2 deposits per `2 * Rules.itemDepositCooldown` seconds (default `0.5s` cooldown = max 2 deposits per `1000ms`, or 1 per 30 ticks).
2. **Block Interaction Ratekeeper (`Administration.allowAction` on `PlayerInfo.rate`)**: Shared across both `ActionType.withdrawItem` (`Call.requestItem`) and `ActionType.depositItem` (`Call.transferInventory`). Default window is `6000ms` (6 seconds), with `interactRateLimit = 25` actions (~1 per 15 ticks) before rejecting/warning and `interactRateKick = 60` actions (10 actions/s) before kicking the player for 30 seconds.

Additionally, a factory's `Building.items` (`ItemModule`) stores both input ingredients and crafted output products together. While `Call.transferInventory` uses `build.acceptStack` (which checks `block.consumesItem(item)` / `UnitFactoryBuild.currentPlan`), `Call.requestItem` invokes `build.removeStack` without distinguishing inputs from outputs—so withdrawing from a factory must strictly request only output items.

## Goals / Non-Goals

**Goals:**
- Implement `FactorySupplyTask` (`"factory-supply"`) and `FactorySupplyAI` to automatically supply inputs to nearby production factories and collect their products into Cores/Containers.
- Prioritize `CoreBuild` (and core-linked `StorageBuild`s) over standalone `StorageBuild` Containers/Vaults when withdrawing input items or depositing factory products.
- Service factories in round-robin order (`factoryCursor`), cycling through each factory's needed input items in round-robin order (`inputCursor`) and immediately collecting available output items (`outputCursor`) on the same visit.
- Position the player unit in the overlap zone within `Vars.itemTransferRange` (`220f`) of both the target storage building and the target factory so transfers happen within drop range.
- Rate-limit all item RPCs (`Call.requestItem`, `Call.transferInventory`, `Call.dropItem`) with a shared cooldown (`>= max(30 ticks, Rules.itemDepositCooldown * 60f)`) and in-flight pending state tracking so the player is never kicked by multiplayer servers.
- Provide a settings UI listing all eligible production blocks as filter chips along with **Select All** and **Clear All** buttons.

**Non-Goals:**
- Long-distance map-wide courier hauling to factories that have no Core or Container within drop range.
- Modifying Mindustry's native `InputHandler` or networking packet definitions.

## Decisions

### Decision 1: Storage Priority (`CoreBuild` > Unlinked `StorageBuild`) & Drop-Range Pairing
- **Rationale**: The user specified that Core blocks take priority over Containers, and factories must be within drop range (`Vars.itemTransferRange = 220f`) of the storage/unit.
- **Implementation**:
  - Collect candidate storage buildings for `unit.team`:
    1. All valid `CoreBuild`s on `unit.team.cores()`, sorted by distance to `unit`.
    2. All valid standalone `StorageBuild`s (`!(b instanceof CoreBuild) && b.linkedCore == null` with `b.block.hasItems`), sorted by distance to `unit`. (Note: `StorageBuild`s with `linkedCore != null` share `linkedCore.items` and are treated as part of Core storage).
  - A factory $F$ and storage $S$ can be serviced together when the unit can reach drop range (`Vars.itemTransferRange = 220f`) of each (specifically where $F$ is within `Vars.itemTransferRange` of $S$, or up to `2 * (Vars.itemTransferRange - margin)` where an overlap point $M$ exists with $\text{dst}(M, S) \le 200\text{f}$ and $\text{dst}(M, F) \le 200\text{f}$).
  - When searching for a storage source (to withdraw an input item) or storage destination (to deposit an output item) for a factory $F$:
    - Check in-range `CoreBuild`s first (`core.items.get(item) > 0` for withdrawal; `core.acceptStack(item, amount, unit) > 0` for deposit).
    - Fall back to the nearest in-range standalone `StorageBuild` if no Core is in range, or if the Core lacks the required input item / is full for the output item.

### Decision 2: Eligible Production Categories & Input/Output Identification
- **Rationale**: Different Mindustry production block subclasses expose inputs and outputs through different fields.
- **Implementation**:
  - **Eligible Block Filter**: A `Block` is an eligible production block if `block.hasItems` and it is an instance of:
    - `GenericCrafter` (includes `AttributeCrafter`, `HeatCrafter`)
    - `Separator`
    - `UnitFactory`, `Reconstructor`, `UnitAssembler`
    - `PowerGenerator` (that consumes items, i.e. `block.consumesItem` / has item consumer)
    - `Drill`, `BeamDrill`, `WallCrafter`
  - **Input Items Needed by Factory $F$**:
    - An `Item` is needed by $F$ if `F.acceptStack(item, 1, unit) > 0` (which natively verifies `block.consumesItem(item)`, `UnitFactoryBuild.currentPlan`, `UnitAssemblerBuild.plan()`, and remaining item capacity).
  - **Output Products Available in Factory $F$**:
    - Never withdraw an item that $F$ only consumes. Specifically:
      - If `F.block instanceof GenericCrafter crafter`: check `crafter.outputItems` (if non-null, any `stack.item` with `F.items.get(stack.item) > 0`).
      - If `F.block instanceof Separator separator`: check `separator.results` (any `stack.item` with `F.items.get(stack.item) > 0`).
      - If `F.block instanceof WallCrafter wallCrafter`: check `wallCrafter.output` (if non-null and `F.items.get(wallCrafter.output) > 0`).
      - If `F.block instanceof Drill` or `F.block instanceof BeamDrill`: check any `Item` where `F.items.get(item) > 0 && !F.block.consumesItem(item)`.

### Decision 3: Round-Robin Scheduling Across Factories and Items
- **Rationale**: Greedy selection starves downstream factories or fills a multi-input crafter with only one ingredient.
- **Implementation**:
  - Maintain a stable list of nearby eligible active factories (sorted deterministically by `building.pos()`) and a `factoryCursor` index, plus a per-factory (or global) `itemCursor` index.
  - When selecting the next job:
    1. If `unit.stack.amount > 0`: complete delivering the currently held item first (deposit into the target factory if it is a needed input for the current target factory, or deposit into the prioritized Core/Container if it is an output product or excess item; if neither can accept it, rate-limited `Call.dropItem(0f)` clears the stack).
    2. When `unit.stack.amount == 0`:
       - If the unit just finished dropping an input into factory $F$ and $F$ currently has an output product that an in-range Core/Container can accept, immediately schedule taking that output product from $F$ before advancing `factoryCursor`.
       - Otherwise, iterate factories starting from `(factoryCursor + 1) % factories.size`:
         - Check if factory $F_i$ has any needed input item available in an in-range Core (priority 1) or Container (priority 2), choosing the input item via round-robin (`inputCursor`).
         - Or check if factory $F_i$ has any produced output item that can be accepted by an in-range Core (priority 1) or Container (priority 2), choosing the output item via round-robin.
         - Advance `factoryCursor = i` and execute the transfer cycle.

### Decision 4: Server-Safe Rate Limiting & In-Flight Guard
- **Rationale**: Preventing server kicks requires respecting `player.itemDepositRate` (2 deposits per `2 * Rules.itemDepositCooldown`), the `Administration` interaction budget (default 25 withdraw+deposit actions per 6s before warn-reject, 60 before a 30s kick), and the ArcNet double-error disconnect (2 packet errors within 2s closes the connection). Verified against the `v160.7` server sources (`InputHandler.transferInventory/requestItem`, `Administration.allowAction`, `ArcNetProvider.received`).
- **Implementation**:
  - All RPC calls (`Call.requestItem`, `Call.transferInventory`, `Call.dropItem`) are executed exclusively inside `FactorySupplyAI.updateMovement()`.
  - Enforce `ACTION_COOLDOWN = Math.max(24f, Vars.state.rules.itemDepositCooldown * 40f + 2f)` ticks between any two RPC calls. At the default `0.5s` cooldown this is 24 ticks (400ms): sustained 15 actions/6s keeps 40% of the default 25-action interact budget free for manual input and other tasks, while deposits land every 48 ticks (800ms) against the required `> 30` ticks (1.6x margin).
  - Track `actionPending` (`boolean`), `expectedEmpty` / `expectedItem`, and `pendingTimeout = 150f` ticks (2.5s, deliberately over the 2s double-error window so a retry after a suspected server rejection can never be the second error within 2s):
    - After `Call.requestItem`, wait until `unit.stack.amount > prevAmount` (or timeout) AND `cooldownTimer <= 0` before issuing `Call.transferInventory`.
    - After `Call.transferInventory`, wait until `unit.stack.amount == 0` (or `amount < prevAmount`, or timeout) AND `cooldownTimer <= 0` before issuing the next `Call.requestItem`.

### Decision 5: Block Filter Settings with "Select All" and "Clear All"
- **Rationale**: Persisting a `disabledBlocks` list (`ConfigValue<Seq<String>>`) allows an empty list to mean "all blocks enabled" by default while still allowing "Clear All" to disable every eligible block.
- **Implementation**:
  - Config key: `factory-supply.disabled-blocks` (`OrderedSeqPersister`).
  - `selectAll()`: sets `disabledBlocks` to an empty `Seq<>()`.
  - `clearAll()`: populates `disabledBlocks` with the names of all eligible production blocks.
  - UI in `buildSettings`: A row with **Select All** and **Clear All** `WebStyles.secondary()` buttons, followed by a `wrap()` container of `WebStyles.filterChipText()` buttons showing each eligible block's `uiIcon` and `localizedName`.

### Decision 6: Conservative Supply Mode (Enabled by Default)
- **Rationale**: Continuous topping-up causes many small RPC round-trips and risks server interaction limits; servicing only low inputs and full outputs minimizes trips while keeping factories running.
- **Implementation**:
  - Config key: `factory-supply.conservative` (`ConfigValue<Boolean>`, default `true`) with a checkbox at the top of `buildSettings`.
  - When enabled, an input is supplied only when `factory.items.get(input) < requiredAmount(block, input)`, where `requiredAmount` is the minimum matching `ConsumeItems` amount or `UnitFactory`/`UnitAssembler` plan requirement (falling back to `block.itemCapacity`), so factories that already hold a production cycle's worth of input are skipped.
  - When enabled, an output is collected only when `factory.items.get(output) >= block.itemCapacity` (output buffer full).
  - When disabled, the previous behavior applies (any `acceptStack > 0` need, any stored output `> 0`).

## Risks / Trade-offs

- [Risk] `Rules.onlyDepositCore` is enabled on a map → Mitigation: Check `build.allowDeposit()` before attempting to deposit items into any factory or non-core container; if `onlyDepositCore` is active, factories cannot receive manual deposits and the task will only withdraw products to deposit into the Core.
- [Risk] Unit holds leftover items that no factory or storage in range can accept → Mitigation: After attempting deposit into in-range Core/Containers, if no building accepts the held stack, invoke `Call.dropItem(0f)` subject to the same rate-limit cooldown so the unit does not deadlock.
