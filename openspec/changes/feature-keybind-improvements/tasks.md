## 1. FeatureKeybind Reactive Extensions

- [ ] 1.1 Add reactive key signal, rebind, and reset methods to `FeatureKeybind`
- [ ] 1.2 Write unit tests verifying programmatic rebind, reset, and reactive emission in `FeatureKeybindTest`

## 2. Localization & Styling Setup

- [ ] 2.1 Add bundle properties with descriptive comments for keybind section, inline rebinding prompt ("Press any key...", "Esc to cancel"), unbind tooltip, and reset action to `assets/bundles/bundle.properties`
- [ ] 2.2 Define chip styles in `WebStyles` for idle and listening keybind states

## 3. UI Component Implementation

- [ ] 3.1 Implement `SettingKeybindRow` component handling inline key listening, Esc cancel, Backspace unbind, and reset action
- [ ] 3.2 Implement `SettingKeybindCard` section card builder supporting both single feature (`SettingKeybindCard.forFeature(feature)`) and arbitrary collections
- [ ] 3.3 Add unit and behavioral tests for `SettingKeybindCard` and `SettingKeybindRow` verifying rendered states, listening lifecycle, and reset reactivity

## 4. Feature Settings Integration

- [ ] 4.1 Add `SettingKeybindCard.forFeature(feature)` to `TimeControlSettingsView`
- [ ] 4.2 Add `SettingKeybindCard.forFeature(feature)` to `SmartUpgradeSettingsView`
- [ ] 4.3 Add `SettingKeybindCard.forFeature(feature)` to `ScreenshotSettingsView`
- [ ] 4.4 Add `SettingKeybindCard.forFeature(feature)` to `HealthBarSettingsView`
- [ ] 4.5 Add `SettingKeybindCard.forFeature(feature)` to `RangeDisplaySettingsView`
- [ ] 4.6 Add `SettingKeybindCard.forFeature(feature)` to `VisualizerSettingsView`
- [ ] 4.7 Add `SettingKeybindCard.forFeature(feature)` to `WavePreviewSettingsView`
- [ ] 4.8 Add `SettingKeybindCard.forFeature(feature)` to `FreeCamera` settings and other features with registered hotkeys

## 5. General Settings Centralized Overview

- [ ] 5.1 Integrate a centralized keybind section card into `GeneralSettingsView` listing all registered feature hotkeys
- [ ] 5.2 Verify bidirectional synchronization with Mindustry's vanilla Keybinds dialog and ensure all automated tests pass
