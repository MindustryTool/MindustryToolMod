## 1. TeamResourceState reference release

- [ ] 1.1 Modify `clearHistory()` to also clear `itemHistories` and `graphHistories` maps (not just buffers)
- [ ] 1.2 Ensure `reset()`, `onWorldLoad()`, and `setSelectedTeam()` invoke the updated clear
- [ ] 1.3 Verify `usedItems`/`usedUnits` reset paths still work after map clear

## 2. BlockStatsDisplayFeature zero-allocation draw

- [ ] 2.1 Add cached primitive fields `cachedZoomThreshold`, `cachedOpacity`, `cachedScale`
- [ ] 2.2 Register `Effect.of(...)` for each config signal to populate the cached fields
- [ ] 2.3 Update `draw()` to read the cached primitives instead of `signal().peek()`
- [ ] 2.4 Add a zero-allocation test case to `RenderLoopAllocationTest` (or new test) for BlockStats

## 3. AutoplayFeature cooldown label cache

- [ ] 3.1 Add `cachedCooldownLabel` String field and `cachedCooldownTenths` int field
- [ ] 3.2 In `draw()`, recompute the label only when the rounded tenths value changes
- [ ] 3.3 Add zero-allocation coverage for the cooldown label path

## 4. MusicFeature isEnabled guard

- [ ] 4.1 Add `if (!isEnabled()) return;` as first statement in the 0.1s timer callback
- [ ] 4.2 Verify `captureOriginalMusic`/`loadAllCustomMusic` paths are only reached when enabled

## 5. LogicVisualizerFeature frustum culling

- [ ] 5.1 Replace `Groups.unit.each(unitDrawer)` with bounds-checked iteration using `Core.camera.bounds`
- [ ] 5.2 Reuse a scratch Rect field for bounds

## 6. ToggleRenderingFeature refactor

- [ ] 6.1 Replace `Groups.draw.each(lambda)` with manual indexed loop into reused `toHide` Seq
- [ ] 6.2 Replace `tileview.removeAll(lambda)` with manual index-based removal
- [ ] 6.3 Cache the reflection field read into a local per invocation
- [ ] 6.4 Smoke-test hide/restore with a small unit set

## 7. Pathfinding cache bounds

- [ ] 7.1 Add `MAX_ENTRIES = 4096` constant to `PathfindingCacheManager`
- [ ] 7.2 Implement eviction of oldest `lastUsedTime` when cap exceeded in `put()`
- [ ] 7.3 Change cleanup interval from 60 to 30 frames in `PathfindingFeature.update()`
- [ ] 7.4 Add a test asserting the cap and eviction

## 8. VisualizerFeature state pool release

- [ ] 8.1 Call `statePool.clear()` inside `clearFlowStates()`
- [ ] 8.2 Verify world-load/reset paths invoke `clearFlowStates()`

## 9. Hot (draw/update) listener gating sweep

- [ ] 9.1 Audit constructor-registered listeners on `Trigger.draw`/`Trigger.update` across `mod/`
- [ ] 9.2 Add `if (!isEnabled()) return;` as first statement to each
- [ ] 9.3 Record remaining (UI/event) listeners as deferred debt in AGENTS.md Known Debt table

## 10. Validation

- [ ] 10.1 Run `RenderLoopAllocationTest` and add cases for the newly covered features
- [ ] 10.2 Run existing mod tests for touched features
- [ ] 10.3 Update AGENTS.md Known Debt table (remove fixed rows D8/D9/D10/D11/D12/D13/D14 where applicable)