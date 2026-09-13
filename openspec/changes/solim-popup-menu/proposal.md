## Why

Chat message actions today live in a bespoke 240-line `ChatActionPopup` that fights `SolimDialog` (a modal abstraction) to behave like a lightweight context menu: it clears the dialog background, disables fill-parent, positions manually, and hand-rolls outside-dismissal. Only ~50 of those lines are chat-specific; the rest is generic floating-menu machinery (positioning + clamping, tap-outside/Back/Esc dismissal, show/hide lifecycle) that belongs in Solim — where `solim.overlay.Popup` already sits marked as a placeholder waiting to be finished.

## What Changes

- **Evolve `solim.overlay.Popup` into a native floating context menu**: reactive content via `children(provider)`, explicit `show(data, x, y)` / `hide()`, custom scene-hosted menu element (no dialog reuse), anchor positioning with prefer-above/flip/clamp math, and built-in dismissal (tap-outside swallow, Back + Esc keys, hide on resize).
- **Fluent chainable API**: every mutating method returns `Popup<T>` so configuration reads as one chain (`popup().children(...).rounded(...)`).
- **BREAKING**: the `popup()` facade changes from attaching `p.table()` to the ambient parent to constructing a scene-hosted menu component; existing placeholder tests are updated to the new behavior.
- **Adopt in chat**: slim `ChatActionPopup` down to a thin shell (shared static instance, `PopupRequest` signal data, Copy/Reply/Translate provider rows, translation logic) driven by the native menu. No spec-level chat behavior changes.

## Capabilities

### New Capabilities
- `solim-popup-menu`: Native Solim floating context menu. Covers reactive provider content, explicit show/hide with stage-coordinate placement, prefer-above/flip/clamp positioning, tap-outside (swallow) dismissal, Back + Esc dismissal, resize dismissal, and headless-safe no-ops when no scene exists.

### Modified Capabilities
- None. Chat message-action scenarios (`chat-message-group-layout`) keep passing unchanged; only the implementation vehicle moves from bespoke dialog abuse to the native menu.

## Impact

- `solim-core/src/solim/overlay/Popup.java`: evolved from placeholder Table wrapper to full menu component (generic `Popup<T>`, scene-hosted element, key/capture listeners, pure clamp math).
- `solim/src/solim/UI.java`: `popup()` facade becomes generic (`Popup<T> popup()` + `children(provider)` chaining); old attach-to-parent behavior removed.
- `solim-core` overlay tests: placeholder `Popup` tests rewritten for menu behavior; new tests for clamp math, provider rebuild, and headless no-ops.
- `mod/src/mindustrytool/features/chat/ChatActionPopup.java`: reduced to shared-instance shell (~50 lines); `openActions` anchor math stays (show-coordinates-only API, no `showNear`).
- No new translation keys (menu reuses existing chat action strings); no protocol or layout changes elsewhere.
