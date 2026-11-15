## Why

Recent changes to the Team Resource Overlay (`SplitBar` power gauge, `TeamResourceState` per-frame scans, and live tooltip updates) cause rapid heap memory growth during normal gameplay. While not a permanent memory leak (memory is eventually freed by garbage collection), the continuous generation of strings, `GlyphLayout` structures, and `SeqIterator` allocations inside 60 FPS frame and update loops creates high-rate allocation churn, triggering frequent GC pauses, frame stutter, and increased battery consumption.

## What Changes

- **Zero-Allocation Power Display in `SplitBar`**: Cache precomputed power strings and `GlyphLayout` instances per power graph in `SplitBar`, updating them only on the 30-tick (0.5s) power refresh cycle instead of every frame. Hoist the empty network localized string so `Core.bundle.get` and `layout.setText` never run per-frame in `draw()`.
- **Throttled & Iterator-Free State Updates in `TeamResourceState`**: Throttle item inventory and unit count scans to an interval timer (every 30 ticks / 0.5s), and convert all enhanced `for-each` loops over Arc `Seq` collections to indexed `for (int i = 0; i < size; i++)` loops, eliminating per-frame `SeqIterator` allocations.
- **Tooltip Live Label Optimization**: Clean up `Label.update()` hooks in item and power tooltips so that hovering does not continuously allocate strings and trigger Scene2D glyph layouts every frame.
- **Automated Regression Test**: Add a `RenderLoopAllocationTest` verifying that steady-state frame rendering of `SplitBar` and HUD updates allocates exactly 0 bytes on the heap.

## Capabilities

### New Capabilities
<!-- No new capabilities being introduced -->

### Modified Capabilities
- `team-resource-overlay`: Require zero-allocation steady-state rendering for `SplitBar` and throttled, iterator-free scans in `TeamResourceState`.
- `agent-perf-discipline`: Expand the zero-allocation render loop contract and verification test suite to include `TeamResourceFeature` HUD rendering and update loops.

## Impact

- Affected files:
  - `mod/src/mindustrytool/features/teamresource/SplitBar.java`
  - `mod/src/mindustrytool/features/teamresource/TeamResourceState.java`
  - `mod/src/mindustrytool/features/teamresource/TeamResourceHudView.java`
  - `mod/src/test/java/mindustrytool/RenderLoopAllocationTest.java` (or new test in `features/teamresource`)
- APIs/Dependencies: Internal to `mod`. No public API changes, no breaking changes to settings or save formats.
