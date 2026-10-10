## 1. Internationalization & Bundles

- [ ] 1.1 Add `feature.smart-upgrade.delete` (Delete button tooltip) and `feature.smart-upgrade.deleted` (Deleted blocks toast with `{0}` count placeholder) with descriptive translator comments in `assets/bundles/bundle.properties` and matching Vietnamese translations in `assets/bundles/bundle_vi.properties`.

## 2. Smart Upgrade Popup Menu & Chain Deletion

- [ ] 2.1 Update `SmartUpgradeFeature.showMenu(Tile tile)` to always open for valid supported buildings (even when `getUpgradeCandidates` is empty) and append a Delete button (`Icon.trash` tinted scarlet with tooltip `feature.smart-upgrade.delete`) that invokes `deleteChain(tile)` and displays the `feature.smart-upgrade.deleted` toast.
- [ ] 2.2 Implement `shouldDeleteBlock(Block startBlock, BlockGroup group, @Nullable Block currentBlock)` and `deleteChain(Tile startTile)` in `SmartUpgradeFeature` to queue breaking `BuildPlan(bx, by)` on `Vars.player.unit()` up to `cachedMaxUpdates`, supporting exact-type filtering, multi-size group deletion, and traversed connector deletion when `traverseBridges` is enabled.

## 3. Testing & Verification

- [ ] 3.1 Add unit tests in `SmartUpgradeFeatureTest` verifying deletion eligibility (`shouldDeleteBlock`) across `onlySameType` on/off, `traverseBridges` on/off, traversed connectors (`Router`, `Junction`, `ItemBridge`, `LiquidBridge`, etc.), and mixed-size blocks within the same `BlockGroup`.
- [ ] 3.2 Run `./gradlew checkBundles test check` to verify bundle localization keys, unit tests, and Checkstyle compliance.
