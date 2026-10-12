## Why

Currently, MindustryTool features can register hotkeys (`FeatureKeybind`), but users can only view or rebind them by navigating to Mindustry's vanilla Settings -> Controls -> Rebind Keys dialog. Individual feature settings dialogs lack keybind visibility and configuration controls, making discovery and customization inconvenient. Furthermore, there is no centralized keybind overview within the mod's General Settings dialog.

## What Changes

- Add a reusable Solim settings component `SettingKeybindCard` for viewing and rebinding feature hotkeys inline within feature settings dialogs.
- Provide interactive key rebinding directly inside settings views: clicking a key chip enters a listening state to capture the next keypress, with `Escape` to cancel, `Backspace` to unbind (`KeyCode.unset`), and a one-click reset button to restore default bindings.
- Add `SettingKeybindCard.forFeature(feature)` to all existing feature settings views that provide keybindings (Time Control, Smart Upgrade, Free Camera, Screenshot, Range Display, Health Bar, Visualizer, Wave Preview, Chat, etc.).
- Add a centralized feature keybinds section to `GeneralSettingsView` displaying all registered feature hotkeys in one unified table.

## Capabilities

### New Capabilities
- `feature-keybind-ui`: Declarative Solim UI components and interactions for displaying, inline rebinding, and resetting feature keybindings both within individual feature settings dialogs and in the general settings view.

### Modified Capabilities
- `feature-keybinds`: Expose keybind mutation and reset operations reactive to UI updates and keep Arc `KeyBind` settings synchronized immediately.

## Impact

- **UI Components**: Introduces `mindustrytool.components.SettingKeybindCard` and `SettingKeybindRow` complying with Solim declarative UI standards.
- **Feature Settings Views**: Updates feature setting dialogs/views across `mindustrytool.features.*` to include keybind cards.
- **General Settings**: Updates `GeneralSettingsView` to display a centralized keybind management section.
- **Localization**: Adds translation keys to `assets/bundles/bundle.properties` for keybind status labels, inline rebinding instructions, and reset tooltips.
