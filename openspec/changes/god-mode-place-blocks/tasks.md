## 1. Provider Layer Updates

- [ ] 1.1 Add `placeBlock(Block block, Team team, float x, float y, int rotation)` to `GodModeProvider` with default `placeCore` backward-compatibility bridge.
- [ ] 1.2 Implement `placeBlock` in `InternalGodModeProvider` with rotation and force-overwrite tile placement.
- [ ] 1.3 Implement `placeBlock` in `JSGodModeProvider` using Rhino-compatible `/js` command with rotation.

## 2. Localization & Bundle Keys

- [ ] 2.1 Add bundle keys with descriptive comments to `assets/bundles/bundle.properties` for dialog title, block selection, category chips, rotation directions, and HUD tooltip.

## 3. UI Implementation & HUD Integration

- [ ] 3.1 Create `GodModeBlockDialog` with search query input, category filter chips, responsive block grid, dynamic 4-way rotation selector, team selector, and position picker.
- [ ] 3.2 Update `GodModeHudView` hammer button to open `GodModeBlockDialog` with updated tooltip.
- [ ] 3.3 Remove or replace legacy `GodModeCoreDialog`.

## 4. Testing & Verification

- [ ] 4.1 Add unit tests for `GodModeProvider` block placement and rotation handling.
- [ ] 4.2 Run project test suite and verify build passes without regressions.
