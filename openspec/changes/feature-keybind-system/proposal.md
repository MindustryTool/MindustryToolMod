## Why

Currently, keybind support across mod features is inconsistent, fragmented, and boilerplate-heavy. Only two features (Schematic Browser and Map Browser) manually poll keys in ad-hoc update loops, while other features (like Autoplay, Chat, and TimeControl) either lack keybind support or previously relied on naive legacy toggle logic.

Introducing a unified, multi-keybind system allows features to declare one or more customizable hotkeys with distinct actions (e.g. toggle state, open dialog, perform one-shot operations), fully integrated with Mindustry's native Controls menu and protected by centralized input focus guards.

## What Changes

- Introduce `FeatureKeybind` descriptor encapsulating an Arc `KeyBind`, a `Runnable` action, and an execution precondition (`requireEnabled`).
- Add fluent registration helpers to `Feature` (`bindToggle`, `bindAction`, `bindDialog`) allowing features to declare hotkeys cleanly.
- Introduce `FeatureKeybindManager` as a centralized update dispatcher that safely executes hotkeys while ignoring inputs when any scene field has focus (`!Core.scene.hasField()`).
- Integrate all feature keybinds into Mindustry's vanilla Controls menu (`Settings -> Controls -> Rebind Keys`) under the `MindustryTool` category.
- Migrate `SchematicBrowserFeature` and `MapBrowserFeature` to use the unified keybind framework, eliminating their manual update loops.
- Add keybind support to other major features, including `AutoplayFeature` (toggle on/off), `ChatFeature` (toggle overlay), and `TimeControlFeature` (pause/speed controls).
- Add translatable keybind names and category labels to `assets/bundles/bundle.properties`.

## Capabilities

### New Capabilities
- `feature-keybinds`: Multi-keybind registration and centralized dispatch system for mod features with action callbacks, enabled-state guards, and vanilla Controls menu integration.

### Modified Capabilities
<!-- None: Browsers spec requirements are satisfied by the new system without changing external spec contracts. -->

## Impact

- **Affected Code**: `mindustrytool.features.Feature`, `mindustrytool.features.FeatureMetadata`, `mindustrytool.features.FeatureManager`, `mindustrytool.features.browser.*`, and features adopting keybinds (`autoplay`, `chat`, `timecontrol`).
- **Dependencies**: No new external dependencies; relies on Arc's `arc.input.KeyBind` and `arc.input.KeyCode`.
- **User Interface**: New keybind rows appear under the "MindustryTool" section in Mindustry's standard "Rebind Keys" dialog.
