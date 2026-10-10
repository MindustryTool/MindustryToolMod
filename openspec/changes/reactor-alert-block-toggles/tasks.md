## 1. Per-Block Toggle State

- [x] 1.1 Add a per-block setting key helper (e.g. `blockSettingKey(Block)`) under the feature's config namespace in `ReactorAlertFeature`.
- [x] 1.2 Add a cached per-block `Signal<Boolean>` accessor defaulting to `true`, persisted through settings and writing through on change.
- [x] 1.3 Add an event-time `isBlockEnabled(Block)` lookup with lazy rebuild from loaded content and a `true` fallback when no lookup exists.
- [x] 1.4 Add `setBlockEnabled(Block, boolean)` and `setAllBlocksEnabled(boolean)` bulk writers used by the settings UI.
- [x] 1.5 Gate detection on the toggle: evaluate alerts only when `isExplosiveReactor(block) && isBlockEnabled(block)`.

## 2. Block Discovery

- [x] 2.1 Add an explosive-block listing helper scanning `Vars.content.blocks()` with the existing predicate, excluding blocks players cannot place.
- [x] 2.2 Rebuild the event-time lookup lazily when absent at placement evaluation time.

## 3. Settings UI

- [x] 3.1 Add a "Monitored blocks" section with All/None buttons to `ReactorAlertSettingsView` below the radius slider.
- [x] 3.2 Render one labeled row per discovered block (icon with `fullIcon` fallback, localized name, explosion radius, checkbox bound to the block signal).
- [x] 3.3 Wire row checkbox interaction to `setBlockEnabled` and All/None buttons to `setAllBlocksEnabled`.

## 4. Internationalization

- [x] 4.1 Add the monitored-blocks section label key with a translator comment.
- [x] 4.2 Add the All/None button label keys with translator comments.
- [x] 4.3 Add translations for all new keys to `bundle.properties` and every locale bundle so `checkBundles` passes.

## 5. Verification

- [x] 5.1 Compile the mod (`:mod:compileJava`) with no errors.
- [ ] 5.2 Verify a muted reactor shows no alert while an enabled reactor still alerts near a core.
- [ ] 5.3 Verify toggles persist across a game restart and default to enabled for newly discovered modded blocks.
- [ ] 5.4 Verify All enables every listed block and None mutes all alerts.
