## Why

Quick Access HUD buttons render in feature registration order with no way to rearrange them. Players want their most-used features first, independent of the settings-grid order and metadata order.

## What Changes

- Add a persisted display order for Quick Access HUD features as a `ConfigValue<Seq<String>>` owned by `QuickAccessFeature`'s config group (key `display-order`, `OrderedSeqPersister`), independent of `FeatureMetadata.order` and `ModSettings.featureOrder`.
- Render Quick Access HUD buttons in stored display order (hidden features filtered after sorting; settings button stays last).
- Add up/down arrow buttons at the rightmost end of each feature row in the Quick Access settings dialog to move that feature one position earlier/later via adjacent swap; boundary rows show a placeholder spacer instead of the arrow.
- Hidden features keep their positions in the stored list; moves operate on the full quick-access order.
- Lazily normalize the stored list on read (drop stale ids, append newly registered quick-access features at the end, write back only when changed).
- Add `assets/icons/chevron-up.png` plus `feature.quick-access.settings.move-up` / `move-down` bundle keys.

## Capabilities

### New Capabilities

- `quickaccess-hud-order`: persisted per-feature display order for the Quick Access HUD, lazy normalization, and swap-based up/down reordering from the settings dialog.

### Modified Capabilities

- `app-settings`: Quick Access HUD buttons follow the stored display order instead of registration order, and the Quick Access settings dialog gains per-row reorder controls.

## Impact

- Affected code: `mod/.../quickaccess` (`QuickAccessFeature`, `QuickAccessHudView`, `QuickAccessSettingsView`), `assets/icons/chevron-up.png`, `assets/bundles/bundle.properties`.
- No changes to `FeatureManager` ordering, `ModSettings.featureOrder`, or `FeatureMetadata.order`.
- No API or spec changes outside the capabilities listed above.
