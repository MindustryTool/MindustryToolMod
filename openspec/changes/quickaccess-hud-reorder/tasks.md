## 1. Order storage and model

- [x] 1.1 Add `displayOrderConfig` (`ConfigValue<Seq<String>>`, key `display-order`, `OrderedSeqPersister`) to `QuickAccessFeature`
- [x] 1.2 Implement lazy normalization (drop stale/development ids, append missing quick-access ids at end, write back only on change)
- [x] 1.3 Implement `moveUp(id)` / `moveDown(id)` adjacent swaps with boundary checks, plus per-id `canMoveUp` / `canMoveDown` signals

## 2. Assets and translations

- [x] 2.1 Add `assets/icons/chevron-up.png` matching the existing chevron set
- [x] 2.2 Add `feature.quick-access.settings.move-up` / `move-down` keys with translator comments to `bundle.properties`

## 3. Settings dialog rows

- [x] 3.1 Replace the static feature loop in `QuickAccessSettingsView` with a `ForEach` over the ordered ids mapped to features, keyed by id
- [x] 3.2 Add up/down arrow buttons at each row's right end (`FileIcon` chevrons with `Icon` fallbacks, ghost style, tooltips) wired to the move operations
- [x] 3.3 Render same-size placeholder spacers instead of arrows on boundary rows

## 4. HUD display order

- [x] 4.1 Sort `computeVisibleItems` by the stored display order, filter hidden after sorting, keep `__settings__` last
- [x] 4.2 Verify grid reconciliation shuffles existing buttons on reorder without rebuilding (toggle states survive)

## 5. Verification

- [ ] 5.1 Verify order persists across restart, stale ids heal, newcomers append, and boundaries behave in-game
- [x] 5.2 Run relevant build checks with no regressions
