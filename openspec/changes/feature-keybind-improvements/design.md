## Context

Features in MindustryTool register hotkeys using `Feature#bindToggle`, `Feature#bindAction`, and `Feature#bindDialog`, creating Arc `KeyBind` objects registered under the category `"MindustryTool"`. While `FeatureKeybindManager` dispatches these keys every tick (respecting `Core.scene.hasField()` and `requireEnabled`), users currently have no way to configure, inspect, or reset hotkeys from within a feature's dedicated settings view or within the mod's General Settings.

## Goals / Non-Goals

**Goals:**
- Provide a reusable Solim component (`SettingKeybindCard`) adhering to WebStyles and compact card conventions.
- Allow inline interactive key rebinding: clicking the key chip puts it into a capturing state, listening for the next keypress or mouse button.
- Support `Escape` to cancel rebinding without modifications, and `Backspace` to clear/unset the key.
- Provide a reset action per keybind that resets to its default keycode and automatically saves to Arc settings (`Core.settings`).
- Maintain full two-way synchronization with Mindustry's vanilla `Settings -> Controls -> Rebind Keys` menu.
- Integrate `SettingKeybindCard` into all feature settings views that define keybindings.
- Provide a centralized keybinds overview in `GeneralSettingsView`.

**Non-Goals:**
- Multi-key modifier combinations (e.g. `Ctrl + Shift + Key`) — keybinds adhere directly to standard Arc `KeyBind` / `KeyCode` single inputs.
- Re-architecting `FeatureKeybindManager` dispatching lifecycle — the existing frame dispatch logic remains intact.

## Decisions

### 1. Reusable `SettingKeybindCard` Component
- **Decision**: Implement `SettingKeybindCard` in `mindustrytool.components` exposing static factory methods:
  - `SettingKeybindCard.of(Feature feature)`: renders a section card titled with translation `setting.section.keybinds` if `feature.getKeybinds().size > 0`.
  - `SettingKeybindCard.of(String title, Seq<FeatureKeybind> keybinds)`: renders keybinds for broader contexts such as `GeneralSettingsView`.
- **Alternatives considered**:
  - Automatically injecting into `SettingScaffold`: Rejected because features require explicit compositional control over card ordering and view layout.

### 2. Inline Key Capture Mechanism
- **Decision**: When a user clicks a keybind button/chip, the component transitions to a listening state (`isListening = true`). An `arc.Events.run(Trigger.update, ...)` or scene key listener captures `Core.input.isKeyJustPressed(key)`.
  - If `KeyCode.escape`: Cancel listening, leave key unchanged.
  - If `KeyCode.backspace`: Set key to `KeyCode.unset`.
  - Any other key/mouse press: Update `bind.value = new Axis(pressedKey)`, call `bind.save()`, and notify reactive UI signals.
- **Alternatives considered**:
  - Modal popup dialog: Adds unnecessary visual interruption; inline button rebinding matches modern UX standards.
  - Opening Mindustry's vanilla `KeybindDialog`: Forces the user out of the settings view into vanilla's full table.

### 3. Reactive Keybind Representation
- **Decision**: Wrap the keybind state in a Solim `Signal<KeyCode>` inside the row component so when a key is rebound or reset, the chip label and reset button state update immediately without tearing down or rebuilding the UI tree.

## Risks / Trade-offs

- **[Risk] Unintentional trigger during rebinding**: Pressing a key to rebind could immediately trigger game actions or close dialogs.
  → **Mitigation**: Consume/suppress the key event or wait for key release when leaving listening mode; ensure `isListening` prevents `FeatureKeybindManager` from evaluating that frame.
- **[Risk] Name collisions or unlocalized keybind names**:
  → **Mitigation**: Resolve display names via `Core.bundle.get("keybind." + bind.name + ".name", bind.name)` matching Arc conventions.
