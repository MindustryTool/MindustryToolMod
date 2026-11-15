## 1. SplitBar Zero-Allocation Drawing

- [ ] 1.1 Hoist and cache the localized empty power network message and its `GlyphLayout` so `Core.bundle.get` and `layout.setText` never run in `draw()`
- [ ] 1.2 Implement precomputed text and `GlyphLayout` caching for power graph segments in `SplitBar`, updating on power stat changes rather than every frame
- [ ] 1.3 Refactor `SplitBar.draw()` to render strictly from cached layouts and primitive variables with zero heap allocations per frame

## 2. TeamResourceState Loop Optimization

- [ ] 2.1 Convert enhanced for-loops over `Vars.content.items()` and `Vars.content.units()` to indexed loops (`for (int i = 0; i < size; i++)`), eliminating per-frame `SeqIterator` allocations
- [ ] 2.2 Throttle item discovery and unit count scans to the 30-tick (0.5s) interval timer in `TeamResourceState.update()`
- [ ] 2.3 Convert `sampleHistory()` and rate update collection scans to iterator-free indexed loops

## 3. Tooltip Live Updating Cleanup

- [ ] 3.1 Optimize tooltip labels in `TeamResourceHudView.createItemCard` so they do not build strings or trigger glyph layouts on unchanged frames
- [ ] 3.2 Optimize tooltip labels in `SplitBar` so they do not build strings on unchanged frames

## 4. Verification & Regression Tests

- [ ] 4.1 Add automated zero-allocation test cases in `RenderLoopAllocationTest` asserting zero heap bytes allocated during steady-state `SplitBar.draw()` and HUD update passes
- [ ] 4.2 Execute `./gradlew test` to ensure clean compilation and 100% test suite passage
