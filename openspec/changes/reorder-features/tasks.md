## 1. Ordered persistence

- [x] 1.1 Add an ordered `Seq<String>` JSON persister usable via `ConfigGroup.value(...)` that preserves list positions across save/load.
- [x] 1.2 Declare the feature-order `ConfigValue<Seq<String>>` entry in `ModSettings` backed by the ordered persister.
- [x] 1.3 Cover persister round-trip (order preserved, empty list, missing key default) with tests.

## 2. Manager ordering and normalization

- [x] 2.1 Implement load-time normalization in `FeatureManager.init()`: dedupe (keep first), drop stale and development ids, append missing non-dev ids sorted by metadata order then id, persist only when dirty, reset corrupt payloads to default order.
- [x] 2.2 Derive the reactive feature sequence as non-dev ids in list order followed by the development block sorted by feature id.
- [x] 2.3 Implement global-list adjacent swap (move-left/move-right) with boundary positions exposed for the UI.
- [x] 2.4 Cover normalize (duplicates, stale, dev, missing, corrupt, clean-no-write) and swap (middle, boundaries, dev excluded) with tests.

## 3. Reactive settings view

- [x] 3.1 Derive `filteredFeatures` from both the search filter signal and the ordered feature sequence so order writes refresh the grid.
- [x] 3.2 Keep grid keying by feature id so reorders reconcile by move rather than rebuild.
- [x] 3.3 Disable reordering affordances while the search query is non-empty.

## 4. FeatureCard status row and localization

- [x] 4.1 Wrap `statusText` in a row with left-aligned text, a spacer, and chevron-left/right buttons at the most-right end; keep the divider accent below.
- [x] 4.2 Hide chevrons on development cards; bind chevron enabled state to boundary positions and the active-filter rule.
- [x] 4.3 Ensure chevron activation swaps global-list positions, persists, and does not toggle the card's enabled state.
- [x] 4.4 Add bundle keys with translator comments for the reorder controls; verify no hardcoded user-visible text.

## 5. Verification

- [x] 5.1 Verify no `Set`-based unordered persistence is used for the order list and no Java 9+ runtime APIs are introduced.
- [x] 5.2 Verify grid reflects swaps reactively, dev block stays last sorted by id, and a restart preserves the user order.
