## 1. Feature Core — Rewrite ProgressDisplayFeature

- [ ] 1.1 Remove `development(true)` from `ProgressDisplayFeature` metadata; set `enabledByDefault(true)`
- [ ] 1.2 Add `ConfigValue<Float> zoomThresholdConfig` (default `0.5`, via `configGroup()`)
- [ ] 1.3 Add `ConfigValue<Float> opacityConfig` (default `1.0`)
- [ ] 1.4 Add `ConfigValue<Float> scaleConfig` (default `1.0`)
- [ ] 1.5 Add `BitSet blockEnabled` field and `rebuildBitSet()` method that reads per-block settings keys (default `true`)
- [ ] 1.6 Register `Events.run(Trigger.draw, this::draw)` in the constructor
- [ ] 1.7 Implement `draw()`: guard clause (enabled, isGame, hudfrag.shown, zoom threshold), peek frame-scratch scalars, then `eachBlock` with predicate + drawer
- [ ] 1.8 Implement building predicate: `UnitFactoryBuild || ReconstructorBuild || UnitAssemblerBuild || GenericCrafterBuild`
- [ ] 1.9 Implement `drawBuilding(Building)`: compute `fraction` and `remainingSeconds` per building type (see design D3); skip if `fraction < 0.01f`
- [ ] 1.10 Implement `drawText(float x, float y, float remainingSeconds)`: call `Fonts.outline.draw(...)` center-aligned; apply opacity and scale from frame-scratch fields
- [ ] 1.11 Expose `isBlockEnabled(Block)` and `setBlockEnabled(Block, boolean)` helpers; call `rebuildBitSet()` in `setBlockEnabled`
- [ ] 1.12 Override `getSettingDialog()` to lazily construct and return `ProgressDisplaySettingsDialog`
- [ ] 1.13 Call `rebuildBitSet()` lazily on first `draw()` (guard: BitSet is null) to handle post-content-load init

## 2. Settings Dialog + View

- [ ] 2.1 Create `ProgressDisplaySettingsDialog extends SolimDialog` with title, close button, close-on-back, and a reset action button
- [ ] 2.2 Create `ProgressDisplaySettingsView extends BaseComponent` and wire it as `children(() -> new ProgressDisplaySettingsView(feature))` in the dialog
- [ ] 2.3 In `ProgressDisplaySettingsView.build()`: outer `scroll()` → `column()` → section headers + block checkbox groups + slider rows
- [ ] 2.4 Add "Unit Factories" section header; render a `wrap()` of checkboxes for all `UnitFactory` blocks from `Vars.content.blocks()`
- [ ] 2.5 Add "Reconstructors" section header; render a `wrap()` of checkboxes for all `Reconstructor` blocks
- [ ] 2.6 Add "Unit Assemblers" section header; render a `wrap()` of checkboxes for all `UnitAssembler` blocks
- [ ] 2.7 Add "Crafters" section header; render a `wrap()` of checkboxes for all `GenericCrafter` blocks
- [ ] 2.8 Each block checkbox: label = `block.localizedName`, initial state from `feature.isBlockEnabled(block)`, on-change calls `feature.setBlockEnabled(block, value)`
- [ ] 2.9 Add `divider()` between block sections and slider section
- [ ] 2.10 Add zoom threshold slider row (`0..2`, step `0.1`); label shows `"Off"` when ≤ 0.01, else `"X.Xx"` — bind to `feature.zoomThresholdConfig.signal()`
- [ ] 2.11 Add opacity slider row (`0..1`, step `0.05`); label shows `"XX%"` — bind to `feature.opacityConfig.signal()`
- [ ] 2.12 Add scale slider row (`0.5..1.5`, step `0.1`); label shows `"XX%"` — bind to `feature.scaleConfig.signal()`
- [ ] 2.13 Implement `resetToDefaults()` on the feature; wire it to the dialog's reset button

## 3. Internationalisation

- [ ] 3.1 Add `feature.progress-display.settings.title` key to `bundle.properties`
- [ ] 3.2 Add `feature.progress-display.settings.reset` key
- [ ] 3.3 Add `feature.progress-display.settings.section.unit-factories` key
- [ ] 3.4 Add `feature.progress-display.settings.section.reconstructors` key
- [ ] 3.5 Add `feature.progress-display.settings.section.unit-assemblers` key
- [ ] 3.6 Add `feature.progress-display.settings.section.crafters` key
- [ ] 3.7 Add `feature.progress-display.settings.zoom-threshold` key
- [ ] 3.8 Add `feature.progress-display.settings.off` key (reuse `feature.health-bar.settings.off` if identical text is acceptable, otherwise add new key)
- [ ] 3.9 Add `feature.progress-display.settings.opacity` key
- [ ] 3.10 Add `feature.progress-display.settings.scale` key
- [ ] 3.11 Verify every string in the view and dialog uses `Core.bundle.get(...)` — no hardcoded display text

## 4. Verification

- [ ] 4.1 Build the mod; confirm no compile errors
- [ ] 4.2 Verify `ProgressDisplayFeature` is no longer in development mode and can be toggled on/off in the feature settings dialog
- [ ] 4.3 Verify countdown text appears above active `UnitFactory`, `Reconstructor`, `UnitAssembler`, and `GenericCrafter` buildings during gameplay
- [ ] 4.4 Verify no text renders on idle or fully-complete buildings
- [ ] 4.5 Verify zoom threshold hides text when zoomed out past the configured value
- [ ] 4.6 Verify per-block toggle in settings persists across mod restart (Core.settings round-trip)
- [ ] 4.7 Verify BitSet is rebuilt immediately when a block toggle is changed in settings
- [ ] 4.8 Verify crafter checkboxes wrap correctly in the settings view when there are 30+ blocks
- [ ] 4.9 Verify opacity and scale sliders affect rendered text appearance
