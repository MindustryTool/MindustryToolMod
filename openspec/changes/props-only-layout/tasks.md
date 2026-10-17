## 1. UI Facade & Layout Prop Re-exports

- [x] 1.1 Re-export layout and alignment prop factories (`gap`, `center`, `top`, `bottom`, `left`, `right`, `align`) in `solim.UI`
- [x] 1.2 Verify `Values` has full coverage of all alignment and spacing prop variants

## 2. Mod Codebase Migration to Props-Only Layout

- [x] 2.1 Migrate components in `mindustrytool/components/` (`SettingCard`, `SettingKeybindRow`, `SettingScaffold`, `SettingSlider`) to use props for gap and alignment
- [x] 2.2 Migrate `features/armycontrol/` and `features/autoplay/`
- [x] 2.3 Migrate `features/browser/` (map, patch, schematic, common)
- [x] 2.4 Migrate `features/callout/`, `features/camerazoom/`, `features/chat/`
- [x] 2.5 Migrate `features/music/`, `features/quickaccess/`, `features/schematicgrid/`
- [x] 2.6 Migrate `features/settings/`, `features/teamresource/`, `features/timecontrol/`, `features/togglerendering/`
- [x] 2.7 Migrate `features/translation/`, `features/visualizer/`, `features/wavepreview/`, `features/web/`
- [x] 2.8 Migrate `services/` (`auth`, `crash`, `update`)
- [x] 2.9 Migrate `features/maprules/`, `features/pathfinding/`, `features/playerconnect/`
- [x] 2.10 Migrate `features/displaygroup/`, `features/emoji/`, `features/godmode/`, `features/griefalert/`
- [x] 2.11 Migrate `features/healthbar/`, `features/joystick/`, `features/logicvisualizer/`, `features/prettychat/`, `features/quickrotate/`, `features/rangedisplay/`, `features/screenshot/`

## 3. Remove Chained Layout Methods from Solim Core

- [x] 3.1 Remove chained `.gap(...)` and alignment methods (`.top()`, `.bottom()`, `.left()`, `.right()`, `.center()`, `.align()`) from `Column`, `Row`, `Card`, `Wrap`, and `Grid`
- [x] 3.2 Update tests in `solim-core` and `solim` to use `.props(...)` for gap and alignment

## 4. Verification

- [x] 4.1 Run `./gradlew compileJava` to confirm compilation across all modules
- [x] 4.2 Run `./gradlew test` across all modules to verify all tests pass
