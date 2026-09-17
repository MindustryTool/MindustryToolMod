## Context

MindustryToolMod provides 25+ features including gameplay assistants (Autoplay, GodMode), utility browsers (Schematic Browser, Map Browser), interactive HUDs (Chat, TimeControl), and rendering visualizers.

Previously, hotkey handling was ad-hoc and fragmented:
- Only `SchematicBrowserFeature` and `MapBrowserFeature` wired keybinds, each registering a dedicated `Events.run(Trigger.update, ...)` loop and performing identical field checks (`!BrowserKeybinds.noInputFocused()`).
- `FeatureMetadata` held an unused `Optional<KeyBind>` field.
- Legacy code had a helper that assumed all keybinds only toggle feature enable state, which does not fit dialogs, interactive HUDs, or multi-action features.

This design introduces a unified keybind subsystem where features declare one or more keybinds with action-specific callbacks (`Runnable`) and clear preconditions (`requireEnabled`), dispatched centrally and integrated into Mindustry's vanilla controls menu.

## Goals / Non-Goals

**Goals:**
- **Multi-Keybind Support**: Allow any feature to register one or more keybinds (e.g., `TimeControlFeature` registering pause, speed up, speed down).
- **Flexible Action Handlers**: Support distinct actions per keybind via direct callbacks (`Runnable`), including toggle state, open dialog, and custom actions.
- **Action-Dependent Preconditions**: Support `requireEnabled` so toggle actions work even when the feature is disabled (to enable it), while operational actions require the feature to be active.
- **Centralized Dispatch & Input Safety**: Consolidate key polling into a single `FeatureKeybindManager` that checks `!Core.scene.hasField()` to prevent accidental activation while typing.
- **Vanilla Controls Integration**: Use Arc's `KeyBind.add(name, defaultKey, "MindustryTool")` so users customize keys in Mindustry's standard "Rebind Keys" menu.
- **Zero Boilerplate for Features**: Provide fluent helper methods on `Feature` (`bindToggle`, `bindAction`, `bindDialog`) so feature classes configure keybinds in 1 line.

**Non-Goals:**
- Custom in-mod key remapping UI (vanilla Mindustry Controls dialog already handles remapping, saving, and conflict detection).
- Keyboard macro recorder or key combination sequences (e.g. chords like `Ctrl+Shift+K`).

## Decisions

### 1. Separate Descriptor Object: `FeatureKeybind`
- **Decision**: Encapsulate each keybind in a `FeatureKeybind` containing `KeyBind bind`, `Runnable action`, and `boolean requireEnabled`.
- **Rationale**: Keeps `FeatureMetadata` clean and avoids limiting features to a single keybind or hardcoded enum action.
- **Alternatives Considered**:
  - *Single keybind in `FeatureMetadata`*: Too restrictive; features like TimeControl or Music require multiple hotkeys.
  - *Polymorphic `Feature.onKeybind()` hook*: Does not distinguish between multiple keys within the same feature.

### 2. Centralized `FeatureKeybindManager` Dispatcher
- **Decision**: A single listener registered on `Trigger.update` by `FeatureManager.init()` iterates over registered features and their `FeatureKeybind`s.
- **Rationale**: Eliminates duplicate `Events.run(Trigger.update)` registrations across feature classes, guarantees consistent `Core.scene.hasField()` keyboard focus checks, and runs on the main game thread.
- **Alternatives Considered**:
  - *Per-feature polling*: Duplicate code in every feature, easy to forget focus checks, wasted event registrations.

### 3. Action-Dependent Enable Check (`requireEnabled`)
- **Decision**: Each `FeatureKeybind` explicitly specifies whether it requires the feature to be enabled (`isRequireEnabled()`).
- **Rationale**:
  - Toggling a feature (e.g. `Autoplay` on/off) must trigger when the feature is disabled.
  - Operational tasks (e.g. speeding up time, placing smart drills) must be inhibited if the feature is turned off in mod settings.
  - Dialog features (like Schematic Browser) can choose whether opening the dialog is allowed only when enabled.

### 4. Integration with Mindustry's Vanilla Controls Settings
- **Decision**: Register all keys with category `"MindustryTool"` via `KeyBind.add(name, defaultKey, "MindustryTool")`.
- **Rationale**: Mindustry's `KeybindDialog` automatically picks up all registered `KeyBind`s and groups them under section `"MindustryTool"`. Key rebinding, persistence, and reset-to-default are handled by the engine without custom UI maintenance.
- **Localization**: Localized via `bundle.properties` using standard Mindustry keys: `section.MindustryTool.name` and `keybind.<name>.name`.

## Risks / Trade-offs

- **[Vanilla Keybind Namespace Clashes]** → Prefix all keybind names with `mindustrytool.` or feature-specific namespaces (e.g. `timecontrol.pause`).
- **[Overhead of Iterating Features Each Frame]** → Mod has ~25 features with ~10-15 total keybinds. Simple iteration on `Trigger.update` takes microseconds and avoids complex state machine caching.
- **[Accidental Triggering During Text Input]** → Enforce `if (Core.scene.hasField()) return;` before checking any key release in `FeatureKeybindManager`.
