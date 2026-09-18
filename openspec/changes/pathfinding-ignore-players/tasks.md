## 1. Semantics spike

- [x] 1.1 Confirm `Unit.getPlayer()` returns the possessing player for remote players (not just local) in the pinned Mindustry version; fall back to a `Groups.player` scan if it does not

## 2. Filtering

- [x] 2.1 Replace the `unit == Vars.player.unit()` check with a player-possession skip in `updateProcessUnitPath` (no cache fill) and `drawProcessUnitPath` (no stale render)
- [x] 2.2 Confirm `CommandAI` (including player-issued RTS orders), LogicAI, ally-toggle, cost-type, and spawn-point behavior are unchanged

## 3. Verification

- [x] 3.1 Run the pathfinding test suite and confirm green
- [ ] 3.2 Verify in-game in multiplayer that driven units (local + remote) show no paths while RTS-commanded groups still do
